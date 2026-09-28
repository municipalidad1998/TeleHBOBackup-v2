package com.denilson.music.data.scanner

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import com.denilson.music.data.db.SongEntity
import com.denilson.music.data.metadata.MetadataReader
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun query(): List<SongEntity> {
        val songs = ArrayList<SongEntity>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE
        )

        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 10000"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val iId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val iData = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val iTitle = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val iArtist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val iAlbum = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val iAlbumId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val iDuration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val iYear = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val iAdded = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val iSize = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

            while (cursor.moveToNext()) {
                try {
                    val id = cursor.getLong(iId)
                    val path = cursor.getString(iData) ?: continue
                    val file = File(path)
                    val fileName = file.name
                    val extension = fileName.substringAfterLast('.', "").uppercase()
                    var title = cursor.getString(iTitle) ?: ""
                    var artist = cursor.getString(iArtist) ?: ""
                    val album = cursor.getString(iAlbum) ?: ""
                    var genre = ""
                    var year = cursor.getInt(iYear)
                    var track = 0

                    // Detección inteligente: WhatsApp/Telegram (AUD-...-WA0001.opus, etc.)
                    if (MetadataReader.isGenericTitle(title, fileName)) {
                        val tags = MetadataReader.read(path)
                        title = if (tags.title.isNotBlank()) tags.title else ""
                        artist = if (artist.isBlank() || artist == MediaStore.UNKNOWN_STRING) {
                            if (tags.artist.isNotBlank()) tags.artist else ""
                        } else artist
                        genre = tags.genre
                        if (year == 0) year = tags.year
                        track = tags.trackNumber
                        val guess = MetadataReader.guessFromFileName(fileName)
                        if (title.isBlank() && guess != null) {
                            if (artist.isBlank()) artist = guess.first
                            title = guess.second
                        }
                        if (title.isBlank()) title = fileName.substringBeforeLast('.')
                    } else if (artist.isBlank() || artist == MediaStore.UNKNOWN_STRING) {
                        artist = ""
                    }

                    songs.add(
                        SongEntity(
                            id = id,
                            dataPath = path,
                            title = title,
                            artist = artist,
                            album = album,
                            albumId = cursor.getLong(iAlbumId),
                            folder = file.parent?.substringAfterLast('/') ?: "",
                            durationMs = cursor.getLong(iDuration),
                            year = year,
                            format = extension,
                            genre = genre,
                            trackNumber = track,
                            sizeBytes = cursor.getLong(iSize),
                            dateAdded = cursor.getLong(iAdded)
                        )
                    )
                } catch (_: Exception) {
                    // Archivo ilegible: se ignora sin romper el escaneo
                }
            }
        }
        return songs
    }
}
