# Home Screen Tests Design

**Date:** 2026-03-25
**Status:** Approved

## Overview

Add comprehensive tests for the mobile home screen across two layers:
- **Unit tests** for pure Kotlin helpers in `HomeContent.kt`
- **Instrumentation UI tests** for `MobileExpressiveHomeContent` rendering and interactions

No existing home screen UI tests exist. `MainAppViewModelTest.kt` is disabled. This fills the gap in coverage for the home screen's data derivation logic and Compose rendering.

---

## Architecture

### Files Created / Modified

| File | Type | Action |
|---|---|---|
| `HomeContentHelpersTest.kt` | Unit (JVM) | Create at `app/src/test/.../ui/screens/home/` |
| `ImmersiveHomeScreenTest.kt` | Instrumentation | Create at `app/src/androidTest/.../ui/screens/` |
| `ImmersiveHomeScreen.kt` | Production | Change `MobileExpressiveHomeContent` from `private` to `internal @VisibleForTesting` |

### Patterns

- MockK for mocking `BaseItemDto` (`relaxed = true` + explicit stubs for accessed properties)
- `StandardTestDispatcher` for coroutine control (matches existing `MediaCardsTest` pattern)
- `createComposeRule(effectContext = StandardTestDispatcher())` for instrumentation tests
- `JellyfinAndroidTheme` wrapper on all Compose content
- Image URL lambdas return `null` to skip network calls in UI tests
- `@OptIn(ExperimentalCoroutinesApi::class)` where needed

---

## Unit Tests: `HomeContentHelpersTest.kt`

**Package:** `com.rpeters.jellyfin.ui.screens.home`

Tests the internal pure functions in `HomeContent.kt`. Note: there are **two** `itemSubtitle` functions in the codebase — one in `HomeContent.kt` (`com.rpeters.jellyfin.ui.screens.home`) and one in `ImmersiveMediaRow.kt` (`com.rpeters.jellyfin.ui.components.immersive`). These tests target the `HomeContent.kt` version only, which is used by the non-immersive `HomeContent` composable.

### `getContinueWatchingItems`

| Test | Condition | Expected |
|---|---|---|
| `getContinueWatchingItems_withLimit_returnsCorrectCount` | 5 items, limit 3 | Returns 3 items |
| `getContinueWatchingItems_withEmptyList_returnsEmpty` | Empty `continueWatching` | Returns empty list |
| `getContinueWatchingItems_limitExceedsSize_returnsAll` | 2 items, limit 10 | Returns all 2 items |

### `itemSubtitle` (HomeContent.kt version)

| Test | Item type | Expected |
|---|---|---|
| `itemSubtitle_episode_returnsSeriesName` | EPISODE with seriesName "Breaking Bad" | `"Breaking Bad"` |
| `itemSubtitle_episodeNullSeriesName_returnsEmpty` | EPISODE, seriesName null | `""` |
| `itemSubtitle_movie_returnsProductionYear` | MOVIE, year 2022 | `"2022"` |
| `itemSubtitle_movieNullYear_returnsEmpty` | MOVIE, year null | `""` |
| `itemSubtitle_series_returnsProductionYear` | SERIES, year 2019 | `"2019"` |
| `itemSubtitle_audio_returnsFirstArtist` | AUDIO, artists `["Artist A", "Artist B"]` | `"Artist A"` |
| `itemSubtitle_audioNoArtists_returnsEmpty` | AUDIO, artists null/empty | `""` |

### `toCarouselItem`

| Test | Condition | Expected |
|---|---|---|
| `toCarouselItem_mapsFieldsCorrectly` | Item with id, name | `CarouselItem.id` == item.id.toString(), `CarouselItem.title` == titleOverride |
| `toCarouselItem_respectsTitleOverride` | titleOverride != item.name | Uses titleOverride, not item.name |
| `toCarouselItem_respectsSubtitleOverride` | subtitleOverride provided | Uses subtitleOverride in `subtitle` field |
| `toCarouselItem_mapsImageUrl` | imageUrl `"https://example.com/img.jpg"` | `CarouselItem.imageUrl` matches |
| `toCarouselItem_typeDefaultsToMovie` | Input item is `SERIES`-typed (non-Movie) | `CarouselItem.type == MediaType.MOVIE` — proves the extension never maps from item type, it always gets the `CarouselItem` default |

---

## Production Code Change: Visibility

`MobileExpressiveHomeContent` in `ImmersiveHomeScreen.kt` is currently `private`. To enable direct testing without Hilt ViewModel wiring, change its visibility to `internal` and annotate with `@VisibleForTesting`. This is consistent with the project's existing use of `@VisibleForTesting` on `setAppStateForTest` in `MainAppViewModel`.

```kotlin
@VisibleForTesting
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileExpressiveHomeContent(...)
```

---

## Instrumentation Tests: `ImmersiveHomeScreenTest.kt`

**Package:** `com.rpeters.jellyfin.ui.screens`

Tests `MobileExpressiveHomeContent` directly (phone layout path) after the visibility change above. No Hilt needed — all dependencies are passed as lambdas or `MainAppState`.

### Test Helpers

```kotlin
fun makeMovie(name: String = "Test Movie"): BaseItemDto
fun makeEpisode(name: String = "Test Episode", seriesName: String = "Test Series"): BaseItemDto
fun makeLibrary(name: String = "Movies"): BaseItemDto
```

