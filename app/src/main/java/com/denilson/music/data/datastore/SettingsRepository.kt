package com.denilson.music.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { LIGHT, DARK, SYSTEM }
enum class NormalizationMode { OFF, TRACK, ALBUM }

data class CrossfadeSettings(
    val enabled: Boolean = false,
    val durationMs: Long = 3000L,
    val entryPointMs: Long = 0L,   // punto de entrada de la siguiente canción
    val exitPointMs: Long = 3000L  // segundos antes del final donde inicia el fade out
)

data class SilenceSettings(
    val removeInitialSilence: Boolean = true,
    val thresholdDb: Float = -50f,   // umbral de silencio en dBFS
    val minSilenceMs: Long = 120L,   // tiempo mínimo de silencio
    val sensitivity: Float = 0.5f    // 0..1 (ajusta el umbral automáticamente)
)

data class EqSettings(
    val enabled: Boolean = false,
    val bass: Int = 0,               // 0..100
    val treble: Int = 0,             // 0..100
    val gains: List<Float> = List(7) { 0f } // 60,150,400,1k,2.4k,6k,15k Hz
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val crossfade: CrossfadeSettings = CrossfadeSettings(),
    val silence: SilenceSettings = SilenceSettings(),
    val normalization: NormalizationMode = NormalizationMode.OFF,
    val eq: EqSettings = EqSettings(),
    val adBlock: Boolean = true,
    val lastScan: Long = 0L,
    val customPresets: List<Pair<String, List<Float>>> = emptyList()
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val cfEnabled = booleanPreferencesKey("cf_enabled")
        val cfDuration = longPreferencesKey("cf_duration")
        val cfEntry = longPreferencesKey("cf_entry")
        val cfExit = longPreferencesKey("cf_exit")
        val silenceRemove = booleanPreferencesKey("silence_remove")
        val silenceThreshold = floatPreferencesKey("silence_threshold")
        val silenceMin = longPreferencesKey("silence_min")
        val silenceSensitivity = floatPreferencesKey("silence_sensitivity")
        val normalization = stringPreferencesKey("normalization_mode")
        val eqEnabled = booleanPreferencesKey("eq_enabled")
        val eqBass = intPreferencesKey("eq_bass")
        val eqTreble = intPreferencesKey("eq_treble")
        val eqGains = stringPreferencesKey("eq_gains")
        val adBlock = booleanPreferencesKey("ad_block")
        val lastScan = longPreferencesKey("last_scan")
        val customPresets = stringPreferencesKey("custom_presets")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.DARK,
            crossfade = CrossfadeSettings(
                enabled = p[Keys.cfEnabled] ?: false,
                durationMs = p[Keys.cfDuration] ?: 3000L,
                entryPointMs = p[Keys.cfEntry] ?: 0L,
                exitPointMs = p[Keys.cfExit] ?: 3000L
            ),
            silence = SilenceSettings(
                removeInitialSilence = p[Keys.silenceRemove] ?: true,
                thresholdDb = p[Keys.silenceThreshold] ?: -50f,
                minSilenceMs = p[Keys.silenceMin] ?: 120L,
                sensitivity = p[Keys.silenceSensitivity] ?: 0.5f
            ),
            normalization = p[Keys.normalization]
                ?.let { runCatching { NormalizationMode.valueOf(it) }.getOrNull() }
                ?: NormalizationMode.OFF,
            eq = EqSettings(
                enabled = p[Keys.eqEnabled] ?: false,
                bass = p[Keys.eqBass] ?: 0,
                treble = p[Keys.eqTreble] ?: 0,
                gains = parseGains(p[Keys.eqGains])
            ),
            adBlock = p[Keys.adBlock] ?: true,
            lastScan = p[Keys.lastScan] ?: 0L,
            customPresets = parsePresets(p[Keys.customPresets])
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.theme] = mode.name }

    suspend fun setCrossfade(s: CrossfadeSettings) = context.dataStore.edit {
        it[Keys.cfEnabled] = s.enabled
        it[Keys.cfDuration] = s.durationMs
        it[Keys.cfEntry] = s.entryPointMs
        it[Keys.cfExit] = s.exitPointMs
    }

    suspend fun setSilence(s: SilenceSettings) = context.dataStore.edit {
        it[Keys.silenceRemove] = s.removeInitialSilence
        it[Keys.silenceThreshold] = s.thresholdDb
        it[Keys.silenceMin] = s.minSilenceMs
        it[Keys.silenceSensitivity] = s.sensitivity
    }

    suspend fun setNormalization(mode: NormalizationMode) =
        context.dataStore.edit { it[Keys.normalization] = mode.name }

    suspend fun setEq(s: EqSettings) = context.dataStore.edit {
        it[Keys.eqEnabled] = s.enabled
        it[Keys.eqBass] = s.bass
        it[Keys.eqTreble] = s.treble
        it[Keys.eqGains] = s.gains.joinToString(",") { g -> (g * 10).toInt().toString() }
    }

    suspend fun setAdBlock(enabled: Boolean) = context.dataStore.edit { it[Keys.adBlock] = enabled }

    suspend fun setLastScan(time: Long) = context.dataStore.edit { it[Keys.lastScan] = time }

    suspend fun saveCustomPreset(name: String, gains: List<Float>) = context.dataStore.edit {
        val existing = parsePresets(it[Keys.customPresets]).filterNot { p -> p.first == name }
        val serialized = (existing + (name to gains)).joinToString("|") { p ->
            p.first + ";" + p.second.joinToString(",") { g -> (g * 10).toInt().toString() }
        }
        it[Keys.customPresets] = serialized
    }

    suspend fun deleteCustomPreset(name: String) = context.dataStore.edit {
        val existing = parsePresets(it[Keys.customPresets]).filterNot { p -> p.first == name }
        it[Keys.customPresets] = existing.joinToString("|") { p ->
            p.first + ";" + p.second.joinToString(",") { g -> (g * 10).toInt().toString() }
        }
    }

    private fun parseGains(raw: String?): List<Float> =
        raw?.split(",")?.mapNotNull { it.toFloatOrNull() }?.take(7)
            ?.let { it + List(7 - it.size) { 0f } } ?: List(7) { 0f }

    private fun parsePresets(raw: String?): List<Pair<String, List<Float>>> =
        raw?.split("|")?.mapNotNull { entry ->
            val parts = entry.split(";")
            if (parts.size == 2) {
                val gains = parts[1].split(",").mapNotNull { it.toFloatOrNull() }
                    .map { it / 10f }
                if (gains.size == 7) parts[0] to gains else null
            } else null
        } ?: emptyList()
}
