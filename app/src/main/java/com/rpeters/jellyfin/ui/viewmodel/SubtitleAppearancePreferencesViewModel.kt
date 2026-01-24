package com.rpeters.jellyfin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpeters.jellyfin.data.preferences.SubtitleAppearancePreferences
import com.rpeters.jellyfin.data.preferences.SubtitleAppearancePreferencesRepository
import com.rpeters.jellyfin.data.preferences.SubtitleBackground
import com.rpeters.jellyfin.data.preferences.SubtitleFont
import com.rpeters.jellyfin.data.preferences.SubtitleTextSize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubtitleAppearancePreferencesViewModel @Inject constructor(
    private val repository: SubtitleAppearancePreferencesRepository,
) : ViewModel() {

    private val _preferences = MutableStateFlow(SubtitleAppearancePreferences.DEFAULT)
    val preferences: StateFlow<SubtitleAppearancePreferences> = _preferences.asStateFlow()

    init {
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            repository.preferencesFlow.collectLatest { prefs ->
                _preferences.value = prefs
            }
        }
    }

    fun setTextSize(textSize: SubtitleTextSize) {
        viewModelScope.launch {
            repository.setTextSize(textSize)
        }
    }

    fun setFont(font: SubtitleFont) {
        viewModelScope.launch {
            repository.setFont(font)
        }
    }

    fun setBackground(background: SubtitleBackground) {
        viewModelScope.launch {
            repository.setBackground(background)
        }
    }
}
