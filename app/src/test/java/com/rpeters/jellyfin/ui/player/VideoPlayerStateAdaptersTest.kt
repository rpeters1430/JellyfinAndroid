package com.rpeters.jellyfin.ui.player

import com.rpeters.jellyfin.ui.player.components.toOverlayState
import io.mockk.mockk
import org.jellyfin.sdk.model.api.BaseItemDto
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPlayerStateAdaptersTest {

    @Test
    fun `toOverlayState shows primary loading ui only during initial load`() {
        val loading = VideoPlayerState(isLoading = true, currentPosition = 0L, isPlaying = false)
        val started = VideoPlayerState(isLoading = true, currentPosition = 1L, isPlaying = false)

        assertTrue(loading.toOverlayState().showPrimaryLoadingUi)
        assertFalse(started.toOverlayState().showPrimaryLoadingUi)
    }

    @Test
    fun `toOverlayState shows next episode prompt when outro starts`() {
        val state = VideoPlayerState(
            duration = 120_000L,
            currentPosition = 100_000L,
            outroStartMs = 95_000L,
            nextEpisode = mockk<BaseItemDto>(relaxed = true),
        )

        assertTrue(state.toOverlayState().showNextEpisodePrompt)
    }

    @Test
    fun `toOverlayState hides next episode prompt when dismissed`() {
        val state = VideoPlayerState(
            duration = 120_000L,
            currentPosition = 110_000L,
            nextEpisode = mockk<BaseItemDto>(relaxed = true),
            isNextEpisodePromptDismissed = true,
        )

        assertFalse(state.toOverlayState().showNextEpisodePrompt)
    }
}