All `BaseItemDto` mocks use `mockk<BaseItemDto>(relaxed = true)` with explicit stubs for `id`, `name`, `type`, `dateCreated`. Map keys use `BaseItemKind.X.name` (the enum's `.name` property), not string literals.

### Important: Composable Signature

`MobileExpressiveHomeContent` full signature:
```
appState, contentLists, getImageUrl, getBackdropUrl, getSeriesImageUrl,
onItemClick, onItemLongPress, onLibraryClick, viewingMood, contentPadding, modifier
```

- `appState` drives `libraries` (for `LibraryNavigationCarousel`) and `continueWatching`
- `contentLists` drives the hero carousel, next-up, and row sections — must be constructed explicitly
- **`viewingMood` is a standalone `String?` parameter**, NOT derived from `appState.viewingMood` inside the composable. Pass it as a direct argument: `viewingMood = "Action mood"`, not via `appState`

```kotlin
// Example setup for hero test:
val movie = makeMovie("Inception")
val contentLists = HomeContentLists(recentMovies = listOf(movie))
composeTestRule.setContent {
    JellyfinAndroidTheme {
        MobileExpressiveHomeContent(
            appState = MainAppState(),
            contentLists = contentLists,
            viewingMood = null,
            contentPadding = PaddingValues(),
            getImageUrl = { null },
            getBackdropUrl = { null },
            getSeriesImageUrl = { null },
            onItemClick = {},
            onItemLongPress = {},
            onLibraryClick = {},
        )
    }
}
```

`onItemClick`/`onLibraryClick` use `mockk(relaxed = true)` when verifying callbacks. All `BaseItemDto` mocks must stub `id` as a specific deterministic `UUID` (not left to relaxed defaults) so that the ID-string matching inside the composable works correctly.

### Hero Carousel

Hero is driven by `contentLists.recentMovies` (passed directly to `MobileExpressiveHomeContent`, not derived from `appState` at test time). Seed `HomeContentLists(recentMovies = listOf(...))` accordingly.

`ImmersiveHeroCarousel` uses `HorizontalUncontainedCarousel`. Only the first item is guaranteed visible without scrolling — tests assert on the first item only via `onNodeWithText(firstMovie.name)`.

| Test | Setup | Assertion |
|---|---|---|
| `heroCarousel_withMovies_isDisplayed` | `contentLists.recentMovies` = 3 movies | First movie name visible |
| `heroCarousel_withNoMovies_isNotRendered` | `contentLists.recentMovies` = empty | First movie name not in tree |

### Continue Watching

| Test | State | Assertion |
|---|---|---|
| `continueWatching_withItems_sectionVisible` | 2 items in `continueWatching` | Item names present in tree |
| `continueWatching_empty_sectionAbsent` | Empty list | Item names not found |

### Next Up

State key: `recentlyAddedByTypes[BaseItemKind.EPISODE.name]`

| Test | State | Assertion |
|---|---|---|
| `nextUp_withEpisodes_sectionVisible` | 1 episode | Episode name visible |
| `nextUp_empty_sectionAbsent` | Empty | Episode name absent |

### Libraries

Rendered by `LibraryNavigationCarousel` → `LibraryExpressiveCard`. The `item` block in `MobileExpressiveHomeContent` is always emitted, but `LibraryNavigationCarousel` has an early `if (libraries.isEmpty()) return`. Library names appear as `Text` inside `LibraryExpressiveCard` (line 762 of `ImmersiveHomeScreen.kt`).

| Test | State | Assertion |
|---|---|---|
| `libraries_withItems_carouselVisible` | 2 libraries in `appState.libraries` | Library names visible via `onNodeWithText` |
| `libraries_empty_carouselAbsent` | Empty `appState.libraries` | No `Text` nodes matching library names in tree |

### Viewing Mood Widget

The condition in `MobileExpressiveHomeContent` is `!viewingMood.isNullOrBlank()`. **`viewingMood` is a standalone parameter — pass it directly to `MobileExpressiveHomeContent`, not via `appState`.**

| Test | `viewingMood` argument | Assertion |
|---|---|---|
| `viewingMoodWidget_whenMoodSet_isVisible` | `"Action mood"` | Text "Action mood" displayed |
| `viewingMoodWidget_whenMoodNull_isHidden` | `null` | Text absent |
| `viewingMoodWidget_whenMoodEmpty_isHidden` | `""` | Text absent (blank check) |

### Interactions

| Test | Action | Assertion |
|---|---|---|
| `itemClick_firesWithCorrectItem` | Click first hero carousel item (movie) | `onItemClick` called with that movie item |
| `emptyState_noMoviesNoLibraries_doesNotCrash` | Render with all-empty `MainAppState` | No exception, composeTestRule idle |

---

## Test Data Strategy

- `BaseItemDto` mocks: `mockk<BaseItemDto>(relaxed = true)` with stubs for `id`, `name`, `type`, `dateCreated`
- Map keys are `BaseItemKind.MOVIE.name`, `BaseItemKind.EPISODE.name`, etc. — never bare string literals
- Image lambdas always return `null` in UI tests (avoids Coil network calls)
- `MainAppState` constructed directly via data class constructor
- `onItemClick`, `onLibraryClick`, etc. are `mockk(relaxed = true)` lambdas verified with `verify { ... }`

---

## What Is Not Tested Here

- Tablet bento grid layout (`ExpressiveBentoGrid`) — separate concern
- Immersive version of `itemSubtitle` (in `ImmersiveMediaRow.kt`) — separate unit test file if needed
- `ImmersiveScaffold` overlay FABs and animations — flaky and low value
- Actual image loading — covered by Coil's own tests
- `MainAppViewModel` data loading — covered in existing `MainAppViewModelLibraryLoadTest`, `MainAppViewModelDeleteItemTest`
