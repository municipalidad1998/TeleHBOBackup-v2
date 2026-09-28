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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denilson.music.data.db.SongEntity
import com.denilson.music.ui.components.MaxTile
import com.denilson.music.ui.components.QuickButton
import com.denilson.music.ui.components.SectionHeader
import com.denilson.music.ui.navigation.Routes
import com.denilson.music.ui.theme.MaxBlue
import com.denilson.music.ui.theme.MaxCyan
import com.denilson.music.ui.theme.MaxMagenta
import com.denilson.music.ui.theme.MaxViolet
import com.denilson.music.ui.theme.YouTubeRed
import com.denilson.music.ui.theme.YtMusicRed
import com.denilson.music.ui.theme.heroGradient
import com.denilson.music.ui.theme.maxGradient
import com.denilson.music.ui.theme.ytMusicGradient
import com.denilson.music.ui.theme.youtubeGradient
import com.denilson.music.ui.viewmodels.LibraryViewModel

@Composable
fun HomeScreen(
    libVm: LibraryViewModel,
    onNavigate: (String) -> Unit
) {
    val songs by libVm.songs.collectAsStateWithLifecycle()
    val recent by libVm.recent.collectAsStateWithLifecycle()
    val favorites by libVm.favorites.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item { HeroBanner(totalSongs = songs.size) }

        // ---------- Botones horizontales de acceso ----------
        item { SectionHeader("Accesos rapidos", maxGradient()) }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    QuickButton(Icons.Rounded.LibraryMusic, "Mis canciones",
                        tint = MaxCyan) { onNavigate(Routes.SONGS) }
                }
                item {
                    QuickButton(Icons.Rounded.Favorite, "Favoritos",
                        tint = YouTubeRed) { onNavigate(Routes.FAVORITES) }
                }
                item {
                    QuickButton(Icons.Rounded.QueueMusic, "Playlists",
                        tint = MaxMagenta) { onNavigate(Routes.PLAYLISTS) }
                }
                item {
                    QuickButton(Icons.Rounded.Settings, "Ajustes",
                        tint = Color.White) { onNavigate(Routes.SETTINGS) }
                }
            }
        }

        // ---------- Tarjetas principales ----------
        item { SectionHeader("Video y streaming", maxGradient()) }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MaxTile(
                    icon = Icons.Rounded.SmartDisplay,
                    title = "YouTube",
                    subtitle = "Sin anuncios",
                    brush = youtubeGradient(),
                    modifier = Modifier
                        .weight(1f)
                        .height(148.dp),
                    onClick = { onNavigate(Routes.YOUTUBE) }
                )
                MaxTile(
                    icon = Icons.Rounded.MusicNote,
                    title = "YouTube Music",
                    subtitle = "Sin anuncios",
                    brush = ytMusicGradient(),
                    modifier = Modifier
                        .weight(1f)
                        .height(148.dp),
                    onClick = { onNavigate(Routes.YOUTUBE_MUSIC) }
                )
            }
        }


        // ---------- Biblioteca local ----------
        item {
            SectionHeader(
                title = "Tu biblioteca",
                accent = Brush.linearGradient(listOf(MaxCyan, MaxBlue)),
                action = if (libVm.scanning) "Escaneando..." else "Escanear",
                onAction = { if (!libVm.scanning) libVm.scan() }
            )
        }

        if (songs.isEmpty() && !libVm.scanning) {
            item { EmptyLibrary(onScan = { libVm.scan() }) }
        }

        if (recent.isNotEmpty()) {
            item { SectionHeader("Agregadas recientemente", maxGradient()) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recent.take(20)) { song ->
                        SongCard(song = song, onClick = { libVm.play(recent, recent.indexOf(song)) })
                    }
                }
            }
        }

        if (favorites.isNotEmpty()) {
            item { SectionHeader("Tus favoritos", maxGradient()) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(favorites.take(20)) { song ->
                        SongCard(song = song, onClick = { libVm.play(favorites, favorites.indexOf(song)) })
                    }
                }
            }
        }
    }
}



@Composable
private fun HeroBanner(totalSongs: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .background(heroGradient())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "DENILSON",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (totalSongs > 0)
                    "$totalSongs canciones en tu biblioteca  -  YouTube sin anuncios"
                else "Tu musica, tu estilo  -  YouTube y YouTube Music integrados",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun SongCard(song: SongEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(maxGradient()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White,

                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = song.title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1
        )
        Text(
            text = song.artist,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyLibrary(onScan: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaxViolet,
                modifier = Modifier.size(38.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Aun no hay canciones",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Escanea tu dispositivo para llenar la biblioteca",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(14.dp))
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
                Text("Escanear ahora", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
