package com.denilson.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denilson.music.data.datastore.EqSettings
import com.denilson.music.ui.theme.MaxMagenta
import com.denilson.music.ui.theme.MaxViolet
import com.denilson.music.ui.theme.maxGradient
import com.denilson.music.ui.viewmodels.MainViewModel

/** Frecuencias centrales, en el mismo orden que usa AudioFx. */
private val BANDS = listOf("60 Hz", "150 Hz", "400 Hz", "1 kHz", "2.4 kHz", "6 kHz", "15 kHz")

/**
 * Presets de fabrica. Los valores van de -1.0 a +1.0 y AudioFx los
 * convierte a milibelios (valor * 100), es decir +/- 10 dB por banda.
 */
private val PRESETS: List<Pair<String, List<Float>>> = listOf(
    "Normal" to listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f),
    "Rock" to listOf(0.6f, 0.45f, 0.2f, -0.1f, 0.15f, 0.4f, 0.55f),
    "Pop" to listOf(-0.15f, 0.1f, 0.35f, 0.45f, 0.3f, 0.15f, -0.05f),
    "Jazz" to listOf(0.4f, 0.3f, 0.1f, 0.15f, 0.05f, 0.2f, 0.3f),
    "Clasica" to listOf(0.2f, 0.15f, 0.05f, 0f, 0f, 0.15f, 0.35f),
    "Vocal" to listOf(-0.3f, -0.1f, 0.3f, 0.55f, 0.5f, 0.25f, -0.1f),
    "Electronica" to listOf(0.7f, 0.5f, 0.1f, -0.05f, 0.2f, 0.45f, 0.6f),
    "Bass Boost" to listOf(1f, 0.8f, 0.4f, 0f, -0.1f, -0.2f, -0.3f)
)

@Composable
fun EqualizerScreen(vm: MainViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val eq = settings.eq
    var saveDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // ---------- Cabecera ----------
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
                    text = "Ecualizador",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "7 bandas · procesado en el dispositivo",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = eq.enabled,
                onCheckedChange = { vm.setEq(eq.copy(enabled = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaxViolet
                )
            )
        }

        // ---------- Bandas ----------
        Column(Modifier.padding(horizontal = 20.dp)) {
            BANDS.forEachIndexed { i, label ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.width(58.dp)
                    )
                    Slider(
                        value = eq.gains.getOrElse(i) { 0f },
                        onValueChange = { v ->
                            val gains = eq.gains.toMutableList()
                            while (gains.size < 7) gains.add(0f)
                            gains[i] = v
                            vm.setEq(eq.copy(gains = gains))
                        },
                        valueRange = -1f..1f,
                        enabled = eq.enabled,
                        colors = SliderDefaults.colors(
                            thumbColor = MaxMagenta,
                            activeTrackColor = MaxViolet,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = gainLabel(eq.gains.getOrElse(i) { 0f }),
                        color = if (eq.enabled) Color.White
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.width(46.dp)
                    )
                }
            }
        }

        // ---------- Bass / Treble ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MiniControl(
                label = "Bass",
                value = eq.bass,
                enabled = eq.enabled,
                modifier = Modifier.weight(1f)
            ) { vm.setEq(eq.copy(bass = it)) }
            MiniControl(
                label = "Treble",
                value = eq.treble,
                enabled = eq.enabled,
                modifier = Modifier.weight(1f)
            ) { vm.setEq(eq.copy(treble = it)) }
        }

        // ---------- Presets ----------
        Text(
            text = "Presets",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp)
        )
        PresetGrid(PRESETS) { gains ->
            vm.setEq(eq.copy(enabled = true, gains = gains))
        }

        if (settings.customPresets.isNotEmpty()) {
            Text(
                text = "Mis presets",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
            )
            PresetGrid(settings.customPresets) { gains ->
                vm.setEq(eq.copy(enabled = true, gains = gains))
            }
        }



        // ---------- Acciones ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionButton(Icons.Rounded.Restore, "Restablecer", Modifier.weight(1f)) {
                vm.setEq(EqSettings(enabled = eq.enabled))
            }
            ActionButton(Icons.Rounded.Save, "Guardar", Modifier.weight(1f)) {
                presetName = ""
                saveDialog = true
            }
        }
    }

    if (saveDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { saveDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Guardar preset", color = Color.White) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    placeholder = { Text("Nombre del preset") },
                    singleLine = true
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (presetName.isNotBlank()) {
                            vm.saveCustomPreset(presetName.trim(), eq.gains)
                        }
                        saveDialog = false
                    }
                ) { Text("Guardar", color = Color.White) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { saveDialog = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

private fun gainLabel(v: Float): String {
    val db = v * 10f
    return if (db > 0.05f) "+%.1f dB".format(db)
    else if (db < -0.05f) "%.1f dB".format(db)
    else "0 dB"
}

@Composable
private fun PresetGrid(
    items: List<Pair<String, List<Float>>>,
    onPick: (List<Float>) -> Unit
) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        items.chunked(3).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (name, gains) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onPick(gains) }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            color = Color.White,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MiniControl(
    label: String,
    value: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit
) {
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text("$value", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..100f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = MaxMagenta,
                activeTrackColor = MaxViolet,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
