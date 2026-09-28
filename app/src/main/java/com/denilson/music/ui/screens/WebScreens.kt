package com.denilson.music.ui.screens

import android.content.Context
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.denilson.music.ui.theme.BlockGreen
import com.denilson.music.ui.theme.maxGradient
import com.denilson.music.web.AdBlocker
import com.denilson.music.web.AdBlockWebViewClient
import com.denilson.music.web.BlocklistLoader
import com.denilson.music.web.configureWebViewForInApp

private val IMMERSIVE_FLAGS =
    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
    View.SYSTEM_UI_FLAG_FULLSCREEN or
    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

@Composable
private fun InAppWebPlayer(
    startUrl: String,
    title: String,
    icon: ImageVector,
    onBack: () -> Unit
) {
    var progress by remember { mutableIntStateOf(0) }
    var blocked by remember { mutableIntStateOf(AdBlocker.sessionBlocked) }
    var currentUrl by remember { mutableStateOf(startUrl) }

    var customView by remember { mutableStateOf<View?>(null) }
    var customCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var immersive by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var chromeVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val webViewRef = remember { arrayOfNulls<WebView>(1) }
    val activity = remember(context) { context.findActivity() }

    LaunchedEffect(Unit) { AdBlocker.init(context.applicationContext) }

    fun toggleSystemBars(visible: Boolean) {
        val act = activity ?: return
        if (visible) {
            act.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            act.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            act.window.decorView.systemUiVisibility = IMMERSIVE_FLAGS
            act.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        immersive = !visible
    }

    fun goBack() {
        if (customView != null) {
            (webViewRef[0]?.webChromeClient as? WebChromeClient)?.onHideCustomView()
            customView = null
            customCallback = null
            return
        }
        val wv = webViewRef[0]
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            if (immersive) {
                toggleSystemBars(true)
            }
            onBack()
        }
    }

    fun goHome() {
        (webViewRef[0]?.webChromeClient as? WebChromeClient)?.onHideCustomView()
        customView = null
        customCallback = null
        if (immersive) toggleSystemBars(true)
        val wv = webViewRef[0]
        currentUrl = startUrl
        wv?.loadUrl(startUrl)
    }

    fun onSiteFullscreen(view: View?, callback: WebChromeClient.CustomViewCallback?) {
        if (view != null) {
            customView = view
            customCallback = callback
            activity?.window?.decorView?.systemUiVisibility = IMMERSIVE_FLAGS
        } else {
            customView = null
            customCallback = null
        }
    }

    customView?.let { cv ->
        BackHandler(enabled = true) { goBack() }
        AndroidView(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            factory = { cv }
        )
        return
    }

    BackHandler(enabled = true) { goBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .navigationBarsPadding()
    ) {
        if (chromeVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopBarIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", ::goBack)
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(maxGradient()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "YouTube Oficial sin anuncios",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0E3A2A))
                        .clickable { showStats = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Shield,
                        contentDescription = "Bloqueador",
                        tint = BlockGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$blocked",
                        color = BlockGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(6.dp))
                TopBarIcon(Icons.Rounded.Refresh, "Recargar") {
                    webViewRef[0]?.loadUrl(currentUrl)
                }
                Spacer(Modifier.width(6.dp))
                TopBarIcon(Icons.Rounded.Fullscreen, "Pantalla completa") {
                    toggleSystemBars(!immersive)
                }
                Spacer(Modifier.width(6.dp))
                TopBarIcon(Icons.Rounded.Home, "Inicio", ::goHome)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0x55222222))
                    .clickable { chromeVisible = !chromeVisible },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (chromeVisible) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (chromeVisible) "Ocultar controles" else "Mostrar controles",
                    tint = Color(0xCCFFFFFF),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (progress in 1..99) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0xFF222222))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((progress / 100f).coerceIn(0f, 1f))
                        .height(2.dp)
                        .background(maxGradient())
                )
            }
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewRef[0] = this
                    configureWebViewForInApp(
                        webView = this,
                        onProgress = { progress = it },
                        onFullscreenChange = { view, cb -> onSiteFullscreen(view, cb) }
                    )
                    webViewClient = AdBlockWebViewClient { blocked = it }
                    loadUrl(startUrl)
                }
            },
            onRelease = { wv ->
                webViewRef[0] = null
                wv.stopLoading()
                wv.destroy()
            }
        )
    }

    if (showStats) {
        AlertDialog(
            onDismissRequest = { showStats = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = BlockGreen, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Bloqueador de anuncios", color = Color.White)
                }
            },
            text = {
                Column {
                    StatsRow("Esta sesión", "$blocked")
                    StatsRow("Total acumulado", "${AdBlocker.totalBlocked}")
                    StatsRow("Listas", listLabel())
                    StatsRow("Reglas cargadas", "${BlocklistLoader.ruleCount}")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Filtra banners, pop-ups y acelera los anuncios de video.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showStats = false }) {
                    Text("Cerrar", color = Color.White)
                }
            }
        )
    }
}

private fun listLabel(): String = when {
    BlocklistLoader.ruleCount <= 0 -> "Lista embebida"
    BlocklistLoader.lastSuccessfulUpdate > 0L -> "EasyList + EasyPrivacy"
    else -> "EasyList + EasyPrivacy (caché)"
}

@Composable
private fun StatsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private tailrec fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun TopBarIcon(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun YoutubeScreen(onBack: () -> Unit) {
    InAppWebPlayer(
        startUrl = "https://m.youtube.com/",
        title = "YouTube",
        icon = Icons.Rounded.SmartDisplay,
        onBack = onBack
    )
}

@Composable
fun YoutubeMusicScreen(onBack: () -> Unit) {
    InAppWebPlayer(
        startUrl = "https://music.youtube.com/",
        title = "YouTube Music",
        icon = Icons.Rounded.MusicNote,
        onBack = onBack
    )
}
