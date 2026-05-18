package com.generic.audioplayes.tags

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.generic.audioplayes.util.AudioFileActions
import com.generic.audioplayes.util.Stage4DebugLog
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.images.Artwork
import org.jaudiotagger.tag.images.StandardArtwork
import timber.log.Timber
import java.io.File
import java.nio.charset.Charset

/**
 * Fixes mojibake in text that went through one of the two common broken pipes:
 *
 *  1. "Latin‑1 mojibake" — the bytes are UTF‑8 but were read as ISO‑8859‑1, so "Глава"
 *     becomes "Ð“Ð»Ð°Ð²Ð°". Each byte lands on a code‑point in 0x00C0..0x00FF.
 *  2. "CP1251 mojibake" — the bytes are UTF‑8 but were read as Windows‑1251 and then
 *     re‑encoded as UTF‑8, so "Глава" becomes "Р“Р»Р°Р°Р²Р°". The string is filled with
 *     uppercase Cyrillic 'Р' (U+0420) interleaved with Latin‑1 punctuation like » ° ².
 *
 * We generate candidates for both paths and pick the one that looks most like natural
 * Russian text via [naturalRussianScore] (counts lowercase Cyrillic, penalises stray
 * Latin‑1 punctuation and the U+FFFD replacement char). Safe for plain ASCII / already
 * correct UTF‑8 tags — it falls back to the original when no candidate scores higher.
 */
internal fun maybeFixMojibake(raw: String?): String {
    if (raw.isNullOrEmpty()) return raw.orEmpty()

    // Fast path: if the string is pure ASCII letters + digits + common punctuation, nothing
    // can be wrong with it, skip the expensive re‑encoding dance.
    if (raw.all { it.code in 0x20..0x7E }) return raw

    val candidates = mutableListOf(raw)

    // Case 1: UTF‑8 bytes read as Latin‑1 (prefix bytes land in 0xC0..0xFF).
    if (raw.any { it.code in 0x0080..0x00FF }) {
        runCatching {
            val bytes = raw.toByteArray(Charsets.ISO_8859_1)
            decodeSilently(bytes, Charsets.UTF_8)?.let(candidates::add)
            decodeSilently(bytes, CHARSET_CP1251)?.let(candidates::add)
        }
    }

    // Case 2: UTF‑8 bytes read as Windows‑1251 and re‑encoded. Signature: many uppercase
    // Cyrillic 'Р' (0x0420) letters interleaved with non‑Cyrillic chars, or any character
    // that maps back cleanly onto a CP1251 byte. Re‑encode as CP1251 and re‑read as UTF‑8.
    if (raw.any { it.code in 0x0400..0x04FF } ||
        raw.any { it.code in 0x00A0..0x00FF } ||
        raw.any { it.code in 0x2000..0x206F }
    ) {
        runCatching {
            val bytes = raw.toByteArray(CHARSET_CP1251)
            decodeSilently(bytes, Charsets.UTF_8)?.let(candidates::add)
        }
    }

    // Reject candidates peppered with U+FFFD (? replacement) — they came from a wrong guess.
    val viable = candidates.filter { !it.contains('\uFFFD') }.ifEmpty { candidates }
    val best = viable.maxByOrNull { naturalRussianScore(it) } ?: return raw

    // Only accept the rewrite when it both scores higher AND produces MORE lowercase
    // Cyrillic characters than the raw string. Without this second gate "Über" would be
    // mangled into "Ьber" (uppercase Cyrillic Ь from CP1251 byte 0xDC) because both score
    // 0 for Cyrillic‑lower and the rewrite has lower Latin‑1 penalty.
    val rawLower = cyrillicLowerCount(raw)
    val bestLower = cyrillicLowerCount(best)
    val rawScore = naturalRussianScore(raw)
    val bestScore = naturalRussianScore(best)
    return if (bestLower > rawLower && bestScore > rawScore) best else raw
}

private fun cyrillicLowerCount(s: String): Int {
    var n = 0
    for (c in s) {
        val code = c.code
        if (code in 0x0430..0x044F || code == 0x0451) n++
    }
    return n
}

private val CHARSET_CP1251: Charset = try {
    Charset.forName("windows-1251")
} catch (_: Exception) {
    Charsets.ISO_8859_1
}

private fun decodeSilently(bytes: ByteArray, charset: Charset): String? = try {
    String(bytes, charset)
} catch (_: Exception) {
    null
}

