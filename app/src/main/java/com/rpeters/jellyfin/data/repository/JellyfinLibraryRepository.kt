package com.rpeters.jellyfin.data.repository

import com.rpeters.jellyfin.core.constants.Constants
import com.rpeters.jellyfin.data.repository.common.ApiResult
import com.rpeters.jellyfin.data.repository.common.ErrorType
import com.rpeters.jellyfin.ui.utils.RetryManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JellyfinLibraryRepository @Inject constructor(
    private val authRepository: JellyfinAuthRepository,
    private val retryManager: RetryManager,
) {

    /**
     * Get user libraries with enhanced error handling and retry mechanism
     */
    suspend fun getUserLibraries(): Flow<ApiResult<List<BaseItemDto>>> = flow {
        try {
            val server = authRepository.getCurrentServer()
            if (server?.accessToken == null || server.userId == null) {
                emit(ApiResult.Error(ErrorType.UNAUTHORIZED, "Not authenticated"))
                return@flow
            }

            val result = retryManager.executeWithRetry("getUserLibraries") {
                server.api.userLibraryApi.getUserViews(
                    userId = server.userId,
                    includeExternalContent = true,
                )
            }

            emit(ApiResult.Success(result.items))
        } catch (e: Exception) {
            emit(ApiResult.Error(ErrorType.UNKNOWN, e.message ?: "Unknown error"))
        }
    }

    /**
     * Get recently added items for a library with configurable limit
     */
    suspend fun getRecentlyAdded(
        libraryId: String,
        limit: Int = Constants.RECENTLY_ADDED_LIMIT,
    ): Flow<ApiResult<List<BaseItemDto>>> = flow {
        try {
            val server = authRepository.getCurrentServer()
            if (server?.accessToken == null || server.userId == null) {
                emit(ApiResult.Error(ErrorType.UNAUTHORIZED, "Not authenticated"))
                return@flow
            }

            val result = retryManager.executeWithRetry("getRecentlyAdded") {
                server.api.userLibraryApi.getLatestMedia(
                    userId = server.userId,
                    parentId = libraryId,
                    limit = limit,
                    fields = listOf(
                        ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                        ItemFields.BASIC_SYNOPSIS,
                        ItemFields.MEDIA_SOURCES,
                        ItemFields.MEDIA_STREAMS,
                    ),
                )
            }

            emit(ApiResult.Success(result))
        } catch (e: Exception) {
            emit(ApiResult.Error(ErrorType.UNKNOWN, e.message ?: "Unknown error"))
        }
    }

    /**
     * Get recently added items by type with enhanced filtering
     */
    suspend fun getRecentlyAddedByType(
        libraryId: String,
        itemType: BaseItemKind,
        limit: Int = Constants.RECENTLY_ADDED_BY_TYPE_LIMIT,
    ): Flow<ApiResult<List<BaseItemDto>>> = flow {
        try {
            val server = authRepository.getCurrentServer()
            if (server?.accessToken == null || server.userId == null) {
                emit(ApiResult.Error(ErrorType.UNAUTHORIZED, "Not authenticated"))
                return@flow
            }

            val result = retryManager.executeWithRetry("getRecentlyAddedByType") {
                server.api.userLibraryApi.getLatestMedia(
                    userId = server.userId,
                    parentId = libraryId,
                    includeItemTypes = listOf(itemType),
                    limit = limit,
                    fields = listOf(
                        ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                        ItemFields.BASIC_SYNOPSIS,
                        ItemFields.MEDIA_SOURCES,
                        ItemFields.MEDIA_STREAMS,
                    ),
                )
            }

            emit(ApiResult.Success(result))
        } catch (e: Exception) {
            emit(ApiResult.Error(ErrorType.UNKNOWN, e.message ?: "Unknown error"))
        }
    }

    /**
     * Get items by library type with advanced filtering and sorting
     */
    suspend fun getItemsByType(
        libraryId: String,
        itemType: BaseItemKind,
        startIndex: Int = 0,
        limit: Int = Constants.LARGE_PAGE_SIZE,
        sortBy: ItemSortBy = ItemSortBy.SORT_NAME,
        sortOrder: SortOrder = SortOrder.ASCENDING,
        filters: List<ItemFilter> = emptyList(),
    ): Flow<ApiResult<List<BaseItemDto>>> = flow {
        try {
            val server = authRepository.getCurrentServer()
            if (server?.accessToken == null || server.userId == null) {
                emit(ApiResult.Error(ErrorType.UNAUTHORIZED, "Not authenticated"))
                return@flow
            }

            val result = retryManager.executeWithRetry("getItemsByType") {
                server.api.userLibraryApi.getItems(
                    userId = server.userId,
                    parentId = libraryId,
                    includeItemTypes = listOf(itemType),
                    startIndex = startIndex,
                    limit = limit,
                    sortBy = listOf(sortBy),
                    sortOrder = listOf(sortOrder),
                    filters = filters,
                    fields = listOf(
                        ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                        ItemFields.BASIC_SYNOPSIS,
                        ItemFields.MEDIA_SOURCES,
                        ItemFields.MEDIA_STREAMS,
                        ItemFields.USER_DATA,
                    ),
                )
            }

            emit(ApiResult.Success(result.items))
        } catch (e: Exception) {
            emit(ApiResult.Error(ErrorType.UNKNOWN, e.message ?: "Unknown error"))
        }
    }

    /**
     * Get continue watching items for a library
     */
    suspend fun getContinueWatching(
        libraryId: String,
        limit: Int = Constants.CONTINUE_WATCHING_LIMIT,
    ): Flow<ApiResult<List<BaseItemDto>>> = flow {
        try {
            val server = authRepository.getCurrentServer()
            if (server?.accessToken == null || server.userId == null) {
                emit(ApiResult.Error(ErrorType.UNAUTHORIZED, "Not authenticated"))
                return@flow
            }

            val result = retryManager.executeWithRetry("getContinueWatching") {
                server.api.userLibraryApi.getResumeItems(
                    userId = server.userId,
                    parentId = libraryId,
                    limit = limit,
                    fields = listOf(
                        ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                        ItemFields.BASIC_SYNOPSIS,
                        ItemFields.MEDIA_SOURCES,
                        ItemFields.MEDIA_STREAMS,
                        ItemFields.USER_DATA,
                    ),
                )
            }

            emit(ApiResult.Success(result.items))
        } catch (e: Exception) {
            emit(ApiResult.Error(ErrorType.UNKNOWN, e.message ?: "Unknown error"))
        }
    }

    /**
     * Get next up items for TV series
     */
    suspend fun getNextUp(
        libraryId: String,
        limit: Int = Constants.CONTINUE_WATCHING_LIMIT,
    ): Flow<ApiResult<List<BaseItemDto>>> = flow {
        try {
            val server = authRepository.getCurrentServer()
            if (server?.accessToken == null || server.userId == null) {
                emit(ApiResult.Error(ErrorType.UNAUTHORIZED, "Not authenticated"))
                return@flow
            }

            val result = retryManager.executeWithRetry("getNextUp") {
                server.api.userLibraryApi.getNextUp(
                    userId = server.userId,
                    parentId = libraryId,
                    limit = limit,
                    fields = listOf(
                        ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                        ItemFields.BASIC_SYNOPSIS,
                        ItemFields.MEDIA_SOURCES,
                        ItemFields.MEDIA_STREAMS,
                        ItemFields.USER_DATA,
                    ),
                )
            }

            emit(ApiResult.Success(result.items))
        } catch (e: Exception) {
            emit(ApiResult.Error(ErrorType.UNKNOWN, e.message ?: "Unknown error"))
        }
    }

    /**
     * Get favorite items for a library
     */
    suspend fun getFavorites(
        libraryId: String,
        startIndex: Int = 0,
        limit: Int = Constants.LARGE_PAGE_SIZE,
    ): Flow<ApiResult<List<BaseItemDto>>> = flow {
        try {
            val server = authRepository.getCurrentServer()
            if (server?.accessToken == null || server.userId == null) {
                emit(ApiResult.Error(ErrorType.UNAUTHORIZED, "Not authenticated"))
                return@flow
            }

            val result = retryManager.executeWithRetry("getFavorites") {
                server.api.userLibraryApi.getItems(
                    userId = server.userId,
                    parentId = libraryId,
                    startIndex = startIndex,
                    limit = limit,
                    filters = listOf(ItemFilter.IS_FAVORITE),
                    sortBy = listOf(ItemSortBy.SORT_NAME),
                    sortOrder = listOf(SortOrder.ASCENDING),
                    fields = listOf(
                        ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                        ItemFields.BASIC_SYNOPSIS,
                        ItemFields.MEDIA_SOURCES,
                        ItemFields.MEDIA_STREAMS,
                        ItemFields.USER_DATA,
                    ),
                )
            }

            emit(ApiResult.Success(result.items))
        } catch (e: Exception) {
            emit(ApiResult.Error(ErrorType.UNKNOWN, e.message ?: "Unknown error"))
        }
    }

    /**
     * Get unwatched items for a library
     */
    suspend fun getUnwatched(
        libraryId: String,
        startIndex: Int = 0,
        limit: Int = Constants.LARGE_PAGE_SIZE,
    ): Flow<ApiResult<List<BaseItemDto>>> = flow {
        try {
            val server = authRepository.getCurrentServer()
            if (server?.accessToken == null || server.userId == null) {
                emit(ApiResult.Error(ErrorType.UNAUTHORIZED, "Not authenticated"))
                return@flow
            }

            val result = retryManager.executeWithRetry("getUnwatched") {
                server.api.userLibraryApi.getItems(
                    userId = server.userId,
                    parentId = libraryId,
                    startIndex = startIndex,
                    limit = limit,
                    filters = listOf(ItemFilter.IS_UNPLAYED),
                    sortBy = listOf(ItemSortBy.SORT_NAME),
                    sortOrder = listOf(SortOrder.ASCENDING),
                    fields = listOf(
                        ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                        ItemFields.BASIC_SYNOPSIS,
                        ItemFields.MEDIA_SOURCES,
                        ItemFields.MEDIA_STREAMS,
                        ItemFields.USER_DATA,
                    ),
                )
            }

            emit(ApiResult.Success(result.items))
        } catch (e: Exception) {
            emit(ApiResult.Error(ErrorType.UNKNOWN, e.message ?: "Unknown error"))
        }
    }
}
