package com.rpeters.jellyfin.ui.player

import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Manages the UI state for the video player.
 *
 * Must be @ViewModelScoped so that VideoPlayerViewModel and all managers it owns
 * (VideoPlayerPlaybackManager, VideoPlayerTrackManager, etc.) share the exact same
 * instance.  Without the scope, Hilt creates a separate instance per injection site,
 * meaning position/duration updates written by the playback manager are invisible to
 * the ViewModel's exposed StateFlow.
 */
@ViewModelScoped
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
