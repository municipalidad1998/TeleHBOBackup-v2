package com.denilson.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denilson.music.data.datastore.AppSettings
import com.denilson.music.data.datastore.SettingsRepository
import com.denilson.music.data.datastore.ThemeMode
import com.denilson.music.media.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    val player: PlayerConnection
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val themeMode: StateFlow<ThemeMode> = settingsRepo.settings
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.DARK)

    val adBlockEnabled: StateFlow<Boolean> = settingsRepo.settings
        .map { it.adBlock }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepo.setThemeMode(mode) }

    fun setAdBlock(enabled: Boolean) = viewModelScope.launch { settingsRepo.setAdBlock(enabled) }

    fun setEq(eq: com.denilson.music.data.datastore.EqSettings) =
        viewModelScope.launch { settingsRepo.setEq(eq) }

    fun saveCustomPreset(name: String, gains: List<Float>) =
        viewModelScope.launch { settingsRepo.saveCustomPreset(name, gains) }

    fun deleteCustomPreset(name: String) =
        viewModelScope.launch { settingsRepo.deleteCustomPreset(name) }
}
