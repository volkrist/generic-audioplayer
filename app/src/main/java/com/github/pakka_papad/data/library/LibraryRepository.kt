package com.github.pakka_papad.data.library

import com.github.pakka_papad.data.music.SongExtractor
import com.github.pakka_papad.data.ZenCrashReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Library is cached in Room; [SongService] flows emit immediately from DB.
 * [updateLibraryFromMediaStore] runs an incremental MediaStore sync in the background.
 */
@Singleton
class LibraryRepository @Inject constructor(
    private val songExtractor: SongExtractor,
    private val crashReporter: ZenCrashReporter,
) {

    suspend fun loadLibraryFromCache(): Boolean =
        songExtractor.hasCachedLibrary()

    suspend fun updateLibraryFromMediaStore() {
        withContext(Dispatchers.IO) {
            try {
                songExtractor.syncLibraryIncremental()
            } catch (e: Exception) {
                crashReporter.logException(e)
            }
        }
    }
}
