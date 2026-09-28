package com.denilson.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.denilson.music.ui.components.QuickButton
import com.denilson.music.ui.theme.MaxMagenta
import com.denilson.music.ui.theme.MaxViolet
import com.denilson.music.ui.theme.maxGradient
import com.denilson.music.ui.viewmodels.MainViewModel

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
fun PlayerScreen(vm: MainViewModel, onBack: () -> Unit) {
    val player = vm.player
    val nowPlaying by player.nowPlaying.collectAsStateWithLifecycle()
    val isPlaying by player.isPlaying.collectAsStateWithLifecycle()
    val position by player.position.collectAsStateWithLifecycle()
    val duration by player.duration.collectAsStateWithLifecycle()
    val shuffle by player.shuffle.collectAsStateWithLifecycle()
    val repeatMode by player.repeatMode.collectAsStateWithLifecycle()
    val speed by player.speed.collectAsStateWithLifecycle()

    val speeds = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Reproduciendo ahora",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Text(
                    text = nowPlaying?.album.orEmpty().ifBlank { "Denilson Music" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 44.dp, vertical = 12.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(maxGradient()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(96.dp)
            )
        }

        Text(
            text = nowPlaying?.title.orEmpty().ifBlank { "Nada reproduciendose" },
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp)
        )
        Text(
            text = nowPlaying?.artist.orEmpty(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 28.dp)
        )


        // ---------- Barra de progreso ----------
        Slider(
            value = position.coerceIn(0L, duration.coerceAtLeast(1L)).toFloat(),
            onValueChange = { player.seekTo(it.toLong()) },
            valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = MaxMagenta,
                activeTrackColor = MaxViolet,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(position), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(formatTime(duration), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }

        // ---------- Controles ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (shuffle) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { player.setShuffle(!shuffle) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = "Aleatorio",
                    tint = if (shuffle) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.SkipPrevious,
                contentDescription = "Anterior",
                tint = Color.White,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { player.previous() }
                    .padding(4.dp)
            )
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(maxGradient())
                    .clickable { player.playPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            Icon(
                Icons.Rounded.SkipNext,
                contentDescription = "Siguiente",
                tint = Color.White,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { player.next() }
                    .padding(4.dp)
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (repeatMode != Player.REPEAT_MODE_OFF)
                            MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .clickable { player.cycleRepeatMode() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (repeatMode == Player.REPEAT_MODE_ONE)
                        Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = "Repetir",
                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---------- Ajustes rapidos ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickButton(
                icon = Icons.Rounded.Speed,
                label = "Velocidad x$speed",
                modifier = Modifier.weight(1f)
            ) {
                val next = speeds[(speeds.indexOf(speed) + 1).coerceAtMost(speeds.lastIndex)]
                player.setSpeed(next)
            }
            QuickButton(
                icon = Icons.Rounded.Bedtime,
                label = "Temporizador 30m",
                modifier = Modifier.weight(1f)
            ) {
                player.sleepTimer(30)
            }
        }
    }
}
