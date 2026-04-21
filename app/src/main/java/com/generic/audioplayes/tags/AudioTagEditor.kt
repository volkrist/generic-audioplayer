package com.generic.audioplayes.tags

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
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
            val trackTrimmed = draft.trackNumber.trim()
            if (trackTrimmed.isNotEmpty()) {
                tag.setField(FieldKey.TRACK, trackTrimmed)
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
