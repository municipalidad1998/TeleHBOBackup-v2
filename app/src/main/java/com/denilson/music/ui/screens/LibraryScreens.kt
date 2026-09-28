package com.denilson.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.denilson.music.data.db.SongEntity
import com.denilson.music.ui.components.MetadataEditDialog
import com.denilson.music.ui.theme.maxGradient
import com.denilson.music.ui.viewmodels.LibraryViewModel

@Composable
fun SongsScreen(libVm: LibraryViewModel) {
    val songs by libVm.songs.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<SongEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Header(
            title = "Mis canciones",
            subtitle = "${songs.size} canciones Â· manten pulsado para editar",
            icon = Icons.Rounded.MusicNote,
            onAction = { libVm.scan() },
            actionEnabled = !libVm.scanning
        )

        if (songs.isEmpty()) {
            EmptyHint(
                text = if (libVm.scanning) "Escaneando tu dispositivo..."
                else "No hay canciones. Pulsa Escanear para buscar musica.",
                onScan = { libVm.scan() }
            )
        } else {
            PlayAllBar(count = songs.size) { libVm.play(songs, 0) }
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(songs) { song ->
                    SongRow(
                        song = song,
                        onPlay = { libVm.play(songs, songs.indexOf(song)) },
                        onFavorite = { libVm.toggleFavorite(song) },
                        onLongPress = { editing = song }
                    )
                }
            }
        }
    }

    editing?.let { song ->
        MetadataEditDialog(
            song = song,
            onDismiss = { editing = null },
            onPickCover = { },
            onSave = { title, artist, album, genre, year, track ->
                libVm.updateMetadata(
                    song = song,
                    title = title,
                    artist = artist,
                    album = album,
                    genre = genre,
                    year = year,
                    trackNumber = track,
                    coverPath = song.customCoverPath
                )
                editing = null
            }
        )
    }
}


@Composable
fun FavoritesScreen(libVm: LibraryViewModel) {
    val songs by libVm.favorites.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Header(
            title = "Favoritos",
            subtitle = "${songs.size} canciones marcadas",
            icon = Icons.Rounded.Favorite,
            onAction = null
        )
        if (songs.isEmpty()) {
            EmptyHint(text = "Marca canciones con el corazon para verlas aqui.", onScan = null)
        } else {
            PlayAllBar(count = songs.size) { libVm.play(songs, 0) }
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(songs) { song ->
                    SongRow(
                        song = song,
                        onPlay = { libVm.play(songs, songs.indexOf(song)) },
                        onFavorite = { libVm.toggleFavorite(song) }
                    )
                }
            }
        }
    }
    }



@Composable
fun Header(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onAction: (() -> Unit)?,
    actionEnabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(maxGradient()),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        if (onAction != null) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = actionEnabled, onClick = onAction),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = "Escanear",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun PlayAllBar(count: Int, onPlay: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(maxGradient())
            .clickable(onClick = onPlay)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Reproducir todo ($count)",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: SongEntity,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onLongPress: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onPlay,
                onLongClick = onLongPress
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = song.artist + " - " + song.album,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
        Icon(
            imageVector = if (song.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = "Favorito",
            tint = if (song.favorite) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onFavorite)
                .padding(7.dp)
        )
    }
}

@Composable
fun EmptyHint(text: String, onScan: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(42.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            if (onScan != null) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(maxGradient())
                        .clickable(onClick = onScan)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Escanear", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
