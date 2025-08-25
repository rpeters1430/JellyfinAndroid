package com.rpeters.jellyfin.data.repository

import com.rpeters.jellyfin.core.constants.Constants
import com.rpeters.jellyfin.data.repository.common.ApiResult
import com.rpeters.jellyfin.data.repository.common.ErrorType
import com.rpeters.jellyfin.ui.utils.RetryManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.model.api.PlaybackInfoDto
import org.jellyfin.sdk.model.api.PlaybackInfoResponse
import org.jellyfin.sdk.model.api.PlaybackProgressInfo
import org.jellyfin.sdk.model.api.RepeatMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JellyfinPlaybackRepository @Inject constructor(
    private val authRepository: JellyfinAuthRepository,
    private val retryManager: RetryManager
) {
    
    /**
     * Get playback info for an item
     */
    suspend fun getPlaybackInfo(
        itemId: String,
        startPositionTicks: Long? = null
    ): Flow<ApiResult<PlaybackInfoResponse>> = flow {
        try {
            val server = authRepository.getCurrentServer()
            if (server?.accessToken == null || server.userId == null) {
                emit(ApiResult.Error(ErrorType.UNAUTHORIZED, "Not authenticated"))
                return@flow
            }
            
            val result = retryManager.executeWithRetry("getPlaybackInfo") {
                server.api.playStateApi.getPlaybackInfo(
                    itemId = itemId,
                    userId = server.userId,
                    startPositionTicks = startPositionTicks
                )
            }
            
            emit(ApiResult.Success(result))
        } catch (e: Exception) {
            emit(ApiResult.Error(ErrorType.UNKNOWN, e.message ?: "Unknown error"))
        }
    }
    
    /**
     * Report playback progress
     */
    suspend fun reportPlaybackProgress(
        itemId: String,
        positionTicks: Long,
        isPaused: Boolean = false,
        isMuted: Boolean = false,
        volumeLevel: Int = 100,
        playbackStartTime: Long? = null,
        canSeek: Boolean = true,
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null,
        repeatMode: RepeatMode = RepeatMode.REPEAT_NONE
    ): Flow<ApiResult<Unit>> = flow {
        try {
            val server = authRepository.getCurrentServer()
            if (server?.accessToken == null || server.userId == null) {
                emit(ApiResult.Error(ErrorType.UNAUTHORIZED, "Not authenticated"))
                return@flow
            }
            
            val progressInfo = PlaybackProgressInfo(
                itemId = itemId,
                positionTicks = positionTicks,
                isPaused = isPaused,
                isMuted = isMuted,
                volumeLevel = volumeLevel,
                playbackStartTime = playbackStartTime,
                canSeek = canSeek,
                audioStreamIndex = audioStreamIndex,
                subtitleStreamIndex = subtitleStreamIndex,
                repeatMode = repeatMode
            )
            
            retryManager.executeWithRetry("reportPlaybackProgress") {
                server.api.playStateApi.reportPlaybackProgress(progressInfo)
            }
            
            emit(ApiResult.Success(Unit))
        } catch (e: Exception) {
            emit(ApiResult.Error(ErrorType.UNKNOWN, e.message ?: "Unknown error"))
        }
    }
    
    /**
     * Report playback stopped
     */
    suspend fun reportPlaybackStopped(
        itemId: String,
        positionTicks: Long,
        playbackStartTime: Long? = null
    ): Flow<ApiResult<Unit>> = flow {
        try {
            val server = authRepository.getCurrentServer()
            if (server?.accessToken == null || server.userId == null) {
                emit(ApiResult.Error(ErrorType.UNAUTHORIZED, "Not authenticated"))
                return@flow
            }
            
            val progressInfo = PlaybackProgressInfo(
                itemId = itemId,
                positionTicks = positionTicks,
                playbackStartTime = playbackStartTime
            )
            
            retryManager.executeWithRetry("reportPlaybackStopped") {
                server.api.playStateApi.reportPlaybackStopped(progressInfo)
            }
            
            emit(ApiResult.Success(Unit))
        } catch (e: Exception) {
            emit(ApiResult.Error(ErrorType.UNKNOWN, e.message ?: "Unknown error"))
        }
    }
    
    /**
     * Mark item as played
     */
    suspend fun markAsPlayed(
        itemId: String,
        datePlayed: Long? = null
    ): Flow<ApiResult<Unit>> = flow {
        try {
            val server = authRepository.getCurrentServer()
            if (server?.accessToken == null || server.userId == null) {
                emit(ApiResult.Error(ErrorType.UNAUTHORIZED, "Not authenticated"))
                return@flow
            }
            
            retryManager.executeWithRetry("markAsPlayed") {
                server.api.playStateApi.markAsPlayed(
                    itemId = itemId,
                    userId = server.userId,
                    datePlayed = datePlayed
                )
            }
            
            emit(ApiResult.Success(Unit))
        } catch (e: Exception) {
            emit(ApiResult.Error(ErrorType.UNKNOWN, e.message ?: "Unknown error"))
        }
    }
    
    /**
     * Mark item as unplayed
     */
    suspend fun markAsUnplayed(
        itemId: String
    ): Flow<ApiResult<Unit>> = flow {
        try {
            val server = authRepository.getCurrentServer()
            if (server?.accessToken == null || server.userId == null) {
                emit(ApiResult.Error(ErrorType.UNAUTHORIZED, "Not authenticated"))
                return@flow
            }
            
            retryManager.executeWithRetry("markAsUnplayed") {
                server.api.playStateApi.markAsUnplayed(
                    itemId = itemId,
                    userId = server.userId
                )
            }
            
            emit(ApiResult.Success(Unit))
        } catch (e: Exception) {
            emit(ApiResult.Error(ErrorType.UNKNOWN, e.message ?: "Unknown error"))
        }
    }
    
    /**
     * Update user item data (favorites, played status, etc.)
     */
    suspend fun updateUserItemData(
        itemId: String,
        isFavorite: Boolean? = null,
        played: Boolean? = null,
        playedPercentage: Double? = null,
        unplayedItemCount: Int? = null
    ): Flow<ApiResult<Unit>> = flow {
        try {
            val server = authRepository.getCurrentServer()
            if (server?.accessToken == null || server.userId == null) {
                emit(ApiResult.Error(ErrorType.UNAUTHORIZED, "Not authenticated"))
                return@flow
            }
            
            retryManager.executeWithRetry("updateUserItemData") {
                server.api.playStateApi.updateUserItemData(
                    itemId = itemId,
                    userId = server.userId,
                    isFavorite = isFavorite,
                    played = played,
                    playedPercentage = playedPercentage,
                    unplayedItemCount = unplayedItemCount
                )
            }
            
            emit(ApiResult.Success(Unit))
        } catch (e: Exception) {
            emit(ApiResult.Error(ErrorType.UNKNOWN, e.message ?: "Unknown error"))
        }
    }
    
    /**
     * Get user item data
     */
    suspend fun getUserItemData(
        itemId: String
    ): Flow<ApiResult<org.jellyfin.sdk.model.api.UserItemDataDto>> = flow {
        try {
            val server = authRepository.getCurrentServer()
            if (server?.accessToken == null || server.userId == null) {
                emit(ApiResult.Error(ErrorType.UNAUTHORIZED, "Not authenticated"))
                return@flow
            }
            
            val result = retryManager.executeWithRetry("getUserItemData") {
                server.api.playStateApi.getUserItemData(
                    itemId = itemId,
                    userId = server.userId
                )
            }
            
            emit(ApiResult.Success(result))
        } catch (e: Exception) {
            emit(ApiResult.Error(ErrorType.UNKNOWN, e.message ?: "Unknown error"))
        }
    }
    
    /**
     * Toggle favorite status
     */
    suspend fun toggleFavorite(
        itemId: String
    ): Flow<ApiResult<Boolean>> = flow {
        try {
            val server = authRepository.getCurrentServer()
            if (server?.accessToken == null || server.userId == null) {
                emit(ApiResult.Error(ErrorType.UNAUTHORIZED, "Not authenticated"))
                return@flow
            }
            
            // First get current favorite status
            val currentData = retryManager.executeWithRetry("getUserItemData") {
                server.api.playStateApi.getUserItemData(
                    itemId = itemId,
                    userId = server.userId
                )
            }
            
            val newFavoriteStatus = !(currentData.isFavorite ?: false)
            
            // Update to new status
            retryManager.executeWithRetry("updateUserItemData") {
                server.api.playStateApi.updateUserItemData(
                    itemId = itemId,
                    userId = server.userId,
                    isFavorite = newFavoriteStatus
                )
            }
            
            emit(ApiResult.Success(newFavoriteStatus))
        } catch (e: Exception) {
            emit(ApiResult.Error(ErrorType.UNKNOWN, e.message ?: "Unknown error"))
        }
    }
    
    /**
     * Set favorite status
     */
    suspend fun setFavorite(
        itemId: String,
        isFavorite: Boolean
    ): Flow<ApiResult<Unit>> = flow {
        try {
            val server = authRepository.getCurrentServer()
            if (server?.accessToken == null || server.userId == null) {
                emit(ApiResult.Error(ErrorType.UNAUTHORIZED, "Not authenticated"))
                return@flow
            }
            
            retryManager.executeWithRetry("updateUserItemData") {
                server.api.playStateApi.updateUserItemData(
                    itemId = itemId,
                    userId = server.userId,
                    isFavorite = isFavorite
                )
            }
            
            emit(ApiResult.Success(Unit))
        } catch (e: Exception) {
            emit(ApiResult.Error(ErrorType.UNKNOWN, e.message ?: "Unknown error"))
        }
    }
}