/**
 * Heuristic score: higher = more likely to be natural Russian/Ukrainian text. Lowercase
 * Cyrillic letters are the strongest positive signal (real prose is ~80% lowercase). Stray
 * Latin‑1 punctuation, Latin‑1 letters with diacritics, general‑punctuation smart quotes
 * and U+FFFD are strong negative signals — they show up heavily in mojibake but almost
 * never in real Russian text.
 */
private fun naturalRussianScore(s: String): Int {
    var score = 0
    for (c in s) {
        val code = c.code
        when {
            code in 0x0430..0x044F -> score += 2            // cyrillic lowercase — strong +
            code == 0x0451 -> score += 2                    // ё
            code in 0x0410..0x042F -> score += 0            // uppercase cyrillic — neutral
            code == 0x0401 -> score += 0                    // Ё uppercase
            code in 0x0020..0x007E -> score += 0            // ascii — neutral
            code == 0x00FFFD -> score -= 10                 // replacement char — awful
            code in 0x00A0..0x00BF -> score -= 3            // Latin‑1 punct » ° ² — bad
            code in 0x00C0..0x00FF -> score -= 3            // Latin‑1 letters Ã Ð Ñ — bad
            code in 0x2000..0x206F -> score -= 3            // smart quotes / dashes — bad
            else -> score -= 1
        }
    }
    return score
}

sealed class TagFileWriteResult {
    object Success : TagFileWriteResult()
    /** Launch [pendingIntent], then call [AudioTagEditor.writeFile] again for the same path. */
    data class NeedsWriteConsent(val pendingIntent: PendingIntent) : TagFileWriteResult()
    data class Error(val cause: Throwable) : TagFileWriteResult()
}

/**
 * Reads/writes embedded tags (ID3, Vorbis, MP4, …) via JAudioTagger.
 */
object AudioTagEditor {

    fun readFile(path: String): Result<TagEditDraft> {
        return try {
            val file = File(path)
            if (!file.isFile || !file.canRead()) {
                return Result.failure(IllegalStateException("file not readable"))
            }
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tagOrCreateDefault
            val rawTrack = tag.getFirst(FieldKey.TRACK).orEmpty()
            // Some tag formats store TRACK as "3/12"; show only the leading number to the user.
            val trackOnly = rawTrack.substringBefore('/').trim()
            val coverBytes = runCatching {
                tag.firstArtwork?.binaryData
            }.getOrNull()
            Result.success(
                TagEditDraft(
                    title = maybeFixMojibake(tag.getFirst(FieldKey.TITLE)),
                    artist = maybeFixMojibake(tag.getFirst(FieldKey.ARTIST)),
                    album = maybeFixMojibake(tag.getFirst(FieldKey.ALBUM)),
                    albumArtist = maybeFixMojibake(tag.getFirst(FieldKey.ALBUM_ARTIST)),
                    year = maybeFixMojibake(tag.getFirst(FieldKey.YEAR)),
                    genre = maybeFixMojibake(tag.getFirst(FieldKey.GENRE)),
                    lyricist = maybeFixMojibake(tag.getFirst(FieldKey.LYRICIST)),
                    comment = maybeFixMojibake(tag.getFirst(FieldKey.COMMENT)),
                    trackNumber = trackOnly,
                    embeddedCoverBytes = coverBytes,
                ),
            )
        } catch (e: Exception) {
            Timber.e(e, "readFile")
            Result.failure(e)
        }
    }

    /**
     * Writes tags and optionally embeds cover art (JPEG/PNG bytes).
     * [mimeType] e.g. `image/jpeg` or `image/png`.
     *
     * On scoped storage (Download, etc.) [File.canWrite] is often false even when MediaStore
     * allows writing via content URI — use [context] so we can copy → edit in cache → write back.
     */
    fun writeFile(
        context: Context,
        path: String,
        draft: TagEditDraft,
        coverBytes: ByteArray?,
        coverMimeType: String?,
        writeConsentGranted: Boolean = false,
    ): TagFileWriteResult {
        val file = File(path)
        if (!file.isFile) {
            return TagFileWriteResult.Error(IllegalStateException("not a file"))
        }
        if (file.canWrite()) {
            Stage4DebugLog.i("writeFile direct path=$path")
            return writeTagsToFile(file, draft, coverBytes, coverMimeType).toTagFileWriteResult()
        }
        if (!writeConsentGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AudioFileActions.createWriteRequestOrNull(context, path)?.let { pi ->
                Stage4DebugLog.i("writeFile needs createWriteRequest path=$path")
                return TagFileWriteResult.NeedsWriteConsent(pi)
            }
        }
        val uri = AudioFileActions.audioContentUri(context, path)
            ?: return TagFileWriteResult.Error(IllegalStateException("no MediaStore uri for: $path"))
        Stage4DebugLog.i("writeFile via MediaStore uri=$uri path=$path")
        return writeTagsViaContentUri(context, uri, file, draft, coverBytes, coverMimeType)
    }

