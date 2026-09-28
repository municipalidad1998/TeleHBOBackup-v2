package com.denilson.music.media

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.denilson.music.data.db.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerConnection @Inject constructor(
    @Suppress("UNUSED_PARAMETER") @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {

    data class NowPlaying(
        val mediaId: String,
        val title: String,
        val artist: String,
        val album: String,
        val artworkUri: String?,
        val durationMs: Long
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _queue = MutableStateFlow<List<NowPlaying>>(emptyList())
    val queue: StateFlow<List<NowPlaying>> = _queue

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode

    private val _speed = MutableStateFlow(1f)
    val speed: StateFlow<Float> = _speed

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                _isPlaying.value = player.isPlaying
            }
            if (events.contains(Player.EVENT_MEDIA_METADATA_CHANGED)) {
                _nowPlaying.value = player.currentMediaItem?.toNowPlaying()
                _duration.value = player.duration.coerceAtLeast(0L)
            }
            if (events.contains(Player.EVENT_TIMELINE_CHANGED)) {
                refreshQueue(player)
            }
            if (events.contains(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)) {
                _shuffle.value = player.shuffleModeEnabled
            }
            if (events.contains(Player.EVENT_REPEAT_MODE_CHANGED)) {
                _repeatMode.value = player.repeatMode
            }
            if (events.contains(Player.EVENT_PLAYBACK_PARAMETERS_CHANGED)) {
                _speed.value = player.playbackParameters.speed
            }
        }
    }

    suspend fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val c = withContext(Dispatchers.Default) {
            MediaController.Builder(context, token).buildAsync().get()
        }
        controller = c
        c.addListener(listener)
        _isPlaying.value = c.isPlaying
        _nowPlaying.value = c.currentMediaItem?.toNowPlaying()
        _duration.value = c.duration.coerceAtLeast(0L)
        _shuffle.value = c.shuffleModeEnabled
        _repeatMode.value = c.repeatMode
        _speed.value = c.playbackParameters.speed
        refreshQueue(c)

        scope.launch {
            while (isActive) {
                _position.value = c.currentPosition.coerceAtLeast(0L)
                if (c.duration > 0) _duration.value = c.duration
                delay(250L)
            }
        }
    }

    private fun refreshQueue(player: Player) {
        val items = ArrayList<NowPlaying>()
        for (i in 0 until player.mediaItemCount) {
            items.add(player.getMediaItemAt(i).toNowPlaying())
        }
        _queue.value = items
    }

    private fun MediaItem.toNowPlaying() = NowPlaying(
        mediaId = mediaId ?: "",
        title = mediaMetadata.title?.toString() ?: "",
        artist = mediaMetadata.artist?.toString() ?: "",
        album = mediaMetadata.albumTitle?.toString() ?: "",
        artworkUri = mediaMetadata.artworkUri?.toString(),
        durationMs = mediaMetadata.durationMs ?: 0L
    )

    // ------------------------------------------------------------------
    // Controles

    fun play(songs: List<SongEntity>, startIndex: Int) {
        val c = controller ?: return
        val items = songs.map { MediaItemFactory.from(it) }
        c.setMediaItems(items, startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)), 0L)
        c.prepare()
        c.play()
    }

    fun playPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() {
        controller?.seekToNextMediaItem()
        controller?.play()
    }

    fun previous() {
        val c = controller ?: return
        // Reinicia la pista si ya avanzó más de 3 segundos
        if (c.currentPosition > 3000L) c.seekTo(0L) else c.seekToPreviousMediaItem()
        c.play()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms.coerceAtLeast(0L))
    }

    fun jumpTo(index: Int) {
        controller?.seekTo(index, 0L)
        controller?.play()
    }

    fun setShuffle(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
    }

    fun cycleRepeatMode() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setSpeed(speed: Float) {
        controller?.playbackParameters = PlaybackParameters(speed)
    }

    /** Temporizador de apagado en minutos; 0 cancela. */
    fun sleepTimer(minutes: Int) {
        val c = controller ?: return
        val args = Bundle().apply { putInt(PlaybackService.EXTRA_MINUTES, minutes) }
        c.sendCustomCommand(SessionCommand(PlaybackService.ACTION_SLEEP_TIMER, Bundle.EMPTY), args)
    }
}
