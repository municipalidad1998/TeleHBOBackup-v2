package com.denilson.music.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisDao {

    @Upsert
    suspend fun upsert(entity: AnalysisEntity)

    @Query("SELECT * FROM analysis WHERE songId = :songId")
    suspend fun getBySong(songId: Long): AnalysisEntity?

    @Query(
        "SELECT a.* FROM analysis a INNER JOIN songs s ON a.songId = s.id WHERE s.albumId = :albumId"
    )
    suspend fun getByAlbum(albumId: Long): List<AnalysisEntity>

    @Query("SELECT * FROM analysis")
    suspend fun getAll(): List<AnalysisEntity>

    @Query("SELECT COUNT(*) FROM analysis")
    suspend fun count(): Int

    @Query("DELETE FROM analysis")
    suspend fun clear()
}