    private fun Result<Unit>.toTagFileWriteResult(): TagFileWriteResult =
        fold(
            onSuccess = { TagFileWriteResult.Success },
            onFailure = { TagFileWriteResult.Error(it) },
        )

    private fun writeTagsViaContentUri(
        context: Context,
        uri: Uri,
        sourceFile: File,
        draft: TagEditDraft,
        coverBytes: ByteArray?,
        coverMimeType: String?,
    ): TagFileWriteResult {
        val ext = sourceFile.extension.takeIf { it.isNotBlank() } ?: "mp3"
        val temp = File.createTempFile("tag_edit_", ".$ext", context.cacheDir)
        return try {
            var copied = copyUriToFile(context, uri, temp)
            if (!copied) {
                copied = copyUriToFile(context, uri, temp, useReadOnly = true)
            }
            if (!copied && sourceFile.canRead()) {
                sourceFile.copyTo(temp, overwrite = true)
                copied = temp.isFile && temp.length() > 0L
            }
            if (!temp.isFile || temp.length() == 0L) {
                return TagFileWriteResult.Error(IllegalStateException("could not read audio for tag write"))
            }
            writeTagsToFile(temp, draft, coverBytes, coverMimeType).getOrElse {
                return TagFileWriteResult.Error(it)
            }
            when (val copy = copyFileToUri(context, uri, temp)) {
                is TagFileWriteResult.Success -> TagFileWriteResult.Success
                is TagFileWriteResult.NeedsWriteConsent -> copy
                is TagFileWriteResult.Error -> copy
            }
        } catch (e: Exception) {
            Timber.e(e, "writeTagsViaContentUri")
            TagFileWriteResult.Error(e)
        } finally {
            temp.delete()
        }
    }

    private fun copyUriToFile(
        context: Context,
        uri: Uri,
        dest: File,
        useReadOnly: Boolean = false,
    ): Boolean = runCatching {
        val mode = if (useReadOnly) "r" else "rw"
        try {
            context.contentResolver.openFileDescriptor(uri, mode)?.use { pfd ->
                java.io.FileInputStream(pfd.fileDescriptor).use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                return@runCatching dest.isFile && dest.length() > 0L
            }
        } catch (_: Exception) {
            // fall through to InputStream
        }
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        dest.isFile && dest.length() > 0L
    }.getOrDefault(false)

    private fun copyFileToUri(context: Context, uri: Uri, source: File): TagFileWriteResult {
        return try {
            writeBytesToMediaUri(context, uri, source)
            TagFileWriteResult.Success
        } catch (e: SecurityException) {
            recoverableWriteResultOrNull(e)?.let { return it }
            Timber.e(e, "copyFileToUri security uri=%s", uri)
            TagFileWriteResult.Error(e)
        } catch (e: Exception) {
            Timber.e(e, "copyFileToUri uri=%s", uri)
            TagFileWriteResult.Error(e)
        }
    }

