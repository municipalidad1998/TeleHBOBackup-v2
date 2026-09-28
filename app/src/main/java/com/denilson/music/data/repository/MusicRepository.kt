package com.denilson.music.data.repository

import com.denilson.music.data.datastore.AppSettings
import com.denilson.music.data.datastore.SettingsRepository
import com.denilson.music.data.db.AnalysisDao
import com.denilson.music.data.db.AnalysisEntity
import com.denilson.music.data.db.AppDatabase
import com.denilson.music.data.db.PlaylistDao
import com.denilson.music.data.db.PlaylistEntity
import com.denilson.music.data.db.PlaylistSongEntity
import com.denilson.music.data.db.SongDao
import com.denilson.music.data.db.SongEntity
import com.denilson.music.data.processing.AudioAnalyzer
import com.denilson.music.data.scanner.MediaStoreScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class ScanResult(val added: Int, val removed: Int, val total: Int)

data class AnalyzeProgress(
    val running: Boolean = false,
    val processed: Int = 0,
    val total: Int = 0
)

@Singleton
class MusicRepository @Inject constructor(
    db: AppDatabase,
    private val scanner: MediaStoreScanner,
    private val settingsRepo: SettingsRepository
) {
    private val songDao: SongDao = db.songDao()
    private val playlistDao: PlaylistDao = db.playlistDao()
    private val analysisDao: AnalysisDao = db.analysisDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val analyzeProgress = MutableStateFlow(AnalyzeProgress())

    fun observeSongs(): Flow<List<SongEntity>> = songDao.observeAll()
    fun observeFavorites(): Flow<List<SongEntity>> = songDao.observeFavorites()
    fun observeRecentlyAdded(): Flow<List<SongEntity>> = songDao.observeRecentlyAdded()
    fun observeByAlbum(albumId: Long): Flow<List<SongEntity>> = songDao.observeByAlbum(albumId)
    fun observeByFolder(folder: String): Flow<List<SongEntity>> = songDao.observeByFolder(folder)

    suspend fun getById(id: Long): SongEntity? = songDao.getById(id)
    suspend fun getAllSongs(): List<SongEntity> = songDao.getAll()

    /** Escanea MediaStore; sincroniza con Room sin duplicar entradas. */
    suspend fun scan(): ScanResult {
        val found = scanner.query()
        val existing = songDao.getAll().associateBy { it.id }

        val merged = found.map { fresh ->
            val old = existing[fresh.id]
            when {
                old == null -> fresh
                // Preserva ediciones del usuario, favoritos y portada personalizada
                old.userEdited -> fresh.copy(
                    title = old.title,
                    artist = old.artist,
                    album = old.album,
                    genre = old.genre,
                    year = old.year,
                    trackNumber = old.trackNumber,
                    favorite = old.favorite,
                    userEdited = true,
                    customCoverPath = old.customCoverPath
                )
                else -> fresh.copy(
                    favorite = old.favorite,
                    customCoverPath = old.customCoverPath
                )
            }
        }

        songDao.upsertAll(merged)

        val foundIds = found.map { it.id }.toSet()
        val gone = existing.keys.filterNot { it in foundIds }
        if (gone.isNotEmpty()) songDao.deleteByIds(gone)

        settingsRepo.setLastScan(System.currentTimeMillis())
        return ScanResult(added = merged.count { existing[it.id] == null }, removed = gone.size, total = merged.size)
    }

    suspend fun toggleFavorite(song: SongEntity) = songDao.setFavorite(song.id, !song.favorite)

    suspend fun updateMetadata(
        song: SongEntity,
        title: String,
        artist: String,
        album: String,
        genre: String,
        year: Int,
        trackNumber: Int,
        coverPath: String?
    ) = songDao.updateMetadata(
        song.id, title, artist, album, genre, year, trackNumber, coverPath ?: song.customCoverPath
    )

    // ---------- Playlists ----------

    fun observePlaylists(): Flow<List<PlaylistEntity>> = playlistDao.observeAll()
    fun observePlaylistSongs(playlistId: Long): Flow<List<PlaylistSongEntity>> =
        playlistDao.observeSongs(playlistId)

    suspend fun createPlaylist(name: String): Long =
        playlistDao.insertPlaylist(PlaylistEntity(name = name))

    suspend fun renamePlaylist(id: Long, name: String) = playlistDao.rename(id, name)

    suspend fun deletePlaylist(id: Long) {
        playlistDao.clearSongs(id)
        playlistDao.delete(id)
    }

    suspend fun addToPlaylist(playlistId: Long, songId: Long) {
        val max = playlistDao.maxPosition(playlistId) ?: -1
        playlistDao.insertSong(PlaylistSongEntity(playlistId, songId, max + 1))
    }

    suspend fun removeFromPlaylist(playlistId: Long, songId: Long) =
        playlistDao.removeSong(playlistId, songId)

    suspend fun moveSong(playlistId: Long, songId: Long, delta: Int) {
        val songs = playlistDao.getSongs(playlistId)
        val index = songs.indexOfFirst { it.songId == songId }
        if (index == -1) return
        val target = index + delta
        if (target < 0 || target >= songs.size) return
        val other = songs[target]
        playlistDao.setPosition(playlistId, songs[index].songId, other.position)
        playlistDao.setPosition(playlistId, other.songId, songs[index].position)
    }

    // ---------- Análisis (silencio + normalización) ----------

    suspend fun getAnalysis(songId: Long): AnalysisEntity? = analysisDao.getBySong(songId)

    /** Ganancia promedio del álbum (modo normalización por álbum). */
    suspend fun albumGain(albumId: Long): Float? {
        val song = songDao.getAll().firstOrNull { it.albumId == albumId } ?: return null
        val analyses = analysisDao.getByAlbum(albumId)
        if (analyses.isEmpty()) return null
        return analyses.map { it.gainDb }.average().toFloat()
    }

    suspend fun analyzeSong(song: SongEntity): AnalysisEntity? {
        val settings = settingsRepo.settings.first()
        val silence = settings.silence
        val result = AudioAnalyzer.analyze(
            path = song.dataPath,
            silenceThresholdDb = silence.thresholdDb + (0.5f - silence.sensitivity) * 20f,
            minSilenceMs = silence.minSilenceMs
        ) ?: return null
        val entity = AnalysisEntity(
            songId = song.id,
            gainDb = result.gainDb,
            peakDb = result.peakDb,
            loudnessLufs = result.loudnessLufs,
            silenceStartMs = result.silenceStartMs,
            endTrimMs = result.endTrimMs
        )
        analysisDao.upsert(entity)
        return entity
    }

    /** Analiza toda la biblioteca en segundo plano; guarda resultados en Room. */
    fun analyzeAllAsync() {
        if (analyzeProgress.value.running) return
        scope.launch {
            val songs = songDao.getAll()
            analyzeProgress.value = AnalyzeProgress(running = true, total = songs.size)
            songs.forEachIndexed { index, song ->
                try {
                    if (analysisDao.getBySong(song.id) == null) analyzeSong(song)
                } catch (_: Exception) {
                }
                analyzeProgress.value = analyzeProgress.value.copy(processed = index + 1)
            }
            analyzeProgress.value = analyzeProgress.value.copy(running = false)
        }
    }
}
