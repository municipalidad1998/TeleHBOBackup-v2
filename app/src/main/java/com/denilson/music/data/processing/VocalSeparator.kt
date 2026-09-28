package com.denilson.music.data.processing

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.denilson.music.data.db.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SeparaciÃ³n de voz e instrumental 100% local mediante extracciÃ³n de canal central:
 *  - Instrumental (L+R)/2  -> atenÃºa la voz centrada
 *  - Voz (L-R)/2           -> aÃ­sla los componentes laterales diferenciales
 * No se sube nada a Internet y el archivo original no se modifica.
 */
@Singleton
class VocalSeparator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class Result(val success: Boolean, val percent: Int, val message: String)

    suspend fun separate(
        song: SongEntity,
        onProgress: (Int) -> Unit
    ): Result {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        val nameBase = song.title.ifBlank { File(song.dataPath).nameWithoutExtension }

        var instrumentalFile: File? = null
        var vocalFile: File? = null

        try {
            extractor.setDataSource(song.dataPath)
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
            if (audioTrack < 0 || format == null) {
                return Result(false, 0, "No se pudo decodificar el audio.")
            }
            extractor.selectTrack(audioTrack)

            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            if (channels < 2) {
                return Result(false, 0, "El audio no es estÃ©reo: no se puede separar la voz.")
            }

            codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec.configure(format, null, null, 0)
            codec.start()

            val cacheDir = context.cacheDir
            instrumentalFile = File(cacheDir, "sep_instrumental.pcm")
            vocalFile = File(cacheDir, "sep_vocal.pcm")
            val outInstrumental = RandomAccessFile(instrumentalFile, "rw")
            val outVocal = RandomAccessFile(vocalFile, "rw")

            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var totalFrames = 0L
            val estimatedFrames = song.durationMs * sampleRate / 1000

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
                        }
                        val bytes = ByteArray(info.size)
                        codec.getOutputBuffer(outIdx)!!.position(info.offset)
                        codec.getOutputBuffer(outIdx)!!.get(bytes, 0, info.size)

                        val frames = bytes.size / (channels * 2)
                        if (frames > 0) {
                            val instrumental = ShortArray(frames)
                            val vocal = ShortArray(frames)
                            val step = channels * 2
                            for (f in 0 until frames) {
                                val base = f * step
                                val li = (((bytes[base].toInt() and 0xFF) or (bytes[base + 1].toInt() shl 8)).toShort()) / 32768.0
                                val ri = (((bytes[base + 2].toInt() and 0xFF) or (bytes[base + 3].toInt() shl 8)).toShort()) / 32768.0
                                val inst = ((li + ri) / 2.0).coerceIn(-1.0, 1.0)
                                val voc = ((li - ri) / 2.0).coerceIn(-1.0, 1.0)
                                instrumental[f] = (inst * 32767).toInt().toShort()
                                vocal[f] = (voc * 32767).toInt().toShort()
                            }
                            writePcm(outInstrumental, instrumental)
                            writePcm(outVocal, vocal)
                            totalFrames += frames
                            if (estimatedFrames > 0) {
                                val percent = (totalFrames * 100 / estimatedFrames).toInt().coerceIn(0, 99)
                                onProgress(percent)
                            }
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        sampleRate = codec.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    else -> Unit
                }
            }

            outInstrumental.close()
            outVocal.close()

            val uriInstrumental = saveAsWav(instrumentalFile, "$nameBase - Instrumental", sampleRate)
            val uriVocal = saveAsWav(vocalFile, "$nameBase - Voz", sampleRate)

            instrumentalFile.delete()
            vocalFile.delete()

            onProgress(100)
            val ok = uriInstrumental != null && uriVocal != null
            return Result(
                success = ok,
                percent = 100,
                message = if (ok) "Archivos guardados en MÃºsica/Denilson Music" else "No se pudieron guardar los archivos."
            )
        } catch (e: Exception) {
            return Result(false, 0, "Error: ${e.message ?: "desconocido"}")
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

    private fun writePcm(file: RandomAccessFile, samples: ShortArray) {
        val bytes = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            val v = samples[i].toInt()
            bytes[i * 2] = (v and 0xFF).toByte()
            bytes[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }
        file.write(bytes)
    }

    private fun saveAsWav(pcm: File, name: String, sampleRate: Int): Uri? {
        val dataSize = pcm.length().toInt()
        val channels = 1
        val sampleBytes = 2
        val blockAlign = channels * sampleBytes
        val byteRate = sampleRate * blockAlign
        val header = ByteArray(44)
        fun putLe(offset: Int, value: Int, bytes: Int) {
            for (i in 0 until bytes) header[offset + i] = ((value shr (i * 8)) and 0xFF).toByte()
        }
        "RIFF".toByteArray().copyInto(header, 0)
        putLe(4, 36 + dataSize, 4)
        "WAVE".toByteArray().copyInto(header, 8)
        "fmt ".toByteArray().copyInto(header, 12)
        putLe(16, 16, 4)           // tamaÃ±o fmt
        putLe(20, 1, 2)            // PCM
        putLe(22, channels, 2)
        putLe(24, sampleRate, 4)
        putLe(28, byteRate, 4)
        putLe(32, blockAlign, 2)
        putLe(34, 16, 2)           // bits por muestra
        "data".toByteArray().copyInto(header, 36)
        putLe(40, dataSize, 4)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, "$name.wav")
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Denilson Music")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = resolver.insert(collection, values) ?: return null
            val ok = try {
                resolver.openOutputStream(itemUri)?.use { out ->
                    out.write(header)
                    pcm.inputStream().use { it.copyTo(out) }
                } != null
            } catch (_: Exception) {
                false
            }
            if (!ok) return null
            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
            itemUri
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                "Denilson Music"
            )
            dir.mkdirs()
            val outFile = File(dir, "$name.wav")
            outFile.outputStream().use { out ->
                out.write(header)
                pcm.inputStream().use { it.copyTo(out) }
            }
            MediaScannerConnection.scanFile(context, arrayOf(outFile.absolutePath), arrayOf("audio/wav"), null)
            null
        }
    }
}
