package com.rpeters.jellyfin.ui.player.components

import androidx.compose.runtime.Stable
import com.rpeters.jellyfin.ui.player.VideoPlayerState

@Stable
data class VideoPlayerOverlayState(
    val showPrimaryLoadingUi: Boolean,
    val showNextEpisodePrompt: Boolean,
)

internal fun VideoPlayerState.toOverlayState(): VideoPlayerOverlayState {
    val showPrimaryLoadingUi = isLoading && currentPosition <= 0L && !isPlaying
    val remainingMs = duration - currentPosition
    val reachedOutro = outroStartMs?.let { currentPosition >= it } == true
    val inFallbackWindow = duration > 60_000 && remainingMs in 1..30_000

    return VideoPlayerOverlayState(
        showPrimaryLoadingUi = showPrimaryLoadingUi,
        showNextEpisodePrompt = nextEpisode != null &&
            !isNextEpisodePromptDismissed &&
            !showNextEpisodeCountdown &&
            (reachedOutro || inFallbackWindow),
    )
}
