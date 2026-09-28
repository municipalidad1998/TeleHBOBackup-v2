package com.denilson.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denilson.music.data.db.SongEntity
import com.denilson.music.ui.theme.maxGradient
import com.denilson.music.ui.viewmodels.LibraryViewModel

/**
 * Busqueda instantanea sobre la biblioteca local.
 * Funciona sin Internet: filtra por titulo, artista, album y carpeta.
 */
@Composable
fun SearchScreen(libVm: LibraryViewModel) {
    val songs by libVm.songs.collectAsStateWithLifecycle()
    val playlists by libVm.playlists.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    val q = query.trim().lowercase()

    val songHits = remember(q, songs) {
        if (q.length < 2) emptyList() else songs.filter { s ->
            s.title.lowercase().contains(q) ||
                s.artist.lowercase().contains(q) ||
                s.album.lowercase().contains(q)
        }
    }
    val artistHits = remember(q, songs) {
        if (q.length < 2) emptySet() else songs
            .map { it.artist }
            .filter { it.isNotBlank() && it.lowercase().contains(q) }
            .toSortedSet(String.CASE_INSENSITIVE_ORDER)
    }
    val albumHits = remember(q, songs) {
        if (q.length < 2) emptySet() else songs
            .map { it.album }
            .filter { it.isNotBlank() && it.lowercase().contains(q) }
            .toSortedSet(String.CASE_INSENSITIVE_ORDER)
    }
    val folderHits = remember(q, songs) {
        if (q.length < 2) emptySet() else songs
            .map { it.folder }
            .filter { it.isNotBlank() && it.lowercase().contains(q) }
            .toSortedSet(String.CASE_INSENSITIVE_ORDER)
    }
    val playlistHits = remember(q, playlists) {
        if (q.length < 2) emptyList() else playlists.filter {
            it.name.lowercase().contains(q)
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            placeholder = { Text("Canciones, artistas, albunes…", color = Color(0xFF7E93AB)) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color(0xFF7E93AB)) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Borrar",
                        tint = Color(0xFF7E93AB),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { query = "" }
                            .padding(4.dp)
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        if (q.length < 2) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Escribe para buscar en tu biblioteca",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Funciona sin Internet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
            return
        }

        val total = songHits.size + artistHits.size + albumHits.size +
            folderHits.size + playlistHits.size
        if (total == 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Sin resultados",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
            return
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            if (artistHits.isNotEmpty()) {
                item { GroupLabel("Artistas", Icons.Rounded.Person, artistHits.size) }
                items(artistHits.toList()) { name ->
                    ResultRow(icon = Icons.Rounded.Person, text = name) { query = name }
                }
            }
            if (albumHits.isNotEmpty()) {
                item { GroupLabel("Albumes", Icons.Rounded.Album, albumHits.size) }
                items(albumHits.toList()) { name ->
                    ResultRow(icon = Icons.Rounded.Album, text = name) { query = name }
                }
            }
            if (playlistHits.isNotEmpty()) {
                item { GroupLabel("Playlists", Icons.Rounded.QueueMusic, playlistHits.size) }
                items(playlistHits) { pl ->
                    ResultRow(icon = Icons.Rounded.QueueMusic, text = pl.name) {
                        libVm.playPlaylist(pl.id)
                    }
                }
            }
            if (folderHits.isNotEmpty()) {
                item { GroupLabel("Carpetas", Icons.Rounded.Folder, folderHits.size) }
                items(folderHits.toList()) { name ->
                    val inFolder = songs.filter { it.folder == name }
                    ResultRow(icon = Icons.Rounded.Folder, text = name, sub = "${inFolder.size}") {
                        libVm.play(inFolder, 0)
                    }
                }
            }
            if (songHits.isNotEmpty()) {
                item { GroupLabel("Canciones", Icons.Rounded.MusicNote, songHits.size) }
                items(songHits) { song ->
                    SearchSongRow(song) { libVm.play(songHits, songHits.indexOf(song)) }
                }
            }
        }
    }
}

@Composable
private fun GroupLabel(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color(0xFF7E93AB), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$title ($count)",
            color = Color(0xFF7E93AB),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ResultRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    sub: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        if (sub.isNotEmpty()) {
            Text(text = sub, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SearchSongRow(song: SongEntity, onPlay: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(maxGradient()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1
            )
            Text(
                text = "${song.artist} · ${song.album}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

