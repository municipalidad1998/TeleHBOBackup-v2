package com.denilson.music.media

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.denilson.music.data.db.SongEntity
import java.io.File

object MediaItemFactory {

    private const val ALBUM_ART_URI = "content://media/external/audio/albumart"

    fun from(song: SongEntity): MediaItem {
        val artworkUri = customArtwork(song) ?: ContentUris.withAppendedId(
            Uri.parse(ALBUM_ART_URI), song.albumId
        ).toString()

        val metadata = MediaMetadata.Builder()
            .setTitle(song.title.ifBlank { "Título desconocido" })
            .setArtist(song.artist.ifBlank { "Artista desconocido" })
            .setAlbumTitle(song.album.ifBlank { "Álbum desconocido" })
            .setArtworkUri(Uri.parse(artworkUri))
            .setDurationMs(song.durationMs)
            .build()

        // Determinar URI confiable: si dataPath existe y es accesible, usar archivo directo;
        // de lo contrario usar MediaStore URI.
        val mediaUri = if (song.dataPath.isNotBlank() && File(song.dataPath).exists()) {
            Uri.fromFile(File(song.dataPath))
        } else {
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
        }

        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(mediaUri)
            .setMediaMetadata(metadata)
            .build()
    }

    fun customArtwork(song: SongEntity): String? =
        song.customCoverPath?.let { File(it).toURI().toString() }
}
