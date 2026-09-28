package com.denilson.music.data.metadata

import android.media.MediaMetadataRetriever

data class TagInfo(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val genre: String = "",
    val year: Int = 0,
    val trackNumber: Int = 0
)

object MetadataReader {

    private val GENERIC = Regex(
        "^(AUD|VID|IMG|PTT|REC|Voice|Audio|WhatsApp|Telegram|Snapchat|Record|Audio-Note)[-_ 0-9]*",
        RegexOption.IGNORE_CASE
    )

    /** true si el título parece un nombre automático de app (AUD-20260926-WA0001.opus, etc.) */
    fun isGenericTitle(title: String, fileName: String): Boolean {
        if (title.isBlank()) return true
        val base = fileName.substringBeforeLast('.')
        if (title.equals(base, ignoreCase = true)) return true
        return GENERIC.containsMatchIn(title)
    }

    /** Intenta extraer "Artista - Título" del nombre del archivo */
    fun guessFromFileName(fileName: String): Pair<String, String>? {
        val base = fileName.substringBeforeLast('.')
        val parts = base.split(" - ", " – ", limit = 2)
        return if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            parts[0].trim() to parts[1].trim()
        } else null
    }

    fun read(path: String): TagInfo {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            TagInfo(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "",
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "",
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "",
                genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE) ?: "",
                year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                    ?.toIntOrNull() ?: 0,
                trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                    ?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0
            )
        } catch (_: Exception) {
            TagInfo()
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}
