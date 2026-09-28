package com.denilson.music.web

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Descarga y mantiene actualizadas las listas comunitarias de bloqueo
 * (EasyList para publicidad, EasyPrivacy para rastreo y analitica).
 *
 * Estrategia:
 *  - Descarga en segundo plano, nunca bloquea la interfaz.
 *  - Guarda una copia en disco: sin Internet se sigue filtrando.
 *  - Ante cualquier fallo conserva la ultima lista valida.
 *  - La instancia activa es inmutable; se intercambia de forma atomica,
 *    asi la WebView nunca ve una lista a medio construir.
 */
object BlocklistLoader {

    private const val TAG = "BlocklistLoader"
    private const val EASYLIST_URL = "https://easylist.to/easylist/easylist.txt"
    private const val EASYPRIVACY_URL = "https://easylist.to/easylist/easyprivacy.txt"
    private const val MAX_BYTES = 12 * 1024 * 1024
    private const val TIMEOUT_MS = 20_000

    /** Instantanea atomica e inmutable de las reglas activas. */
    @Volatile
    private var active: Blocklist = Blocklist.EMPTY

    private val updating = AtomicBoolean(false)
    private val totalRequests = AtomicInteger(0)
    private var lastUpdateOk: Long = 0L
    private var lastUpdateAt: Long = 0L

    val current: Blocklist get() = active
    val requestsFiltered: Int get() = totalRequests.get()
    val ruleCount: Int get() = active.ruleCount
    val lastSuccessfulUpdate: Long get() = lastUpdateOk
    val lastAttemptAt: Long get() = lastUpdateAt

    private fun cacheDir(ctx: Context) = ctx.filesDir.resolve("blocklists").apply { mkdirs() }

    /**
     * Carga desde disco (rapido) y lanza una actualizacion en segundo
     * plano. Debe llamarse una vez al arrancar la app.
     */
    fun init(ctx: Context) {
        val cached = loadFromDisk(ctx)
        if (cached != null && !cached.isEmpty) {
            active = cached
            Log.i(TAG, "Listas cargadas desde disco: ${cached.ruleCount} reglas")
        }
        refreshAsync(ctx)
    }

    /** Descarga las listas sin bloquear el hilo actual. */
    fun refreshAsync(ctx: Context) {
        if (!updating.compareAndSet(false, true)) return
        Thread({
            try {
                val builder = StringBuilder(2 shl 20)
                var ok = 0
                for (url in listOf(EASYLIST_URL, EASYPRIVACY_URL)) {
                    val text = download(url) ?: continue
                    builder.append(text).append('\n')
                    ok++
                }
                if (ok > 0) {
                    val parsed = Blocklist.parse(builder.toString())
                    if (!parsed.isEmpty) {
                        active = parsed
                        lastUpdateOk = System.currentTimeMillis()
                        saveToDisk(ctx, builder.toString())
                        Log.i(TAG, "Listas actualizadas: ${parsed.ruleCount} reglas")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Fallo al actualizar listas: ${e.message}")
            } finally {
                lastUpdateAt = System.currentTimeMillis()
                updating.set(false)
            }
        }, "blocklist-update").apply { isDaemon = true }.start()
    }

    private fun download(url: String): String? = try {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = "GET"
            setRequestProperty("User-Agent", "DenilsonMusicPlayer/1.0")
            instanceFollowRedirects = true
        }
        try {
            if (conn.responseCode !in 200..299) {
                Log.w(TAG, "HTTP ${conn.responseCode} en $url")
                null
            } else {
                conn.inputStream.bufferedReader().use { readLimited(it) }
            }
        } finally {
            conn.disconnect()
        }
    } catch (e: Exception) {
        Log.w(TAG, "Error de red en $url: ${e.message}")
        null
    }

    private fun readLimited(reader: BufferedReader): String {
        val sb = StringBuilder(1 shl 20)
        val buf = CharArray(8192)
        var total = 0
        while (true) {
            val n = reader.read(buf)
            if (n < 0) break
            sb.append(buf, 0, n)
            total += n
            if (total > MAX_BYTES) break
        }
        return sb.toString()
    }

    private fun saveToDisk(ctx: Context, text: String) = try {
        cacheDir(ctx).resolve("easylist_combined.txt")
            .writeText(text)
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo guardar cache: ${e.message}")
    }

    private fun loadFromDisk(ctx: Context): Blocklist? = try {
        val f = cacheDir(ctx).resolve("easylist_combined.txt")
        if (f.exists() && f.length() > 0) {
            Blocklist.parse(f.readText())
        } else null
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo leer cache: ${e.message}")
        null
    }

    /** Registra una peticion bloqueada por el filtrado de red. */
    fun registerBlocked() {
        totalRequests.incrementAndGet()
    }
}
