package com.rpeters.jellyfin.ui.player

import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Manages the UI state for the video player.
 */
@UnstableApi
class VideoPlayerStateManager @Inject constructor() {
    private val _playerState = MutableStateFlow(VideoPlayerState())
    val playerState: StateFlow<VideoPlayerState> = _playerState.asStateFlow()

    fun updateState(update: (VideoPlayerState) -> VideoPlayerState) {
        _playerState.update(update)
    }

    fun setState(state: VideoPlayerState) {
        _playerState.value = state
    }
}
