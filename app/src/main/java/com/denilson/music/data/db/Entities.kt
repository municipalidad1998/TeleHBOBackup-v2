package com.denilson.music.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val dataPath: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val folder: String,
    val durationMs: Long,
    val year: Int,
    val format: String,
    val genre: String,
    val trackNumber: Int,
    val sizeBytes: Long,
    val dateAdded: Long = 0L,
    val favorite: Boolean = false,
    val userEdited: Boolean = false,
    val customCoverPath: String? = null
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: Long,
    val position: Int
)

@Entity(tableName = "analysis")
data class AnalysisEntity(
    @PrimaryKey val songId: Long,
    val gainDb: Float,
    val peakDb: Float,
    val loudnessLufs: Float,
    val silenceStartMs: Long,
    val endTrimMs: Long,
    val analyzedAt: Long = System.currentTimeMillis()
)
