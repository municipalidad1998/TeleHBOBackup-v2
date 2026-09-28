package com.denilson.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denilson.music.ui.navigation.AppRoot
import com.denilson.music.ui.theme.DenilsonTheme
import com.denilson.music.ui.viewmodels.MainViewModel
import com.denilson.music.web.AdBlocker
import com.denilson.music.web.BlocklistLoader
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* El escaneo se dispara desde la UI tras conceder acceso */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Bloqueador de anuncios disponible desde el arranque.
        // Carga las listas comunitarias en segundo plano.
        AdBlocker.init(applicationContext)
        BlocklistLoader.init(applicationContext)

        ensureAudioPermission()

        setContent {
            val vm: MainViewModel = hiltViewModel()
            val themeMode by vm.themeMode.collectAsStateWithLifecycle()
            DenilsonTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(onPermissionResult = { ensureAudioPermission() })
                }
            }
        }
    }

    /**
     * Solicita el permiso de lectura de audio con el nombre moderno
     * (READ_MEDIA_AUDIO en Android 13+). Solo se pide lo necesario.
     */
    fun ensureAudioPermission() {
        val needed = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            requestPermission.launch(needed.toTypedArray())
        }
    }
}
