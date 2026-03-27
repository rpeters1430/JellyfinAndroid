package com.rpeters.jellyfin.ui.player

import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.rpeters.jellyfin.data.repository.JellyfinRepository
import com.rpeters.jellyfin.ui.player.cast.CastState
import com.rpeters.jellyfin.utils.AnalyticsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import javax.inject.Inject

/**
 * Handles Google Cast and DLNA integration for the video player.
 */
@UnstableApi
class VideoPlayerCastManager @Inject constructor(
    private val castManager: CastManager,
    private val stateManager: VideoPlayerStateManager,
    private val repository: JellyfinRepository,
    private val analytics: AnalyticsHelper
) {
    private var castPositionJob: Job? = null
    private var hasSentCastLoad = false

    fun initialize(
        scope: CoroutineScope,
        onStartPlayback: (itemId: String, itemName: String, position: Long) -> Unit,
        onReleasePlayer: () -> Unit,
        onStartCasting: (position: Long) -> Unit,
    ) {
        castManager.initialize()
        scope.launch {
            castManager.castState.collect { castState ->
                handleCastState(castState, onStartPlayback, onReleasePlayer, onStartCasting, scope)
            }
        }
    }

    private fun handleCastState(
        castState: CastState,
        onStartPlayback: (itemId: String, itemName: String, position: Long) -> Unit,
        onReleasePlayer: () -> Unit,
        onStartCasting: (position: Long) -> Unit,
        scope: CoroutineScope
    ) {
        val currentState = stateManager.playerState.value
        val isConnecting = castState.isConnected && !stateManager.playerState.value.isCastConnected

        stateManager.updateState { it.copy(
            isCastAvailable = castState.isAvailable,
            isCasting = castState.isCasting,
            isCastConnected = castState.isConnected,
            castDeviceName = castState.deviceName,
            isCastPlaying = castState.isRemotePlaying,
            castPosition = castState.currentPosition,
            castDuration = castState.duration,
            castVolume = castState.volume,
            availableCastDevices = castState.availableDevices,
            castDiscoveryState = castState.discoveryState,
            showCastDialog = if (castState.isConnected) false else it.showCastDialog,
            error = castState.error ?: it.error
        ) }

        if (isConnecting && !hasSentCastLoad) {
            val startPosition = currentState.currentPosition
            onReleasePlayer()
            onStartCasting(startPosition)
        }

        if (currentState.isCastConnected && !castState.isConnected) {
            hasSentCastLoad = false
            val resumePosition = castState.currentPosition
            val itemId = currentState.itemId
            val itemName = currentState.itemName
            if (itemId.isNotEmpty()) {
                onStartPlayback(itemId, itemName, resumePosition)
            }
        }

        if (castState.isCasting) {
            startCastPositionUpdates(scope)
        } else {
            stopCastPositionUpdates()
        }
    }

    private fun startCastPositionUpdates(scope: CoroutineScope) {
        if (castPositionJob?.isActive == true) return
        castPositionJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                castManager.updatePlaybackState()
                delay(1000)
            }
        }
    }

    private fun stopCastPositionUpdates() {
        castPositionJob?.cancel()
        castPositionJob = null
    }

    fun startCasting(
        mediaItem: MediaItem,
        item: BaseItemDto,
        sideLoadedSubs: List<SubtitleSpec>,
        startPositionMs: Long,
        playSessionId: String?,
        mediaSourceId: String?
    ) {
        analytics.logCastEvent(stateManager.playerState.value.castDeviceName ?: "Unknown Device")
        castManager.startCasting(
            mediaItem = mediaItem,
            item = item,
            sideLoadedSubs = sideLoadedSubs,
            startPositionMs = startPositionMs,
            playSessionId = playSessionId,
            mediaSourceId = mediaSourceId
        )
        hasSentCastLoad = true
    }

    fun stopDiscovery() = castManager.stopDiscovery()
    fun startDiscovery() = castManager.startDiscovery()
    suspend fun awaitInitialization() = castManager.awaitInitialization()
    fun connectToDevice(deviceName: String) = castManager.connectToDevice(deviceName)
    fun disconnectCastSession() = castManager.disconnectCastSession()
    fun pauseCasting() = castManager.pauseCasting()
    fun resumeCasting() = castManager.resumeCasting()
    fun stopCasting() {
        castManager.stopCasting()
        hasSentCastLoad = false
        stopCastPositionUpdates()
    }
    fun seekTo(positionMs: Long) = castManager.seekTo(positionMs)
    fun setVolume(volume: Float) = castManager.setVolume(volume)
}
