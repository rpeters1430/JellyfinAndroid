package com.rpeters.jellyfin.ui.screens.home

import com.rpeters.jellyfin.ui.components.CarouselItem
import com.rpeters.jellyfin.ui.components.MediaType
import com.rpeters.jellyfin.ui.viewmodel.MainAppState
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class HomeContentHelpersTest {

    // =========================================================
    // getContinueWatchingItems
    // =========================================================

    @Test
    fun `getContinueWatchingItems_withLimit_returnsCorrectCount`() {
        val items = List(5) { mockk<BaseItemDto>(relaxed = true) }
        val state = MainAppState(continueWatching = items)

        val result = getContinueWatchingItems(state, 3)

        assertEquals(3, result.size)
    }

    @Test
    fun `getContinueWatchingItems_withEmptyList_returnsEmpty`() {
        val state = MainAppState(continueWatching = emptyList())

        val result = getContinueWatchingItems(state, 5)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getContinueWatchingItems_limitExceedsSize_returnsAll`() {
        val items = List(2) { mockk<BaseItemDto>(relaxed = true) }
        val state = MainAppState(continueWatching = items)

        val result = getContinueWatchingItems(state, 10)

        assertEquals(2, result.size)
    }

    // =========================================================
    // itemSubtitle (HomeContent.kt version)
    // =========================================================

    @Test
    fun `itemSubtitle_episode_returnsSeriesName`() {
        val item = mockk<BaseItemDto>(relaxed = true) {
            every { type } returns BaseItemKind.EPISODE
            every { seriesName } returns "Breaking Bad"
        }
        assertEquals("Breaking Bad", itemSubtitle(item))
    }

    @Test
    fun `itemSubtitle_episodeNullSeriesName_returnsEmpty`() {
        val item = mockk<BaseItemDto>(relaxed = true) {
            every { type } returns BaseItemKind.EPISODE
            every { seriesName } returns null
        }
        assertEquals("", itemSubtitle(item))
    }

    @Test
    fun `itemSubtitle_movie_returnsProductionYear`() {
        val item = mockk<BaseItemDto>(relaxed = true) {
            every { type } returns BaseItemKind.MOVIE
            every { productionYear } returns 2022
        }
        assertEquals("2022", itemSubtitle(item))
    }

    @Test
    fun `itemSubtitle_movieNullYear_returnsEmpty`() {
        val item = mockk<BaseItemDto>(relaxed = true) {
            every { type } returns BaseItemKind.MOVIE
            every { productionYear } returns null
        }
        assertEquals("", itemSubtitle(item))
    }

    @Test
    fun `itemSubtitle_series_returnsProductionYear`() {
        val item = mockk<BaseItemDto>(relaxed = true) {
            every { type } returns BaseItemKind.SERIES
            every { productionYear } returns 2019
        }
        assertEquals("2019", itemSubtitle(item))
    }

    @Test
    fun `itemSubtitle_audio_returnsFirstArtist`() {
        val item = mockk<BaseItemDto>(relaxed = true) {
            every { type } returns BaseItemKind.AUDIO
            every { artists } returns listOf("Artist A", "Artist B")
        }
        assertEquals("Artist A", itemSubtitle(item))
    }

    @Test
    fun `itemSubtitle_audioNoArtists_returnsEmpty`() {
        val item = mockk<BaseItemDto>(relaxed = true) {
            every { type } returns BaseItemKind.AUDIO
            every { artists } returns null
        }
        assertEquals("", itemSubtitle(item))
    }

    // =========================================================
    // toCarouselItem
    // =========================================================

    @Test
    fun `toCarouselItem_mapsFieldsCorrectly`() {
        val itemId = UUID.randomUUID()
        val item = mockk<BaseItemDto>(relaxed = true) {
            every { id } returns itemId
        }

        val result = item.toCarouselItem(
            titleOverride = "Override Title",
            subtitleOverride = "2022",
            imageUrl = "https://example.com/img.jpg",
        )

        assertEquals(itemId.toString(), result.id)
        assertEquals("Override Title", result.title)
        assertEquals("2022", result.subtitle)
        assertEquals("https://example.com/img.jpg", result.imageUrl)
    }

    @Test
    fun `toCarouselItem_respectsTitleOverride`() {
        val item = mockk<BaseItemDto>(relaxed = true) {
            every { id } returns UUID.randomUUID()
            every { name } returns "Original Name"
        }

        val result = item.toCarouselItem(
            titleOverride = "Different Title",
            subtitleOverride = "",
            imageUrl = "",
        )

        assertEquals("Different Title", result.title)
    }

    @Test
    fun `toCarouselItem_respectsSubtitleOverride`() {
        val item = mockk<BaseItemDto>(relaxed = true) {
            every { id } returns UUID.randomUUID()
        }

        val result = item.toCarouselItem(
            titleOverride = "Title",
            subtitleOverride = "Custom Subtitle",
            imageUrl = "",
        )

        assertEquals("Custom Subtitle", result.subtitle)
    }

    @Test
    fun `toCarouselItem_mapsImageUrl`() {
        val item = mockk<BaseItemDto>(relaxed = true) {
            every { id } returns UUID.randomUUID()
        }
        val url = "https://server.local/image.jpg"

        val result = item.toCarouselItem(
            titleOverride = "T",
            subtitleOverride = "",
            imageUrl = url,
        )

        assertEquals(url, result.imageUrl)
    }

    @Test
    fun `toCarouselItem_typeDefaultsToMovie_evenForSeriesItem`() {
        // The extension has no 'type' param so CarouselItem.type always gets its default (MOVIE)
        val item = mockk<BaseItemDto>(relaxed = true) {
            every { id } returns UUID.randomUUID()
            every { type } returns BaseItemKind.SERIES
        }

        val result = item.toCarouselItem(
            titleOverride = "A Series",
            subtitleOverride = "",
            imageUrl = "",
        )

        assertEquals(MediaType.MOVIE, result.type)
    }
}
