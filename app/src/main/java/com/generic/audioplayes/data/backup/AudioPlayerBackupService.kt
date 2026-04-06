package com.generic.audioplayes.data.backup

import androidx.datastore.core.DataStore
import com.generic.audioplayes.data.AppDatabase
import com.generic.audioplayes.data.UserPreferences
import com.generic.audioplayes.data.copy
import com.generic.audioplayes.data.UserPreferences.PlaybackParams
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import com.generic.audioplayes.data.music.PlaylistExceptId
import com.generic.audioplayes.data.services.PlaylistService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val BACKUP_SCHEMA_VERSION = 1
private const val MAX_BACKUP_IMPORT_BYTES = 8 * 1024 * 1024

data class BackupImportResult(
    val preferencesApplied: Boolean,
    val playlistsImported: Int,
    val tracksAdded: Int,
    val tracksSkippedMissing: Int,
)

@Singleton
class AudioPlayerBackupService @Inject constructor(
    private val userPreferences: DataStore<UserPreferences>,
    private val db: AppDatabase,
    private val playlistService: PlaylistService,
    private val preferenceProvider: AudioPlayerPreferenceProvider,
) {

    suspend fun exportToJsonString(): String = withContext(Dispatchers.IO) {
        val prefs = userPreferences.data.first()
        val playlistDao = db.playlistDao()
        val allPlaylists = playlistDao.getAllPlaylists().first()
        val playlistsJson = JSONArray()
        for (pl in allPlaylists) {
            val refs = playlistDao.getCrossRefsOrdered(pl.playlistId)
            val tracks = JSONArray()
            refs.forEach { tracks.put(it.location) }
            playlistsJson.put(
                JSONObject().apply {
                    put("name", pl.playlistName)
                    put("tracks", tracks)
                },
            )
        }
        JSONObject().apply {
            put("schemaVersion", BACKUP_SCHEMA_VERSION)
            put("exportedAtEpochMs", System.currentTimeMillis())
            put("preferences", preferencesToJson(prefs))
            put("playlists", playlistsJson)
            put(
                "notes",
                "queueState is not included; restore queue is future work.",
            )
        }.toString(2)
    }

    suspend fun markExportSuccess() {
        preferenceProvider.setLastBackupExportEpochMs(System.currentTimeMillis())
    }

    /**
     * Imports preferences (partial-safe) and appends playlists. Does not replace the whole DB.
     * Skips track locations not present in the local library. Playlist names are de-duplicated with a numeric suffix.
     */
    suspend fun importFromJsonBytes(bytes: ByteArray): Result<BackupImportResult> = withContext(Dispatchers.IO) {
        if (bytes.size > MAX_BACKUP_IMPORT_BYTES) {
            return@withContext Result.failure(IllegalArgumentException("Backup file too large"))
        }
        val text = try {
            bytes.decodeToString()
        } catch (e: Exception) {
            Timber.e(e)
            return@withContext Result.failure(IllegalArgumentException("Invalid encoding"))
        }
        importFromJsonString(text)
    }

    suspend fun importFromJsonString(text: String): Result<BackupImportResult> = withContext(Dispatchers.IO) {
        val root = try {
            JSONObject(text)
        } catch (e: Exception) {
            Timber.e(e)
            return@withContext Result.failure(IllegalArgumentException("Invalid JSON"))
        }
        val schema = root.optInt("schemaVersion", -1)
        if (schema != BACKUP_SCHEMA_VERSION) {
            return@withContext Result.failure(
                IllegalArgumentException("Unsupported backup (schema $schema, need $BACKUP_SCHEMA_VERSION)"),
            )
        }
        var prefsApplied = false
        root.optJSONObject("preferences")?.let { prefObj ->
            try {
                applyPreferencesFromJson(prefObj)
                prefsApplied = true
            } catch (e: Exception) {
                Timber.e(e, "Backup preferences partial failure")
            }
        }
        var playlistsImported = 0
        var tracksAdded = 0
        var tracksSkipped = 0
        root.optJSONArray("playlists")?.let { arr ->
            val validLocations = db.songDao().getSongs().map { it.location }.toHashSet()
            val usedNames = db.playlistDao().getAllPlaylists().first()
                .map { it.playlistName }
                .toMutableSet()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val rawName = o.optString("name", "").trim().ifBlank { "Imported" }
                val name = nextUniquePlaylistName(rawName, usedNames)
                val tracks = o.optJSONArray("tracks") ?: JSONArray()
                val id = try {
                    db.playlistDao().insertPlaylist(
                        PlaylistExceptId(
                            playlistName = name,
                            createdAt = System.currentTimeMillis(),
                        ),
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Playlist insert failed")
                    continue
                }
                playlistsImported++
                val locs = mutableListOf<String>()
                for (t in 0 until tracks.length()) {
                    val loc = tracks.optString(t, null) ?: continue
                    if (loc.isBlank()) continue
                    if (loc !in validLocations) {
                        tracksSkipped++
                        continue
                    }
                    locs.add(loc)
                }
                if (locs.isNotEmpty()) {
                    try {
                        playlistService.addSongsToPlaylist(locs, id)
                        tracksAdded += locs.size
                    } catch (e: Exception) {
                        Timber.e(e, "addSongsToPlaylist")
                    }
                }
            }
        }
        Result.success(
            BackupImportResult(
                preferencesApplied = prefsApplied,
                playlistsImported = playlistsImported,
                tracksAdded = tracksAdded,
                tracksSkippedMissing = tracksSkipped,
            ),
        )
    }

    private fun nextUniquePlaylistName(desired: String, used: MutableSet<String>): String {
        val base = desired.trim().ifBlank { "Imported" }
        var candidate = base
        var n = 1
        while (candidate in used) {
            candidate = "$base ($n)"
            n++
        }
        used.add(candidate)
        return candidate
    }

    private suspend fun applyPreferencesFromJson(pref: JSONObject) {
        userPreferences.updateData { current ->
            current.copy {
                // Theme / accent
                parseEnum(pref.optString("chosenTheme"), UserPreferences.Theme.values())?.let {
                    chosenTheme = it
                }
                parseEnum(pref.optString("chosenAccent"), UserPreferences.Accent.values())?.let {
                    chosenAccent = it
                }
                if (pref.has("useMaterialYouTheme")) {
                    useMaterialYouTheme = pref.optBoolean("useMaterialYouTheme")
                }
                // Playback params (same bounds as [AudioPlayerPreferenceProvider.updatePlaybackParams])
                val speed = pref.optInt("playbackSpeed", playbackParams.playbackSpeed)
                val pitch = pref.optInt("playbackPitch", playbackParams.playbackPitch)
                playbackParams = PlaybackParams.getDefaultInstance().copy {
                    playbackSpeed = speed.coerceIn(1, 200)
                    playbackPitch = pitch.coerceIn(1, 200)
                }
                // Tabs & sort
                pref.optJSONArray("selectedTabs")?.let { arr ->
                    val list = buildList {
                        for (i in 0 until arr.length()) {
                            add(arr.optInt(i))
                        }
                    }
                    if (list.isNotEmpty()) {
                        selectedTabs.apply {
                            clear()
                            addAll(list)
                        }
                    }
                }
                pref.optJSONObject("chosenSortOrder")?.let { sortObj ->
                    chosenSortOrder.clear()
                    sortObj.keys().forEach { k ->
                        chosenSortOrder[k.toInt()] = sortObj.optInt(k)
                    }
                }
                // Toggles
                if (pref.has("crossfadeEnabled")) crossfadeEnabled = pref.optBoolean("crossfadeEnabled")
                if (pref.has("gaplessPlaybackEnabled")) {
                    gaplessPlaybackEnabled = pref.optBoolean("gaplessPlaybackEnabled")
                }
                if (pref.has("keepScreenOn")) keepScreenOn = pref.optBoolean("keepScreenOn")
                if (pref.has("showOnLockScreen")) {
                    showOnLockScreen = pref.optBoolean("showOnLockScreen")
                }
                if (pref.has("pauseOnHeadsetDisconnect")) {
                    pauseOnHeadsetDisconnect = pref.optBoolean("pauseOnHeadsetDisconnect")
                }
                if (pref.has("volumeBoosterPercent")) {
                    volumeBoosterPercent = pref.optInt("volumeBoosterPercent").coerceIn(100, 200)
                }
                // Equalizer
                if (pref.has("equalizerEnabled")) equalizerEnabled = pref.optBoolean("equalizerEnabled")
                parseEnum(
                    pref.optString("equalizerPreset"),
                    UserPreferences.EqualizerPreset.values(),
                )?.let {
                    equalizerPreset = it
                    if (it != UserPreferences.EqualizerPreset.EQUALIZER_PRESET_CUSTOM) {
                        equalizerCustomBandMb.clear()
                    }
                }
                pref.optJSONArray("equalizerCustomBandMb")?.let { arr ->
                    equalizerCustomBandMb.clear()
                    for (i in 0 until arr.length()) {
                        equalizerCustomBandMb.add(arr.optInt(i))
                    }
                }
                if (pref.has("bassBoostStrength")) {
                    bassBoostStrength = pref.optInt("bassBoostStrength").coerceIn(0, 1000)
                }
                if (pref.has("virtualizerStrength")) {
                    virtualizerStrength = pref.optInt("virtualizerStrength").coerceIn(0, 1000)
                }
                if (pref.has("equalizerUiBandCount")) {
                    equalizerUiBandCount = pref.optInt("equalizerUiBandCount").let { n ->
                        if (n == 5 || n == 10) n else 5
                    }
                }
                if (pref.has("reverbPreset")) {
                    reverbPreset = pref.optInt("reverbPreset").coerceIn(0, 6)
                }
                if (pref.has("graphicWallpaperPreset")) {
                    graphicWallpaperPreset = pref.optInt("graphicWallpaperPreset").coerceIn(0, 64)
                }
                if (pref.has("graphicWallpaperCustomUri")) {
                    graphicWallpaperCustomUri = pref.optString("graphicWallpaperCustomUri", "")
                }
                if (pref.has("graphicThemeColorSlot")) {
                    graphicThemeColorSlot = pref.optInt("graphicThemeColorSlot").coerceIn(0, 14)
                }
                if (pref.has("chosenWidgetStyle")) {
                    parseEnum(
                        pref.optString("chosenWidgetStyle"),
                        UserPreferences.WidgetStyle.values(),
                    )?.let { chosenWidgetStyle = it }
                }
                // Intentionally not restoring: sleep_timer_*, crashlytics, onboarding, queue, seed versions
            }
        }
    }

    private fun <T : Enum<T>> parseEnum(raw: String, values: Array<T>): T? {
        val name = raw.ifBlank { return null }
        return values.find { it.name == name }
    }

    private fun preferencesToJson(prefs: UserPreferences): JSONObject = JSONObject().apply {
        put("useMaterialYouTheme", prefs.useMaterialYouTheme)
        put("chosenTheme", prefs.chosenTheme.name)
        put("chosenAccent", prefs.chosenAccent.name)
        put("playbackSpeed", prefs.playbackParams.playbackSpeed)
        put("playbackPitch", prefs.playbackParams.playbackPitch)
        val tabs = JSONArray()
        prefs.selectedTabsList.forEach { tabs.put(it) }
        put("selectedTabs", tabs)
        val sort = JSONObject()
        prefs.chosenSortOrderMap.forEach { (k, v) ->
            sort.put(k.toString(), v)
        }
        put("chosenSortOrder", sort)
        put("crossfadeEnabled", prefs.crossfadeEnabled)
        put("gaplessPlaybackEnabled", prefs.gaplessPlaybackEnabled)
        put("keepScreenOn", prefs.keepScreenOn)
        put("showOnLockScreen", prefs.showOnLockScreen)
        put("pauseOnHeadsetDisconnect", prefs.pauseOnHeadsetDisconnect)
        put("volumeBoosterPercent", prefs.volumeBoosterPercent)
        put("equalizerPreset", prefs.equalizerPreset.name)
        val bands = JSONArray()
        prefs.equalizerCustomBandMbList.forEach { bands.put(it) }
        put("equalizerCustomBandMb", bands)
        put("bassBoostStrength", prefs.bassBoostStrength)
        put("virtualizerStrength", prefs.virtualizerStrength)
        put("equalizerEnabled", prefs.equalizerEnabled)
        put("equalizerUiBandCount", prefs.equalizerUiBandCount)
        put("reverbPreset", prefs.reverbPreset)
        put("graphicWallpaperPreset", prefs.graphicWallpaperPreset)
        put("graphicWallpaperCustomUri", prefs.graphicWallpaperCustomUri)
        put("graphicThemeColorSlot", prefs.graphicThemeColorSlot)
        put("chosenWidgetStyle", prefs.chosenWidgetStyle.name)
    }
}
