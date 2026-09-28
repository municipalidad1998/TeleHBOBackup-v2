package com.denilson.music.media

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import com.denilson.music.data.datastore.EqSettings

/**
 * Ecualizador digital del dispositivo adjunto a la sesiÃ³n de audio de ExoPlayer.
 * Bandas objetivo: 60, 150, 400, 1k, 2.4k, 6k, 15k Hz + Bass/Treble.
 */
class AudioFx {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    /** Frecuencias centrales (en mHz) de las 7 bandas configurables */
    private val targetFreqs = longArrayOf(
        60_000L, 150_000L, 400_000L, 1_000_000L, 2_400_000L, 6_000_000L, 15_000_000L
    )

    fun attach(sessionId: Int, settings: EqSettings) {
        release()
        try {
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = settings.enabled
                setStrength((settings.bass * 10).toShort())
            }
        } catch (_: Exception) {
        }
        try {
            equalizer = Equalizer(0, sessionId).apply {
                enabled = settings.enabled
                applyBands(settings)
            }
        } catch (_: Exception) {
        }
    }

    fun update(settings: EqSettings) {
        try {
            equalizer?.let {
                it.enabled = settings.enabled
                it.applyBands(settings)
            }
            bassBoost?.let {
                it.enabled = settings.enabled
                it.setStrength((settings.bass * 10).toShort())
            }
        } catch (_: Exception) {
        }
    }

    private fun Equalizer.applyBands(settings: EqSettings) {
        val bandCount = numberOfBands.toInt()
        if (bandCount <= 0) return
        val centers = LongArray(bandCount) { getCenterFreq(it.toShort()).toLong() }
        val range = bandLevelRange
        val minLevel = range[0]
        val maxLevel = range[1]

        val levels = ShortArray(bandCount)
        for ((i, freq) in targetFreqs.withIndex()) {
            val band = nearestBand(centers, freq)
            val requested = (settings.gains.getOrElse(i) { 0f } * 100).toInt().toShort()
            levels[band] = ((levels[band].toInt() + requested.toInt()) / 2).toShort()
        }

        val top = bandCount - 1
        val trebleLevel = (settings.treble * 100).toInt().toShort()
        levels[top] = ((levels[top].toInt() + trebleLevel.toInt()) / 2).toShort()

        for (b in levels.indices) {
            setBandLevel(b.toShort(), levels[b].coerceIn(minLevel, maxLevel))
        }
    }

    private fun nearestBand(centers: LongArray, freqHz: Long): Int {
        var best = 0
        var bestDist = Long.MAX_VALUE
        for (i in centers.indices) {
            val dist = Math.abs(centers[i] - freqHz)
            if (dist < bestDist) {
                bestDist = dist
                best = i
            }
        }
        return best
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (_: Exception) {
        }
        try {
            bassBoost?.release()
        } catch (_: Exception) {
        }
        equalizer = null
        bassBoost = null
    }
}
