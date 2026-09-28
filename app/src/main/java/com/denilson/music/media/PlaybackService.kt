package com.denilson.music.media

import android.os.Bundle
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.denilson.music.data.datastore.AppSettings
import com.denilson.music.data.datastore.CrossfadeSettings
import com.denilson.music.data.datastore.NormalizationMode
import com.denilson.music.data.datastore.SettingsRepository
import com.denilson.music.data.datastore.SilenceSettings
import com.denilson.music.data.repository.MusicRepository
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow
import android.content.Intent
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    companion object {
        const val ACTION_SLEEP_TIMER = "com.denilson.music.SLEEP_TIMER"
        const val EXTRA_MINUTES = "minutes"
    }

    @Inject lateinit var settingsRepo: SettingsRepository
    @Inject lateinit var repository: MusicRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var session: MediaSession
    private var player: ExoPlayer? = null
    private lateinit var fx: AudioFx

    private var settings: AppSettings = AppSettings()
    private var primaryGain = 1f
    private var sleepJob: Job? = null
    private var fadeJob: Job? = null

    // ------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        fx = AudioFx()

        val p = createPlayer()
        player = p
        session = MediaSession.Builder(this, p)
            .setCallback(sessionCallback)
            .build()

        scope.launch { monitorPosition() }
        scope.launch {
            settingsRepo.settings.collect {
                settings = it
                fx.update(it.eq)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        session.release()
        player?.release()
        player = null
        fx.release()
        scope.cancel()
        super.onDestroy()
    }

    // ------------------------------------------------------------------
    // ReproducciÃ³n / crossfade

    private fun createPlayer(): ExoPlayer = ExoPlayer.Builder(this)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_LOCAL)
        .setSeekBackIncrementMs(10_000L)
        .setSeekForwardIncrementMs(10_000L)
        .build()
        .also { it.addListener(playerListener) }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val p = player ?: return
            applyForCurrentItem(p)
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            fx.attach(audioSessionId, settings.eq)
        }

        override fun onPlayerError(error: PlaybackException) {
            // Avanza a la siguiente pista ante archivos corruptos
            player?.seekToNextMediaItem()
            player?.prepare()
            player?.play()
        }
    }

    /** Aplica eliminar-silencio-inicial y normalizaciÃ³n a la pista actual. */
    private fun applyForCurrentItem(p: Player) {
        val songId = p.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        scope.launch {
            val analysis = repository.getAnalysis(songId)
            val song = repository.getById(songId)

            if (settings.silence.removeInitialSilence && analysis != null &&
                analysis.silenceStartMs > 0 && p.currentPosition < 500L
            ) {
                p.seekTo(analysis.silenceStartMs)
            }

            val gainDb = when (settings.normalization) {
                NormalizationMode.OFF -> null
                NormalizationMode.TRACK -> analysis?.gainDb
                NormalizationMode.ALBUM -> song?.let { repository.albumGain(it.albumId) }
            }
            primaryGain = if (gainDb != null) (10.0.pow(gainDb / 20.0)).toFloat() else 1f
            p.volume = primaryGain
        }
    }

    private suspend fun monitorPosition() {
        while (kotlinx.coroutines.currentCoroutineContext().isActive) {
            val p = player
            if (p != null && settings.crossfade.enabled && fadeJob?.isActive != true && p.isPlaying) {
                val duration = p.duration
                if (duration != C.TIME_UNSET && duration > 0L) {
                    val exit = settings.crossfade.exitPointMs.coerceAtLeast(500L)
                    val remaining = duration - p.currentPosition
                    val hasNext = p.mediaItemCount > p.currentMediaItemIndex + 1
                    if (remaining <= exit && hasNext) {
                        startCrossfade()
                    }
                }
            }
            delay(250L)
        }
    }

    /**
     * Crossfade manual: arranca la siguiente canciÃ³n en el punto de entrada
     * configurado y hace el fundido con el punto de salida configurado.
     */
    private fun startCrossfade() {
        val cur = player ?: return
        val idx = cur.currentMediaItemIndex
        if (idx + 1 >= cur.mediaItemCount) return
        val nextItem = cur.getMediaItemAt(idx + 1)

        val fadeInPlayer = createPlayer()
        fadeInPlayer.setMediaItem(nextItem)
        fadeInPlayer.seekTo(settings.crossfade.entryPointMs.coerceAtLeast(0L))
        fadeInPlayer.volume = 0f
        fadeInPlayer.prepare()
        fadeInPlayer.play()

        fadeJob = scope.launch {
            val nextGain = nextItem.mediaId?.toLongOrNull()?.let { songId ->
                val analysis = repository.getAnalysis(songId)
                val song = repository.getById(songId)
                val gainDb = when (settings.normalization) {
                    NormalizationMode.OFF -> null
                    NormalizationMode.TRACK -> analysis?.gainDb
                    NormalizationMode.ALBUM -> song?.let { repository.albumGain(it.albumId) }
                }
                if (gainDb != null) (10.0.pow(gainDb / 20.0)).toFloat() else 1f
            } ?: 1f

            val fade = settings.crossfade.durationMs.coerceAtLeast(100L)
            val startedAt = SystemClock.elapsedRealtime()
            val baseGain = primaryGain

            while (true) {
                val t = ((SystemClock.elapsedRealtime() - startedAt) / fade.toFloat())
                    .coerceIn(0f, 1f)
                cur.volume = baseGain * (1f - t)
                fadeInPlayer.volume = nextGain * t
                if (t >= 1f) break
                delay(20L)
            }

            cur.stop()
            cur.release()
            player = fadeInPlayer
            primaryGain = nextGain
            fadeInPlayer.volume = nextGain
            session.setPlayer(fadeInPlayer)
            applyForCurrentItem(fadeInPlayer)
            fadeJob = null
        }
    }

    // ------------------------------------------------------------------
    // SesiÃ³n

    private val sessionCallback = object : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                .buildUpon()
                .add(SessionCommand(ACTION_SLEEP_TIMER, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == ACTION_SLEEP_TIMER) {
                val minutes = args.getInt(EXTRA_MINUTES, -1)
                handleSleepTimer(minutes)
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    /** Temporizador de apagado: pausa la reproducciÃ³n tras N minutos (0 = cancelar). */
    private fun handleSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        if (minutes <= 0) return
        sleepJob = scope.launch {
            delay(minutes * 60_000L)
            player?.pause()
        }
    }
}