    private fun writeBytesToMediaUri(context: Context, uri: Uri, source: File) {
        var usedPendingFlag = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val pending = android.content.ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
                context.contentResolver.update(uri, pending, null, null)
                usedPendingFlag = true
            }.onFailure { e ->
                Stage4DebugLog.w("IS_PENDING skipped uri=$uri err=${e.message}")
            }
        }
        try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                source.inputStream().use { it.copyTo(out) }
            } ?: context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                java.io.FileOutputStream(pfd.fileDescriptor).use { out ->
                    source.inputStream().use { it.copyTo(out) }
                }
            } ?: error("could not open output stream for $uri")
        } finally {
            if (usedPendingFlag && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching {
                    val done = android.content.ContentValues().apply {
                        put(MediaStore.Audio.Media.IS_PENDING, 0)
                    }
                    context.contentResolver.update(uri, done, null, null)
                }
            }
        }
    }

    private fun recoverableWriteResultOrNull(e: SecurityException): TagFileWriteResult? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        if (e !is android.app.RecoverableSecurityException) return null
        Stage4DebugLog.i("writeFile RecoverableSecurityException — needs user consent")
        return TagFileWriteResult.NeedsWriteConsent(e.userAction.actionIntent)
    }

    private fun writeTagsToFile(
        file: File,
        draft: TagEditDraft,
        coverBytes: ByteArray?,
        coverMimeType: String?,
    ): Result<Unit> {
        return try {
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tagOrCreateDefault
            val title = draft.title.trim().ifBlank { file.nameWithoutExtension.ifBlank { "Unknown" } }
            applyTextField(tag, FieldKey.TITLE, title)
            applyTextField(tag, FieldKey.ARTIST, draft.artist)
            applyTextField(tag, FieldKey.ALBUM, draft.album)
            applyTextField(tag, FieldKey.ALBUM_ARTIST, draft.albumArtist)
            applyTextField(tag, FieldKey.YEAR, draft.year)
            applyTextField(tag, FieldKey.GENRE, draft.genre)
            applyTextField(tag, FieldKey.LYRICIST, draft.lyricist)
            applyTextField(tag, FieldKey.COMMENT, draft.comment)
            val trackTrimmed = draft.trackNumber.trim()
            if (trackTrimmed.isNotEmpty()) {
                applyTextField(tag, FieldKey.TRACK, trackTrimmed)
            } else {
                runCatching { tag.deleteField(FieldKey.TRACK) }
            }
            when {
                coverBytes != null && coverBytes.isEmpty() -> tag.deleteArtworkField()
                coverBytes != null && coverBytes.isNotEmpty() -> {
                    tag.deleteArtworkField()
                    val mime = coverMimeType?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
                    val art: Artwork = StandardArtwork().apply {
                        binaryData = coverBytes
                        mimeType = mime
                    }
                    tag.addField(art)
                }
            }
            AudioFileIO.write(audioFile)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "writeTagsToFile")
            Result.failure(e)
        }
    }

    /**
     * ID3v2 throws [org.jaudiotagger.tag.FieldDataInvalidException] when setting empty text on a
     * missing/corrupt frame — delete the field instead of writing "".
     */
    private fun applyTextField(tag: Tag, key: FieldKey, value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            runCatching { tag.deleteField(key) }
            return
        }
        runCatching { tag.setField(key, trimmed) }.onFailure { e ->
            Timber.w(e, "setField %s failed, delete then retry", key)
            runCatching { tag.deleteField(key) }
            tag.setField(key, trimmed)
        }
    }
}

data class TagEditDraft(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArtist: String = "",
    val year: String = "",
    val genre: String = "",
    val lyricist: String = "",
    val comment: String = "",
    val trackNumber: String = "",
    /** Embedded artwork already present in the file (read-only; UI uses [TagEditorUiState.pickedCoverBytes] for new picks). */
    val embeddedCoverBytes: ByteArray? = null,
) {
    /**
     * Generated equality/hashCode helpers handle [embeddedCoverBytes] correctly without crashing
     * on different array identities (default `data class` uses `==` which compares references).
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TagEditDraft) return false
        if (title != other.title) return false
        if (artist != other.artist) return false
        if (album != other.album) return false
        if (albumArtist != other.albumArtist) return false
        if (year != other.year) return false
        if (genre != other.genre) return false
        if (lyricist != other.lyricist) return false
        if (comment != other.comment) return false
        if (trackNumber != other.trackNumber) return false
        if (embeddedCoverBytes != null) {
            if (other.embeddedCoverBytes == null) return false
            if (!embeddedCoverBytes.contentEquals(other.embeddedCoverBytes)) return false
        } else if (other.embeddedCoverBytes != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + album.hashCode()
        result = 31 * result + albumArtist.hashCode()
        result = 31 * result + year.hashCode()
        result = 31 * result + genre.hashCode()
        result = 31 * result + lyricist.hashCode()
        result = 31 * result + comment.hashCode()
        result = 31 * result + trackNumber.hashCode()
        result = 31 * result + (embeddedCoverBytes?.contentHashCode() ?: 0)
        return result
    }
}
