package com.generic.audioplayes.util

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import timber.log.Timber
import java.io.File

/**
 * Result of [deleteAudioFileFromDevice] / [deleteAudioFileByUri].
 */
sealed class MediaDeleteResult {
    object Success : MediaDeleteResult()
    object Failed : MediaDeleteResult()
    /** User must confirm via [pendingIntent] (system UI); then retry delete for the same [uri]. */
    data class Recoverable(val pendingIntent: PendingIntent, val uri: Uri) : MediaDeleteResult()
}

/**
 * System-level actions on audio files (ringtone, external tag editors).
 * Does not modify the in-app library schema.
 */
object AudioFileActions {

    /**
     * Deletes an indexed audio file via [MediaStore] / [android.content.ContentResolver.delete].
     * Prefer this over [File.delete] on Android 10+ (scoped storage).
     * On [MediaDeleteResult.Recoverable], launch [PendingIntent] with [androidx.activity.result.IntentSenderRequest]
     * and call [deleteAudioFileByUri] again after [android.app.Activity.RESULT_OK].
     */
    fun deleteAudioFileFromDevice(context: Context, filePath: String): MediaDeleteResult {
        val uri = resolveAudioContentUri(context, filePath) ?: return MediaDeleteResult.Failed
        return deleteAudioFileByUri(context, uri)
    }

    /**
     * Same as [deleteAudioFileFromDevice], then if MediaStore delete fails (and the failure is not
     * [MediaDeleteResult.Recoverable]), tries [File.delete] for direct paths (some OEMs / older APIs).
     */
    fun deleteAudioFileFromDeviceWithFallback(context: Context, filePath: String): MediaDeleteResult {
        return when (val r = deleteAudioFileFromDevice(context, filePath)) {
            MediaDeleteResult.Success -> r
            is MediaDeleteResult.Recoverable -> r
            MediaDeleteResult.Failed -> deleteFileDirectlyIfPossible(filePath)
        }
    }

    private fun deleteFileDirectlyIfPossible(filePath: String): MediaDeleteResult {
        return try {
            val f = File(filePath)
            if (f.exists() && f.isFile && f.delete()) MediaDeleteResult.Success else MediaDeleteResult.Failed
        } catch (e: Exception) {
            Timber.e(e, "deleteFileDirectlyIfPossible")
            MediaDeleteResult.Failed
        }
    }

    /**
     * Deletes by an already-resolved content [uri] (same [uri] must be used after recoverable confirmation).
     */
    fun deleteAudioFileByUri(context: Context, uri: Uri): MediaDeleteResult {
        return try {
            if (context.contentResolver.delete(uri, null, null) > 0) {
                MediaDeleteResult.Success
            } else {
                MediaDeleteResult.Failed
            }
        } catch (e: RecoverableSecurityException) {
            // User action is [android.app.RemoteAction]; use its PendingIntent for [StartIntentSenderForResult].
            MediaDeleteResult.Recoverable(e.userAction.actionIntent, uri)
        } catch (e: SecurityException) {
            Timber.e(e, "deleteAudioFileByUri")
            MediaDeleteResult.Failed
        }
    }

    private fun resolveAudioContentUri(context: Context, filePath: String): Uri? {
        // API 33+: canonical content Uri from a file Uri (preferred when DATA column is absent).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            for (candidate in pathCandidatesForMediaLookup(filePath)) {
                try {
                    val canonical = MediaStore.getMediaUri(context, Uri.fromFile(File(candidate)))
                    if (canonical != null) return canonical
                } catch (e: Exception) {
                    Timber.e(e, "resolveAudioContentUri getMediaUri(Uri)")
                }
            }
        }
        return audioContentUri(context, filePath)
    }

    fun audioContentUri(context: Context, filePath: String): Uri? {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val dataColumn = MediaStore.Audio.Media.DATA
        for (candidate in pathCandidatesForMediaLookup(filePath)) {
            context.contentResolver.query(
                collection,
                projection,
                "$dataColumn = ?",
                arrayOf(candidate),
                null,
            )?.use { c ->
                if (c.moveToFirst()) {
                    val id = c.getLong(0)
                    return ContentUris.withAppendedId(collection, id)
                }
            }
        }
        return null
    }

    /** Paths that may match MediaStore.DATA when the app stores a different string form. */
    private fun pathCandidatesForMediaLookup(filePath: String): List<String> {
        return buildList {
            add(filePath)
            try {
                add(File(filePath).canonicalPath)
            } catch (_: Exception) {
            }
            try {
                add(File(filePath).absolutePath)
            } catch (_: Exception) {
            }
        }.distinct()
    }

    fun setAsRingtone(context: Context, filePath: String): Boolean {
        val uri = audioContentUri(context, filePath) ?: return false
        return try {
            RingtoneManager.setActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_RINGTONE,
                uri,
            )
            true
        } catch (e: Exception) {
            Timber.e(e, "setAsRingtone")
            false
        }
    }

    fun tryOpenAudioTagEditor(context: Context, filePath: String): Boolean {
        val uri = audioContentUri(context, filePath) ?: return false
        val intent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(uri, "audio/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            context.startActivity(Intent.createChooser(intent, null))
            true
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "tryOpenAudioTagEditor")
            false
        }
    }

    fun tryOpenAudioForCoverChange(context: Context, filePath: String): Boolean {
        // Same entry as tag edit: album art is embedded in ID3/Vorbis by most editors.
        return tryOpenAudioTagEditor(context, filePath)
    }
}
