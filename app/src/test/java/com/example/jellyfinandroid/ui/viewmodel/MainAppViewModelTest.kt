package com.example.jellyfinandroid.ui.viewmodel

import com.example.jellyfinandroid.data.SecureCredentialManager
import com.example.jellyfinandroid.data.repository.ApiResult
import com.example.jellyfinandroid.data.repository.ErrorType
import com.example.jellyfinandroid.data.repository.JellyfinRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Comprehensive test suite for MainAppViewModel.
 * 
 * Tests data loading, user actions, state management, error handling,
 * and interaction with the repository layer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainAppViewModelTest {

    private lateinit var viewModel: MainAppViewModel
    private lateinit var mockRepository: JellyfinRepository
    private lateinit var mockCredentialManager: SecureCredentialManager
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockRepository = mockk()
        mockCredentialManager = mockk()

        // Setup default repository flows
        every { mockRepository.currentServer } returns MutableStateFlow(null)
        every { mockRepository.isConnected } returns MutableStateFlow(false)

        viewModel = MainAppViewModel(mockRepository, mockCredentialManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() {
        // Arrange & Act - viewModel is created in setup

        // Assert
        val initialState = viewModel.appState.value
        assertFalse("Should not be loading initially", initialState.isLoading)
        assertTrue("Libraries should be empty initially", initialState.libraries.isEmpty())
        assertTrue("Recently added should be empty initially", initialState.recentlyAdded.isEmpty())
        assertTrue("Recently added by types should be empty initially", initialState.recentlyAddedByTypes.isEmpty())
        assertTrue("Favorites should be empty initially", initialState.favorites.isEmpty())
        assertTrue("Search results should be empty initially", initialState.searchResults.isEmpty())
        assertEquals("Search query should be empty initially", "", initialState.searchQuery)
        assertFalse("Should not be searching initially", initialState.isSearching)
        assertTrue("All items should be empty initially", initialState.allItems.isEmpty())
        assertFalse("Should not be loading more initially", initialState.isLoadingMore)
        assertTrue("Should have more items initially", initialState.hasMoreItems)
        assertEquals("Current page should be 0 initially", 0, initialState.currentPage)
        assertNull("Error message should be null initially", initialState.errorMessage)
    }

    @Test
    fun `loadInitialData loads all data successfully`() = runTest {
        // Arrange
        val mockLibraries = createMockLibraries()
        val mockRecentItems = createMockRecentItems()
        val mockRecentByTypes = createMockRecentByTypes()
        val mockLibraryItems = createMockLibraryItems()

        coEvery { mockRepository.getUserLibraries() } returns ApiResult.Success(mockLibraries)
        coEvery { mockRepository.getRecentlyAdded() } returns ApiResult.Success(mockRecentItems)
        coEvery { mockRepository.getRecentlyAddedByTypes(any()) } returns ApiResult.Success(mockRecentByTypes)
        coEvery { mockRepository.getLibraryItems(any(), any(), any(), any()) } returns ApiResult.Success(mockLibraryItems)

        // Act
        viewModel.loadInitialData()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.appState.value
        assertFalse("Should not be loading after completion", state.isLoading)
        assertEquals("Should have loaded libraries", mockLibraries.size, state.libraries.size)
        assertEquals("Should have loaded recent items", mockRecentItems.size, state.recentlyAdded.size)
        assertEquals("Should have loaded recent by types", mockRecentByTypes.size, state.recentlyAddedByTypes.size)
        assertEquals("Should have loaded library items", mockLibraryItems.size, state.allItems.size)
        assertNull("Error message should be null", state.errorMessage)

        coVerify { mockRepository.getUserLibraries() }
        coVerify { mockRepository.getRecentlyAdded() }
        coVerify { mockRepository.getRecentlyAddedByTypes(any()) }
        coVerify { mockRepository.getLibraryItems(any(), any(), any(), any()) }
    }

    @Test
    fun `loadInitialData handles library loading error`() = runTest {
        // Arrange
        val errorMessage = "Failed to load libraries"
        coEvery { mockRepository.getUserLibraries() } returns ApiResult.Error(errorMessage, errorType = ErrorType.NETWORK)
        coEvery { mockRepository.getRecentlyAdded() } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.getRecentlyAddedByTypes(any()) } returns ApiResult.Success(emptyMap())
        coEvery { mockRepository.getLibraryItems(any(), any(), any(), any()) } returns ApiResult.Success(emptyList())

        // Act
        viewModel.loadInitialData()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.appState.value
        assertFalse("Should not be loading after completion", state.isLoading)
        assertTrue("Libraries should be empty", state.libraries.isEmpty())
        assertNotNull("Error message should be set", state.errorMessage)
        assertTrue("Error message should contain failure info", state.errorMessage!!.contains("libraries"))
    }

    @Test
    fun `toggleFavorite calls repository and refreshes data`() = runTest {
        // Arrange
        val mockItem = createMockMovie("Test Movie")
        coEvery { mockRepository.toggleFavorite(any(), any()) } returns ApiResult.Success(true)
        coEvery { mockRepository.getUserLibraries() } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.getRecentlyAdded() } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.getRecentlyAddedByTypes(any()) } returns ApiResult.Success(emptyMap())
        coEvery { mockRepository.getLibraryItems(any(), any(), any(), any()) } returns ApiResult.Success(emptyList())

        // Act
        viewModel.toggleFavorite(mockItem)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify { mockRepository.toggleFavorite(mockItem.id.toString(), true) }
        // Verify that loadInitialData was called to refresh
        coVerify { mockRepository.getUserLibraries() }
    }

    @Test
    fun `toggleFavorite handles error correctly`() = runTest {
        // Arrange
        val mockItem = createMockMovie("Test Movie")
        val errorMessage = "Failed to toggle favorite"
        coEvery { mockRepository.toggleFavorite(any(), any()) } returns ApiResult.Error(errorMessage, errorType = ErrorType.NETWORK)

        // Act
        viewModel.toggleFavorite(mockItem)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.appState.value
        assertNotNull("Error message should be set", state.errorMessage)
        assertTrue("Error message should contain failure info", state.errorMessage!!.contains("favorite"))
        coVerify { mockRepository.toggleFavorite(mockItem.id.toString(), true) }
    }

    @Test
    fun `markAsWatched calls repository and refreshes data`() = runTest {
        // Arrange
        val mockItem = createMockMovie("Test Movie")
        coEvery { mockRepository.markAsWatched(any()) } returns ApiResult.Success(true)
        coEvery { mockRepository.getUserLibraries() } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.getRecentlyAdded() } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.getRecentlyAddedByTypes(any()) } returns ApiResult.Success(emptyMap())
        coEvery { mockRepository.getLibraryItems(any(), any(), any(), any()) } returns ApiResult.Success(emptyList())

        // Act
        viewModel.markAsWatched(mockItem)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify { mockRepository.markAsWatched(mockItem.id.toString()) }
        // Verify that loadInitialData was called to refresh
        coVerify { mockRepository.getUserLibraries() }
    }

    @Test
    fun `markAsUnwatched calls repository and refreshes data`() = runTest {
        // Arrange
        val mockItem = createMockMovie("Test Movie")
        coEvery { mockRepository.markAsUnwatched(any()) } returns ApiResult.Success(true)
        coEvery { mockRepository.getUserLibraries() } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.getRecentlyAdded() } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.getRecentlyAddedByTypes(any()) } returns ApiResult.Success(emptyMap())
        coEvery { mockRepository.getLibraryItems(any(), any(), any(), any()) } returns ApiResult.Success(emptyList())

        // Act
        viewModel.markAsUnwatched(mockItem)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify { mockRepository.markAsUnwatched(mockItem.id.toString()) }
        // Verify that loadInitialData was called to refresh
        coVerify { mockRepository.getUserLibraries() }
    }

    @Test
    fun `search updates state correctly`() = runTest {
        // Arrange
        val query = "test query"
        val searchResults = createMockSearchResults()
        coEvery { mockRepository.searchItems(query) } returns ApiResult.Success(searchResults)

        // Act
        viewModel.search(query)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.appState.value
        assertEquals("Search query should be set", query, state.searchQuery)
        assertFalse("Should not be searching after completion", state.isSearching)
        assertEquals("Should have search results", searchResults.size, state.searchResults.size)
        assertNull("Error message should be null", state.errorMessage)
        coVerify { mockRepository.searchItems(query) }
    }

    @Test
    fun `search handles error correctly`() = runTest {
        // Arrange
        val query = "test query"
        val errorMessage = "Search failed"
        coEvery { mockRepository.searchItems(query) } returns ApiResult.Error(errorMessage, errorType = ErrorType.NETWORK)

        // Act
        viewModel.search(query)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.appState.value
        assertEquals("Search query should be set", query, state.searchQuery)
        assertFalse("Should not be searching after completion", state.isSearching)
        assertTrue("Search results should be empty", state.searchResults.isEmpty())
        assertNotNull("Error message should be set", state.errorMessage)
        assertTrue("Error message should contain search failure", state.errorMessage!!.contains("Search failed"))
    }

    @Test
    fun `clearSearch resets search state`() {
        // Act
        viewModel.clearSearch()

        // Assert
        val state = viewModel.appState.value
        assertEquals("Search query should be empty", "", state.searchQuery)
        assertFalse("Should not be searching", state.isSearching)
        assertTrue("Search results should be empty", state.searchResults.isEmpty())
    }

    @Test
    fun `loadFavorites loads favorites successfully`() = runTest {
        // Arrange
        val mockFavorites = createMockFavorites()
        coEvery { mockRepository.getFavorites() } returns ApiResult.Success(mockFavorites)

        // Act
        viewModel.loadFavorites()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.appState.value
        assertFalse("Should not be loading after completion", state.isLoading)
        assertEquals("Should have loaded favorites", mockFavorites.size, state.favorites.size)
        assertNull("Error message should be null", state.errorMessage)
        coVerify { mockRepository.getFavorites() }
    }

    @Test
    fun `loadFavorites handles error correctly`() = runTest {
        // Arrange
        val errorMessage = "Failed to load favorites"
        coEvery { mockRepository.getFavorites() } returns ApiResult.Error(errorMessage, errorType = ErrorType.NETWORK)

        // Act
        viewModel.loadFavorites()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.appState.value
        assertFalse("Should not be loading after completion", state.isLoading)
        assertTrue("Favorites should be empty", state.favorites.isEmpty())
        assertNotNull("Error message should be set", state.errorMessage)
        assertTrue("Error message should contain failure info", state.errorMessage!!.contains("favorites"))
    }

    @Test
    fun `clearError removes error message`() {
        // Arrange - set an error message first
        viewModel.search("invalid query") // This will set an error in a real scenario
        
        // Act
        viewModel.clearError()

        // Assert
        assertNull("Error message should be cleared", viewModel.appState.value.errorMessage)
    }

    @Test
    fun `logout calls repository and credential manager`() = runTest {
        // Arrange
        every { mockRepository.logout() } returns Unit
        coEvery { mockCredentialManager.clearCredentials() } returns Unit

        // Act
        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify { mockRepository.logout() }
        coVerify { mockCredentialManager.clearCredentials() }
    }

    @Test
    fun `getStreamUrl returns repository stream URL`() {
        // Arrange
        val mockItem = createMockMovie("Test Movie")
        val expectedUrl = "https://test.jellyfin.org/Videos/${mockItem.id}/stream"
        every { mockRepository.getStreamUrl(mockItem.id.toString()) } returns expectedUrl

        // Act
        val result = viewModel.getStreamUrl(mockItem)

        // Assert
        assertEquals("Should return repository stream URL", expectedUrl, result)
        verify { mockRepository.getStreamUrl(mockItem.id.toString()) }
    }

    @Test
    fun `loadMoreItems loads additional items when not loading and has more`() = runTest {
        // Arrange
        val additionalItems = createMockLibraryItems()
        coEvery { mockRepository.getLibraryItems(any(), any(), any(), any()) } returns ApiResult.Success(additionalItems)

        // Act
        viewModel.loadMoreItems()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify { mockRepository.getLibraryItems(any(), any(), any(), any()) }
    }

    @Test
    fun `refreshLibraryItems reloads library items from start`() = runTest {
        // Arrange
        val libraryItems = createMockLibraryItems()
        coEvery { mockRepository.getLibraryItems(any(), any(), any(), any()) } returns ApiResult.Success(libraryItems)

        // Act
        viewModel.refreshLibraryItems()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify { mockRepository.getLibraryItems(startIndex = 0, limit = any(), parentId = any(), itemTypes = any()) }
    }

    // Helper methods to create mock data
    private fun createMockLibraries(): List<BaseItemDto> {
        return listOf(
            BaseItemDto(
                id = UUID.randomUUID(),
                name = "Movies",
                type = BaseItemKind.COLLECTION_FOLDER,
                collectionType = "movies"
            ),
            BaseItemDto(
                id = UUID.randomUUID(),
                name = "TV Shows",
                type = BaseItemKind.COLLECTION_FOLDER,
                collectionType = "tvshows"
            )
        )
    }

    private fun createMockRecentItems(): List<BaseItemDto> {
        return listOf(
            createMockMovie("Recent Movie 1"),
            createMockMovie("Recent Movie 2"),
            createMockTVShow("Recent TV Show 1")
        )
    }

    private fun createMockRecentByTypes(): Map<String, List<BaseItemDto>> {
        return mapOf(
            "Movies" to listOf(createMockMovie("Recent Movie")),
            "TV Shows" to listOf(createMockTVShow("Recent TV Show"))
        )
    }

    private fun createMockLibraryItems(): List<BaseItemDto> {
        return listOf(
            createMockMovie("Library Movie 1"),
            createMockMovie("Library Movie 2"),
            createMockTVShow("Library TV Show 1")
        )
    }

    private fun createMockSearchResults(): List<BaseItemDto> {
        return listOf(
            createMockMovie("Search Result Movie"),
            createMockTVShow("Search Result TV Show")
        )
    }

    private fun createMockFavorites(): List<BaseItemDto> {
        return listOf(
            createMockMovie("Favorite Movie"),
            createMockTVShow("Favorite TV Show")
        )
    }

    private fun createMockMovie(name: String): BaseItemDto {
        return BaseItemDto(
            id = UUID.randomUUID(),
            name = name,
            type = BaseItemKind.MOVIE,
            productionYear = 2023,
            userData = org.jellyfin.sdk.model.api.UserItemDataDto(
                isFavorite = false,
                played = false
            )
        )
    }

    private fun createMockTVShow(name: String): BaseItemDto {
        return BaseItemDto(
            id = UUID.randomUUID(),
            name = name,
            type = BaseItemKind.SERIES,
            productionYear = 2023,
            userData = org.jellyfin.sdk.model.api.UserItemDataDto(
                isFavorite = false,
                played = false
            )
        )
    }
}