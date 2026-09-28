package com.denilson.music.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denilson.music.data.db.PlaylistEntity
import com.denilson.music.data.db.SongEntity
import com.denilson.music.data.repository.MusicRepository
import com.denilson.music.media.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repo: MusicRepository,
    val player: PlayerConnection
) : ViewModel() {

    val songs: StateFlow<List<SongEntity>> = repo.observeSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<SongEntity>> = repo.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recent: StateFlow<List<SongEntity>> = repo.observeRecentlyAdded()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = repo.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val analyzeProgress = repo.analyzeProgress

    var scanning by mutableStateOf(false)
        private set

    var lastScanMessage by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch { player.connect() }
    }

    fun scan() {
        if (scanning) return
        scanning = true
        viewModelScope.launch {
            try {
                val r = repo.scan()
                lastScanMessage = "Añadidas ${r.added} · eliminadas ${r.removed} · total ${r.total}"
                repo.analyzeAllAsync()
            } catch (e: Exception) {
                lastScanMessage = "Error al escanear: ${e.message}"
            } finally {
                scanning = false
            }
        }
    }

    fun play(list: List<SongEntity>, index: Int) = player.play(list, index)

    fun toggleFavorite(song: SongEntity) = viewModelScope.launch { repo.toggleFavorite(song) }

    /** Guarda los metadatos editados en la base de datos de la app. */
    fun updateMetadata(
        song: SongEntity,
        title: String,
        artist: String,
        album: String,
        genre: String,
        year: Int,
        trackNumber: Int,
        coverPath: String?
    ) = viewModelScope.launch {
        repo.updateMetadata(
            song = song,
            title = title.ifBlank { song.title },
            artist = artist,
            album = album,
            genre = genre,
            year = year,
            trackNumber = trackNumber,
            coverPath = coverPath
        )
    }

    fun createPlaylist(name: String) = viewModelScope.launch { repo.createPlaylist(name) }

    fun deletePlaylist(id: Long) = viewModelScope.launch { repo.deletePlaylist(id) }

    /** Reproduce una playlist completa mapeando sus ids a canciones existentes. */
    fun playPlaylist(playlistId: Long) = viewModelScope.launch {
        val links = repo.observePlaylistSongs(playlistId).first()
        val all = repo.getAllSongs().associateBy { it.id }
        val list = links.mapNotNull { all[it.songId] }
        if (list.isNotEmpty()) player.play(list, 0)
    }
}
