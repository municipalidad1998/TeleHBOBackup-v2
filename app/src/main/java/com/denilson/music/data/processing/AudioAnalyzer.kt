package com.denilson.music.data.processing

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Analizador de audio 100% local. Decodifica el archivo a PCM (sin modificarlo)
 * y calcula: inicio/fin de silencio, loudness aproximado (LUFS) y pico.
 */
object AudioAnalyzer {

    data class Window(val timeMs: Long, val rmsDb: Float, val peakDb: Float)

    data class Analysis(
        val silenceStartMs: Long,
        val endTrimMs: Long,
        val loudnessLufs: Float,
        val peakDb: Float,
        val gainDb: Float
    )

    const val TARGET_LUFS = -16f
    const val MAX_BOOST_DB = 10f
    const val MAX_CUT_DB = -15f

    fun analyze(path: String, silenceThresholdDb: Float, minSilenceMs: Long, windowMs: Int = 50): Analysis? {
        val windows = decodeWindows(path, windowMs) ?: return null
        if (windows.isEmpty()) return null

        // --- Silencio inicial: primer instante con sonido real ---
        var silenceStart = 0L
        for (w in windows) {
            if (w.rmsDb >= silenceThresholdDb) {
                silenceStart = w.timeMs
                break
            }
        }

        // --- Silencio final: cola silenciosa al terminar ---
        val minRun = (minSilenceMs / windowMs).coerceAtLeast(1)
        var trailingSilent = 0
        for (i in windows.indices.reversed()) {
            if (windows[i].rmsDb < silenceThresholdDb) trailingSilent++ else break
        }
        val endTrim = if (trailingSilent >= minRun) trailingSilent * windowMs.toLong() else 0L

        // --- Loudness integrado aproximado (sin ponderaciÃ³n K, RMS-based) ---
        var sumPower = 0.0
        var peakDb = -120f
        for (w in windows) {
            sumPower += Math.pow(10.0, w.rmsDb / 10.0)
            if (w.peakDb > peakDb) peakDb = w.peakDb
        }
        val meanPower = sumPower / windows.size
        val loudnessLufs = (-0.691 + 10 * log10(meanPower + 1e-12)).toFloat()

        // --- Ganancia con protecciÃ³n contra clipping ---
        var gain = (TARGET_LUFS - loudnessLufs).coerceIn(MAX_CUT_DB, MAX_BOOST_DB)
        val maxGainBeforeClip = -1f - peakDb // dejar 1 dB de margen
        if (gain > maxGainBeforeClip) gain = maxGainBeforeClip

        return Analysis(
            silenceStartMs = silenceStart,
            endTrimMs = endTrim,
            loudnessLufs = loudnessLufs,
            peakDb = peakDb,
            gainDb = gain
        )
    }

    /** Decodifica el primer track de audio a ventanas RMS de ~windowMs. */
    fun decodeWindows(path: String, windowMs: Int): List<Window>? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        return try {
            extractor.setDataSource(path)
            var audioTrack = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrack = i
                    format = f
                    break
                }
            }
            if (audioTrack < 0 || format == null) return null
            extractor.selectTrack(audioTrack)

            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec.configure(format, null, null, 0)
            codec.start()

            val out = ArrayList<Window>()
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var samplesInWindow = 0L
            var totalSamples = 0L
            var accSq = 0.0
            var accPeak = 0.0
            val windowSamples = sampleRate.toLong() * windowMs / 1000

            while (!outputDone) {
                if (!inputDone) {
                    val idx = codec.dequeueInputBuffer(10_000)
                    if (idx >= 0) {
                        val buf = codec.getInputBuffer(idx)!!
                        val size = extractor.readSampleData(buf, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(idx, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    outIdx >= 0 -> {
                        val newFormat = codec.outputFormat
                        if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            sampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            channels = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                        val shorts = codec.getOutputBuffer(outIdx)!!
                            .order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        val count = shorts.remaining()
                        for (i in 0 until count) {
                            val v = shorts.get(i) / 32768.0
                            accSq += v * v
                            if (abs(v) > accPeak) accPeak = abs(v)
                            samplesInWindow++
                            totalSamples++
                            if (samplesInWindow >= windowSamples * channels) {
                                val rms = sqrt(accSq / samplesInWindow)
                                val timeMs = totalSamples / channels * 1000 / sampleRate
                                out.add(
                                    Window(
                                        timeMs = timeMs,
                                        rmsDb = (20 * log10(rms + 1e-9)).toFloat(),
                                        peakDb = (20 * log10(accPeak + 1e-9)).toFloat()
                                    )
                                )
                                samplesInWindow = 0
                                accSq = 0.0
                                accPeak = 0.0
                            }
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val f = codec.outputFormat
                        sampleRate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    else -> Unit
                }
            }
            out
        } catch (_: Exception) {
            null
        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (_: Exception) {
            }
            try {
                extractor.release()
            } catch (_: Exception) {
            }
        }
    }
}
