package com.rpeters.jellyfin.ui.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import com.rpeters.jellyfin.data.playback.AdaptiveBitrateMonitor
import com.rpeters.jellyfin.data.repository.IJellyfinRepository
import com.rpeters.jellyfin.ui.player.cast.CastState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Test for VideoPlayerViewModel initialization race condition fix.
 */
@OptIn(ExperimentalCoroutinesApi::class, UnstableApi::class)
class VideoPlayerViewModelInitTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockContext: Context
    private lateinit var mockRepository: IJellyfinRepository
    private lateinit var mockStateManager: VideoPlayerStateManager
    private lateinit var mockPlaybackManager: VideoPlayerPlaybackManager
    private lateinit var mockTrackManager: VideoPlayerTrackManager
    private lateinit var mockVideoPlayerCastManager: VideoPlayerCastManager
    private lateinit var mockMetadataManager: VideoPlayerMetadataManager
    private lateinit var mockPlaybackProgressManager: PlaybackProgressManager
    private lateinit var mockPlaybackPreferencesRepository: com.rpeters.jellyfin.data.preferences.PlaybackPreferencesRepository
    private lateinit var mockAdaptiveBitrateMonitor: AdaptiveBitrateMonitor

    private val playerStateFlow = MutableStateFlow(VideoPlayerState())
    private val playbackProgressFlow = MutableStateFlow(PlaybackProgress())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockContext = mockk(relaxed = true)
        mockRepository = mockk(relaxed = true)
        mockStateManager = mockk(relaxed = true)
        mockPlaybackManager = mockk(relaxed = true)
        mockTrackManager = mockk(relaxed = true)
        mockVideoPlayerCastManager = mockk(relaxed = true)
        mockMetadataManager = mockk(relaxed = true)
        mockPlaybackProgressManager = mockk(relaxed = true)
        mockPlaybackPreferencesRepository = mockk(relaxed = true)
        mockAdaptiveBitrateMonitor = mockk(relaxed = true)

        every { mockStateManager.playerState } returns playerStateFlow
        every { mockPlaybackProgressManager.playbackProgress } returns playbackProgressFlow
        every { mockAdaptiveBitrateMonitor.qualityRecommendation } returns MutableStateFlow(null)
        every { mockPlaybackPreferencesRepository.preferences } returns MutableStateFlow(com.rpeters.jellyfin.data.preferences.PlaybackPreferences.DEFAULT)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `viewModel initializes without NullPointerException`() = runTest(testDispatcher) {
        // When
        val viewModel = VideoPlayerViewModel(
            context = mockContext,
            repository = mockRepository,
            stateManager = mockStateManager,
            playbackManager = mockPlaybackManager,
            trackManager = mockTrackManager,
            castManager = mockVideoPlayerCastManager,
            metadataManager = mockMetadataManager,
            playbackProgressManager = mockPlaybackProgressManager,
            playbackPreferencesRepository = mockPlaybackPreferencesRepository,
            adaptiveBitrateMonitor = mockAdaptiveBitrateMonitor,
        )

        advanceUntilIdle()

        // Then
        assertNotNull(viewModel.playerState)
    }

    @Test
    fun `viewModel reflect initial state`() = runTest(testDispatcher) {
        // When
        val viewModel = VideoPlayerViewModel(
            context = mockContext,
            repository = mockRepository,
            stateManager = mockStateManager,
            playbackManager = mockPlaybackManager,
            trackManager = mockTrackManager,
            castManager = mockVideoPlayerCastManager,
            metadataManager = mockMetadataManager,
            playbackProgressManager = mockPlaybackProgressManager,
            playbackPreferencesRepository = mockPlaybackPreferencesRepository,
            adaptiveBitrateMonitor = mockAdaptiveBitrateMonitor,
        )

        advanceUntilIdle()

        // Then
        assertEquals(playerStateFlow.value, viewModel.playerState.value)
    }

    @Test
    fun `viewModel handles state updates`() = runTest(testDispatcher) {
        // Given
        val viewModel = VideoPlayerViewModel(
            context = mockContext,
            repository = mockRepository,
            stateManager = mockStateManager,
            playbackManager = mockPlaybackManager,
            trackManager = mockTrackManager,
            castManager = mockVideoPlayerCastManager,
            metadataManager = mockMetadataManager,
            playbackProgressManager = mockPlaybackProgressManager,
            playbackPreferencesRepository = mockPlaybackPreferencesRepository,
            adaptiveBitrateMonitor = mockAdaptiveBitrateMonitor,
        )

        advanceUntilIdle()

        // When
        val newState = VideoPlayerState(itemId = "test-item", isPlaying = true)
        playerStateFlow.value = newState
        advanceUntilIdle()

        // Then
        assertEquals(newState, viewModel.playerState.value)
    }
}
