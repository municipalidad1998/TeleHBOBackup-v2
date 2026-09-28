package com.denilson.music.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denilson.music.data.datastore.ThemeMode
import com.denilson.music.data.updater.AppUpdater
import com.denilson.music.data.updater.UpdateInfo
import com.denilson.music.ui.theme.BlockGreen
import com.denilson.music.ui.theme.MaxCyan
import com.denilson.music.ui.theme.MaxMagenta
import com.denilson.music.ui.theme.MaxViolet
import com.denilson.music.ui.theme.maxGradient
import com.denilson.music.ui.viewmodels.LibraryViewModel
import com.denilson.music.ui.viewmodels.MainViewModel
import com.denilson.music.web.AdBlocker
import com.denilson.music.web.BlocklistLoader
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(mainVm: MainViewModel, libVm: LibraryViewModel, onOpenEqualizer: () -> Unit = {}) {
    val settings by mainVm.settings.collectAsStateWithLifecycle()
    val eqEnabled = settings.eq.enabled
    val themeMode by mainVm.themeMode.collectAsStateWithLifecycle()
    val songs by libVm.songs.collectAsStateWithLifecycle()
    val progress by libVm.analyzeProgress.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var checkingUpdate by remember { mutableStateOf(false) }
    var downloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var updateResult by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateStatusText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Encabezado
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(maxGradient()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = "Ajustes",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Personaliza tu experiencia",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }

        // ---------- Actualizaciones de la APK ----------
        SettingsCard(
            icon = Icons.Rounded.SystemUpdate,
            iconBrush = Brush.linearGradient(listOf(MaxCyan, Color(0xFF0077FE))),
            title = "Actualización de la App",
            subtitle = "Busca versiones nuevas y las instala encima conservando todos tus datos"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(maxGradient())
                        .clickable(enabled = !checkingUpdate && !downloadingUpdate) {
                            checkingUpdate = true
                            updateStatusText = "Buscando actualizaciones..."
                            scope.launch {
                                val info = AppUpdater.checkForUpdates(context)
                                checkingUpdate = false
                                updateResult = info
                                if (info.hasUpdate) {
                                    updateStatusText = "Nueva versión disponible: v${info.latestVersion}"
                                } else {
                                    updateStatusText = "Ya tienes la última versión instalada (v${info.currentVersion})"
                                    Toast.makeText(context, "Tienes la versión más reciente", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (checkingUpdate) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Comprobando...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Buscar actualización",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                updateStatusText?.let { msg ->
                    Text(
                        text = msg,
                        color = if (updateResult?.hasUpdate == true) BlockGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                if (updateResult?.hasUpdate == true && !downloadingUpdate) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0E3A2A))
                            .clickable {
                                downloadingUpdate = true
                                scope.launch {
                                    val apk = AppUpdater.downloadApk(context, updateResult!!.downloadUrl) { pct ->
                                        downloadProgress = pct
                                    }
                                    downloadingUpdate = false
                                    if (apk != null) {
                                        updateStatusText = "Descarga completada. Abriendo instalador..."
                                        AppUpdater.installApk(context, apk)
                                    } else {
                                        updateStatusText = "Error al descargar el archivo."
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Instalar actualización v${updateResult!!.latestVersion}",
                            color = BlockGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                if (downloadingUpdate) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Descargando...", color = Color.White, fontSize = 12.sp)
                            Text("$downloadProgress%", color = MaxCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaxCyan,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // ---------- Ecualizador ----------
        SettingsCard(
            icon = Icons.Rounded.GraphicEq,
            iconBrush = Brush.linearGradient(listOf(MaxMagenta, MaxViolet)),
            title = "Ecualizador",
            subtitle = "7 bandas (60 Hz - 15 kHz), presets y Bass/Treble"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(maxGradient())
                    .clickable { onOpenEqualizer() }
                    .padding(horizontal = 20.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (eqEnabled) "Abrir ecualizador · activo"
                    else "Abrir ecualizador",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ---------- Bloqueador de anuncios ----------
        SettingsCard(
            icon = Icons.Rounded.Block,
            iconBrush = Brush.linearGradient(listOf(BlockGreen, MaxCyan)),
            title = "Bloqueador de anuncios",
            subtitle = "Activo en YouTube y YouTube Music: filtra publicidad sin retrasar la carga"
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = BlockGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Siempre activado",
                        color = BlockGreen,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${AdBlocker.totalBlocked} bloqueados",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                StatLine("Listas activas", listLabel())
                StatLine("Reglas cargadas", "${BlocklistLoader.ruleCount}")
                StatLine(
                    "Última actualización",
                    if (BlocklistLoader.lastSuccessfulUpdate > 0L) {
                        java.text.SimpleDateFormat("dd/MM HH:mm")
                            .format(java.util.Date(BlocklistLoader.lastSuccessfulUpdate))
                    } else "Pendiente"
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "EasyList + EasyPrivacy. Reproducción de medios asegurada al 100%.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

        // ---------- Navegadores integrados ----------
        SettingsCard(
            icon = Icons.Rounded.SmartDisplay,
            iconBrush = Brush.linearGradient(listOf(MaxViolet, MaxMagenta)),
            title = "Navegadores integrados",
            subtitle = "YouTube y YouTube Music con interfaz nativa integrada"
        ) {
            Text(
                text = "Reproducción continua, soporte completo de botón Atrás y sin anuncios intrusivos.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        // ---------- Tema ----------
        SettingsCard(
            icon = Icons.Rounded.DarkMode,
            iconBrush = maxGradient(),
            title = "Tema",
            subtitle = "Oscuro, claro o según el sistema"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeChip("Oscuro", themeMode == ThemeMode.DARK) {
                    mainVm.setThemeMode(ThemeMode.DARK)
                }
                ThemeChip("Claro", themeMode == ThemeMode.LIGHT) {
                    mainVm.setThemeMode(ThemeMode.LIGHT)
                }
                ThemeChip("Sistema", themeMode == ThemeMode.SYSTEM) {
                    mainVm.setThemeMode(ThemeMode.SYSTEM)
                }
            }
        }

        // ---------- Biblioteca ----------
        SettingsCard(
            icon = Icons.Rounded.LibraryMusic,
            iconBrush = Brush.linearGradient(listOf(MaxCyan, MaxViolet)),
            title = "Biblioteca",
            subtitle = "${songs.size} canciones · análisis ${progress.processed}/${progress.total}"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(maxGradient())
                    .clickable(enabled = !libVm.scanning) { libVm.scan() }
                    .padding(horizontal = 20.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (libVm.scanning) "Escaneando…" else "Escanear biblioteca",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            libVm.lastScanMessage?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun listLabel(): String = when {
    BlocklistLoader.ruleCount <= 0 -> "Solo lista embebida"
    BlocklistLoader.lastSuccessfulUpdate > 0L -> "EasyList + EasyPrivacy"
    else -> "EasyList + EasyPrivacy (caché)"
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    iconBrush: Brush,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .then(
                if (selected) Modifier.background(maxGradient())
                else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Icon(
                Icons.Rounded.LightMode,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}
