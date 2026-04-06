package com.generic.audioplayes.tags

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.Artwork
import org.jaudiotagger.tag.images.StandardArtwork
import timber.log.Timber
import java.io.File

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
            Result.success(
                TagEditDraft(
                    title = tag.getFirst(FieldKey.TITLE).orEmpty(),
                    artist = tag.getFirst(FieldKey.ARTIST).orEmpty(),
                    album = tag.getFirst(FieldKey.ALBUM).orEmpty(),
                    albumArtist = tag.getFirst(FieldKey.ALBUM_ARTIST).orEmpty(),
                    year = tag.getFirst(FieldKey.YEAR).orEmpty(),
                    genre = tag.getFirst(FieldKey.GENRE).orEmpty(),
                    lyricist = tag.getFirst(FieldKey.LYRICIST).orEmpty(),
                    comment = tag.getFirst(FieldKey.COMMENT).orEmpty(),
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
     */
    fun writeFile(
        path: String,
        draft: TagEditDraft,
        coverBytes: ByteArray?,
        coverMimeType: String?,
    ): Result<Unit> {
        return try {
            val file = File(path)
            if (!file.isFile) {
                return Result.failure(IllegalStateException("not a file"))
            }
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tagOrCreateDefault
            tag.setField(FieldKey.TITLE, draft.title.trim())
            tag.setField(FieldKey.ARTIST, draft.artist.trim())
            tag.setField(FieldKey.ALBUM, draft.album.trim())
            tag.setField(FieldKey.ALBUM_ARTIST, draft.albumArtist.trim())
            tag.setField(FieldKey.YEAR, draft.year.trim())
            tag.setField(FieldKey.GENRE, draft.genre.trim())
            tag.setField(FieldKey.LYRICIST, draft.lyricist.trim())
            tag.setField(FieldKey.COMMENT, draft.comment.trim())
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
            Timber.e(e, "writeFile")
            Result.failure(e)
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
)
