package com.rpeters.jellyfin.ui.viewmodel

import com.rpeters.jellyfin.ui.utils.EnhancedPlaybackUtils
import com.rpeters.jellyfin.ui.utils.PlaybackCapabilityAnalysis
import io.mockk.coEvery
import io.mockk.eq
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.jellyfin.sdk.model.api.BaseItemDto
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class TVEpisodeDetailViewModelTest {
    private val enhancedPlaybackUtils: EnhancedPlaybackUtils = mockk()
    private val dispatcher = StandardTestDispatcher()
    private val viewModel by lazy { TVEpisodeDetailViewModel(enhancedPlaybackUtils) }

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadEpisodeAnalysis stores analysis result`() = runTest {
        val episode = BaseItemDto(id = UUID.randomUUID())
        val analysis = PlaybackCapabilityAnalysis(
            canPlay = true,
            preferredMethod = com.rpeters.jellyfin.ui.utils.PlaybackMethod.DIRECT_PLAY,
            expectedQuality = "1080p",
            details = "Direct Play",
            codecs = "H264/AAC",
            container = "mp4",
            estimatedBandwidth = 10_000_000,
        )
        coEvery { enhancedPlaybackUtils.analyzePlaybackCapabilities(episode) } returns analysis

        viewModel.loadEpisodeAnalysis(episode)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(analysis, viewModel.state.value.playbackAnalysis)
    }

    @Test
    fun `loadEpisodeAnalysis logs errors and recovers`() = runTest {
        val episode = BaseItemDto(id = UUID.randomUUID())
        val slotException = slot<Exception>()
        every {
            android.util.Log.e(
                eq("TVEpisodeDetailVM"),
                any(),
                capture(slotException),
            )
        } returns 0
        coEvery { enhancedPlaybackUtils.analyzePlaybackCapabilities(episode) } throws IllegalStateException("boom")

        viewModel.loadEpisodeAnalysis(episode)
        dispatcher.scheduler.advanceUntilIdle()

        verify { android.util.Log.e(eq("TVEpisodeDetailVM"), any(), any()) }
        assertNull(viewModel.state.value.playbackAnalysis)
        assertEquals("boom", slotException.captured.message)
    }
}
