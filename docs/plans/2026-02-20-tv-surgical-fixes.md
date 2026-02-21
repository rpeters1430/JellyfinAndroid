# TV Surgical Fixes Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix six specific bugs in the Android TV UI that prevent it from working correctly in production.

**Architecture:** Each fix is self-contained — no shared state between tasks. Fixes progress from infrastructure (version/colors) to data layer (repository/ViewModel) to UI. The navigation graph (`TvNavGraph.kt`) is touched last to wire everything together cleanly.

**Tech Stack:** Kotlin, Jetpack Compose, `androidx.tv:tv-material`, Hilt, Jetpack Navigation, Kotlin Coroutines, MockK, JUnit4

**Design doc:** `docs/plans/2026-02-20-tv-surgical-fixes-design.md`

---

## Background: Key File Locations

Before starting, know these files:
- `gradle/libs.versions.toml` — all dependency versions live here
- `app/src/main/java/com/rpeters/jellyfin/ui/theme/Color.kt` — brand color constants (`JellyfinPurple80`, etc.)
- `app/src/main/java/com/rpeters/jellyfin/ui/theme/TvTheme.kt` — TV design tokens
- `app/src/main/java/com/rpeters/jellyfin/ui/tv/TvJellyfinApp.kt` — TV root composable
- `app/src/main/java/com/rpeters/jellyfin/ui/tv/TvNavGraph.kt` — all TV routes
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvHomeScreen.kt` — home screen
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvLibraryScreen.kt` — library browser
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvItemDetailScreen.kt` — detail screen
- `app/src/main/java/com/rpeters/jellyfin/ui/components/tv/TvHeroCarousel.kt` — featured carousel
- `app/src/main/java/com/rpeters/jellyfin/data/repository/JellyfinRepository.kt` — all API calls
- `app/src/main/java/com/rpeters/jellyfin/ui/viewmodel/MainAppViewModel.kt` — main ViewModel
- `app/src/test/java/com/rpeters/jellyfin/` — unit tests

---

## Task 1: Upgrade `androidx.tv:tv-material` Version

**Files:**
- Modify: `gradle/libs.versions.toml`

This must be first — it may unlock API fixes used by later tasks.

**Step 1: Check the current version**

Open `gradle/libs.versions.toml` and find:
```toml
androidxTvMaterial = "1.1.0-alpha01"
```

**Step 2: Find the latest published version**

Check https://maven.google.com/web/index.html#androidx.tv:tv-material or run:
```bash
curl -s "https://dl.google.com/dl/android/maven2/androidx/tv/tv-material/maven-metadata.xml" | grep -o '<version>[^<]*</version>' | tail -5
```

**Step 3: Update the version**

In `gradle/libs.versions.toml`, replace with the latest version you found:
```toml
androidxTvMaterial = "<latest-version>"
```

**Step 4: Verify build compiles**

```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL. If you see compilation errors involving `@OptIn(ExperimentalTvMaterial3Api::class)`, check if an `@Deprecated` annotation needs to be removed — the opt-in annotations are already applied globally in `app/build.gradle.kts`.

**Step 5: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "chore: upgrade androidx.tv:tv-material to latest"
```

---

## Task 2: Apply Cinefin Brand Colors to TV Theme

**Files:**
- Modify: `app/src/main/java/com/rpeters/jellyfin/ui/theme/TvTheme.kt`
- Modify: `app/src/main/java/com/rpeters/jellyfin/ui/tv/TvJellyfinApp.kt`

The existing `TvMaterialTheme { }` in `TvJellyfinApp.kt` uses the default TV dark color scheme (grey/white). We need to inject Cinefin's purple/blue/teal brand. The color constants already exist in `Color.kt` — `JellyfinPurple80`, `JellyfinBlue80`, `JellyfinTeal80` are the bright variants suited to dark backgrounds.

**Step 1: Add `cinefinTvColorScheme()` to `TvTheme.kt`**

Open `app/src/main/java/com/rpeters/jellyfin/ui/theme/TvTheme.kt`. Add this import block and function after the existing content:

```kotlin
import androidx.tv.material3.ColorScheme as TvColorScheme
import com.rpeters.jellyfin.ui.theme.JellyfinPurple80
import com.rpeters.jellyfin.ui.theme.JellyfinPurple30
import com.rpeters.jellyfin.ui.theme.JellyfinBlue80
import com.rpeters.jellyfin.ui.theme.JellyfinBlue30
import com.rpeters.jellyfin.ui.theme.JellyfinTeal80
import com.rpeters.jellyfin.ui.theme.JellyfinTeal30
import com.rpeters.jellyfin.ui.theme.Neutral10
import com.rpeters.jellyfin.ui.theme.Neutral20
import com.rpeters.jellyfin.ui.theme.Neutral30
import com.rpeters.jellyfin.ui.theme.Neutral40

/**
 * Returns a TV Material 3 color scheme using Cinefin brand colors.
 * TV is always displayed in dark mode (lean-back environment).
 */
fun cinefinTvColorScheme(): TvColorScheme = androidx.tv.material3.darkColorScheme(
    primary = JellyfinPurple80,
    onPrimary = Color(0xFF000000),
    primaryContainer = JellyfinPurple30,
    onPrimaryContainer = JellyfinPurple80,
    secondary = JellyfinBlue80,
    onSecondary = Color(0xFF000000),
    secondaryContainer = JellyfinBlue30,
    onSecondaryContainer = JellyfinBlue80,
    tertiary = JellyfinTeal80,
    onTertiary = Color(0xFF000000),
    tertiaryContainer = JellyfinTeal30,
    onTertiaryContainer = JellyfinTeal80,
    background = Neutral10,
    onBackground = Color(0xFFE6E1E6),
    surface = Neutral10,
    onSurface = Color(0xFFE6E1E6),
    surfaceVariant = Neutral20,
    onSurfaceVariant = Color(0xFFCAC5CA),
    border = Neutral30,
    borderVariant = Neutral40,
    error = Color(0xFFCF6679),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)
```

Note: You also need `import androidx.compose.ui.graphics.Color` at the top of the file (add it if not already present).

**Step 2: Apply the color scheme in `TvJellyfinApp.kt`**

Open `app/src/main/java/com/rpeters/jellyfin/ui/tv/TvJellyfinApp.kt`. Find the line:
```kotlin
TvMaterialTheme {
```

Replace with:
```kotlin
TvMaterialTheme(colorScheme = cinefinTvColorScheme()) {
```

Add the import at the top:
```kotlin
import com.rpeters.jellyfin.ui.theme.cinefinTvColorScheme
```

**Step 3: Verify build**

```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL. If you see unresolved reference errors for color names, check that `Neutral10`, `Neutral20`, etc. are exported (not `private`) in `Color.kt`.

**Step 4: Commit**

```bash
git add app/src/main/java/com/rpeters/jellyfin/ui/theme/TvTheme.kt \
        app/src/main/java/com/rpeters/jellyfin/ui/tv/TvJellyfinApp.kt
git commit -m "feat: apply Cinefin brand colors to TV Material theme"
```

---

## Task 3: Wire Hero Carousel Play Button

**Files:**
- Modify: `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvHomeScreen.kt`
- Modify: `app/src/main/java/com/rpeters/jellyfin/ui/tv/TvNavGraph.kt`

The Play button in `TvHeroCarousel` calls `onPlayClick(item)`, but `TvHomeScreen` passes a no-op lambda. We add an `onPlay` parameter to `TvHomeScreen` and wire it through.

**Step 1: Add `onPlay` param to `TvHomeScreen`**

Open `TvHomeScreen.kt`. Find:
```kotlin
@Composable
fun TvHomeScreen(
    onItemSelect: (String) -> Unit,
    onLibrarySelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainAppViewModel = hiltViewModel(),
) {
```

Replace with:
```kotlin
@Composable
fun TvHomeScreen(
    onItemSelect: (String) -> Unit,
    onLibrarySelect: (String) -> Unit,
    onPlay: (itemId: String, itemName: String, startMs: Long) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: MainAppViewModel = hiltViewModel(),
) {
```

**Step 2: Wire `onPlay` into both carousel header lambdas**

In `TvHomeScreen`, there are two calls to `TvHeroCarousel` — one inside `TabletHomeContent`'s `header =` lambda and one inside `TvCarouselHomeContent`'s `header =` lambda. Both currently have:
```kotlin
onPlayClick = { item -> /* Navigate to player */ },
```

Replace both occurrences with:
```kotlin
onPlayClick = { item ->
    onPlay(item.id.toString(), item.name ?: "", 0L)
},
```

**Step 3: Pass `onPlay` from `TvNavGraph`**

Open `TvNavGraph.kt`. Find the `TvRoutes.Home` composable block:
```kotlin
composable(TvRoutes.Home) {
    TvHomeScreen(
        onItemSelect = { itemId ->
            navController.navigate("tv_item/$itemId")
        },
        onLibrarySelect = { libraryId ->
            navController.navigate("tv_library/$libraryId")
        },
    )
}
```

Replace with:
```kotlin
composable(TvRoutes.Home) {
    TvHomeScreen(
        onItemSelect = { itemId ->
            navController.navigate("tv_item/$itemId")
        },
        onLibrarySelect = { libraryId ->
            navController.navigate("tv_library/$libraryId")
        },
        onPlay = { itemId, itemName, startMs ->
            navController.navigate(TvRoutes.playerRoute(itemId, itemName, startMs))
        },
    )
}
```

**Step 4: Verify build**

```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL. No parameter count errors.

**Step 5: Commit**

```bash
git add app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvHomeScreen.kt \
        app/src/main/java/com/rpeters/jellyfin/ui/tv/TvNavGraph.kt
git commit -m "fix: wire hero carousel Play button to TV player navigation"
```

---

## Task 4: Fix Movies / TV Shows / Music / Favorites Library Routes

**Files:**
- Modify: `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvLibraryScreen.kt`

The nav routes for Movies, TV Shows, and Music pass synthetic IDs (`"movies"`, `"tvshows"`, `"music"`) that don't match real server UUIDs. We add a fallback lookup by collection type. Favorites gets a dedicated loading path since `loadFavorites()` and `appState.favorites` already exist in the ViewModel.

**Step 1: Add collection-type fallback to library lookup**

Open `TvLibraryScreen.kt`. Find this block (near the top of the composable):
```kotlin
val library = appState.libraries.firstOrNull { it.id.toString() == libraryId }
```

Replace with:
```kotlin
val library = appState.libraries.firstOrNull { it.id.toString() == libraryId }
    ?: when (libraryId) {
        "movies"  -> appState.libraries.firstOrNull {
            it.collectionType == org.jellyfin.sdk.model.api.CollectionType.MOVIES
        }
        "tvshows" -> appState.libraries.firstOrNull {
            it.collectionType == org.jellyfin.sdk.model.api.CollectionType.TVSHOWS
        }
        "music"   -> appState.libraries.firstOrNull {
            it.collectionType == org.jellyfin.sdk.model.api.CollectionType.MUSIC
        }
        else -> null
    }
```

**Step 2: Add favorites loading path**

Still in `TvLibraryScreen.kt`, find the existing `LaunchedEffect` that triggers item loading. The full current block looks like:
```kotlin
LaunchedEffect(libraryId, library?.collectionType, appState.libraries) {
    if (appState.libraries.isEmpty()) return@LaunchedEffect

    library?.let { lib ->
        when (lib.collectionType) {
            ...
        }
    }
}
```

Add a separate `LaunchedEffect` **above** that one, to handle the favorites case:
```kotlin
LaunchedEffect(libraryId) {
    if (libraryId == "favorites") {
        viewModel.loadFavorites()
    }
}
```

**Step 3: Use favorites items when in favorites mode**

Find:
```kotlin
val items = appState.itemsByLibrary[libraryId] ?: emptyList()
```

Replace with:
```kotlin
val items = if (libraryId == "favorites") {
    appState.favorites
} else {
    appState.itemsByLibrary[library?.id?.toString() ?: libraryId] ?: emptyList()
}
```

Note: We also fix the key here — we use `library?.id?.toString()` instead of the synthetic string, so the items loaded by `loadLibraryTypeData` (which keys by real UUID) will be found correctly.

**Step 4: Fix the library title display for synthetic IDs**

Find:
```kotlin
TvText(
    text = library?.name ?: "Library",
    ...
)
```

Replace with:
```kotlin
TvText(
    text = library?.name ?: when (libraryId) {
        "movies"   -> "Movies"
        "tvshows"  -> "TV Shows"
        "music"    -> "Music"
        "favorites" -> "Favorites"
        else       -> "Library"
    },
    ...
)
```

**Step 5: Verify build**

```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL.

**Step 6: Commit**

```bash
git add app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvLibraryScreen.kt
git commit -m "fix: resolve Movies/TV Shows/Music/Favorites routes using collection type fallback"
```

---

## Task 5: Item Detail Screen — Fetch Item by ID

**Files:**
- Modify: `app/src/main/java/com/rpeters/jellyfin/data/repository/JellyfinRepository.kt`
- Modify: `app/src/main/java/com/rpeters/jellyfin/ui/viewmodel/MainAppViewModel.kt`
- Modify: `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvItemDetailScreen.kt`
- Test: `app/src/test/java/com/rpeters/jellyfin/ui/viewmodel/MainAppViewModelLoadItemTest.kt`

The detail screen searches local state only. If the item isn't already loaded, it shows blank. We add a generic `getItemDetails(itemId)` to the repository, a `loadItemById(itemId)` to the ViewModel, and a `LaunchedEffect` in the screen that triggers the fetch when needed.

**Step 1: Expose a public `getItemDetails` in `JellyfinRepository`**

Open `JellyfinRepository.kt`. Find the existing `getSeriesDetails` function (around line 791). Add a new public function immediately after `getEpisodeDetails`:

```kotlin
/**
 * Fetches a single item by ID without requiring knowledge of its type.
 * Used when navigating to a detail screen for an item not already in memory.
 */
suspend fun getItemDetails(itemId: String): ApiResult<BaseItemDto> {
    return getItemDetailsById(itemId, "item")
}
```

This works because `getItemDetailsById` already uses the generic `itemsApi.getItems(ids = listOf(itemUuid))` endpoint — the `itemTypeName` parameter is only used in the exception message string, not in the API call.

**Step 2: Write the failing unit test**

Create the test file `app/src/test/java/com/rpeters/jellyfin/ui/viewmodel/MainAppViewModelLoadItemTest.kt`:

```kotlin
package com.rpeters.jellyfin.ui.viewmodel

import com.rpeters.jellyfin.data.repository.JellyfinRepository
import com.rpeters.jellyfin.data.model.ApiResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import org.jellyfin.sdk.model.api.BaseItemDto
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

class MainAppViewModelLoadItemTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: JellyfinRepository
    private lateinit var viewModel: MainAppViewModel

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        // Note: MainAppViewModel uses Hilt in production but we provide the repo directly in tests.
        // If the constructor requires other dependencies, add relaxed mocks for them too.
        viewModel = MainAppViewModel(repository = repository)
    }

    @Test
    fun loadItemById_success_storesItemInState() = runTest(testDispatcher) {
        val fakeId = UUID.randomUUID().toString()
        val fakeItem = BaseItemDto(id = UUID.fromString(fakeId), name = "Test Movie")

        coEvery { repository.getItemDetails(fakeId) } returns ApiResult.Success(fakeItem)

        viewModel.loadItemById(fakeId)
        advanceUntilIdle()

        assertEquals(fakeItem, viewModel.appState.value.selectedItem)
    }

    @Test
    fun loadItemById_error_doesNotCrash() = runTest(testDispatcher) {
        val fakeId = UUID.randomUUID().toString()
        coEvery { repository.getItemDetails(fakeId) } returns ApiResult.Error(
            type = com.rpeters.jellyfin.data.model.ErrorType.NETWORK,
            message = "Not found",
        )

        viewModel.loadItemById(fakeId)
        advanceUntilIdle()

        assertEquals(null, viewModel.appState.value.selectedItem)
    }
}
```

**Step 3: Run the tests to confirm they fail**

```bash
./gradlew testDebugUnitTest --tests "com.rpeters.jellyfin.ui.viewmodel.MainAppViewModelLoadItemTest"
```
Expected: FAIL — `loadItemById` not found on `MainAppViewModel`, and `selectedItem` not in `AppUiState`. That's expected; we haven't written the code yet.

**Step 4: Add `selectedItem` to `AppUiState`**

Open `MainAppViewModel.kt`. Find the `AppUiState` data class definition (the one with `favorites: List<BaseItemDto>`). Add:

```kotlin
val selectedItem: BaseItemDto? = null,
```

**Step 5: Add `loadItemById` to `MainAppViewModel`**

In `MainAppViewModel.kt`, add this function near `loadFavorites()`:

```kotlin
/**
 * Fetches a single item by ID from the server and stores it in [AppUiState.selectedItem].
 * Called when navigating to a detail screen for an item not already in memory.
 */
fun loadItemById(itemId: String) {
    viewModelScope.launch {
        when (val result = repository.getItemDetails(itemId)) {
            is ApiResult.Success -> {
                _appState.value = _appState.value.copy(selectedItem = result.data)
            }
            is ApiResult.Error -> {
                // Item not found; leave selectedItem null so the screen shows an error state
            }
            is ApiResult.Loading -> { /* no-op */ }
        }
    }
}
```

**Step 6: Run the tests — they should pass now**

```bash
./gradlew testDebugUnitTest --tests "com.rpeters.jellyfin.ui.viewmodel.MainAppViewModelLoadItemTest"
```
Expected: PASS for both tests.

**Step 7: Update item resolution in `TvItemDetailScreen`**

Open `TvItemDetailScreen.kt`. Find the item resolution block:
```kotlin
val item: BaseItemDto? = itemId?.let { id ->
    appState.allMovies.firstOrNull { it.id.toString() == id }
        ?: appState.allTVShows.firstOrNull { it.id.toString() == id }
        ?: appState.recentlyAdded.firstOrNull { it.id.toString() == id }
        ?: appState.itemsByLibrary.values.asSequence().flatten().firstOrNull { it.id.toString() == id }
        ?: seasonState.seriesDetails
}
```

Replace with:
```kotlin
val item: BaseItemDto? = itemId?.let { id ->
    appState.allMovies.firstOrNull { it.id.toString() == id }
        ?: appState.allTVShows.firstOrNull { it.id.toString() == id }
        ?: appState.recentlyAdded.firstOrNull { it.id.toString() == id }
        ?: appState.itemsByLibrary.values.asSequence().flatten().firstOrNull { it.id.toString() == id }
        ?: seasonState.seriesDetails
        ?: appState.selectedItem?.takeIf { it.id.toString() == id }
}
```

**Step 8: Add LaunchedEffect to fetch item when not in memory**

In `TvItemDetailScreen`, after the existing `LaunchedEffect(itemId, item?.type)` block, add:

```kotlin
// If item is not in local state, fetch it from the server
LaunchedEffect(itemId, item) {
    if (item == null && itemId != null) {
        viewModel.loadItemById(itemId)
    }
}
```

**Step 9: Verify build**

```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL.

**Step 10: Commit**

```bash
git add app/src/main/java/com/rpeters/jellyfin/data/repository/JellyfinRepository.kt \
        app/src/main/java/com/rpeters/jellyfin/ui/viewmodel/MainAppViewModel.kt \
        app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvItemDetailScreen.kt \
        app/src/test/java/com/rpeters/jellyfin/ui/viewmodel/MainAppViewModelLoadItemTest.kt
git commit -m "fix: fetch item by ID in TV detail screen when not already in memory"
```

---

## Task 6: TV Settings Screen

**Files:**
- Create: `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvSettingsScreen.kt`
- Modify: `app/src/main/java/com/rpeters/jellyfin/ui/tv/TvNavGraph.kt`

**Step 1: Create `TvSettingsScreen.kt`**

Create the file `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvSettingsScreen.kt`:

```kotlin
package com.rpeters.jellyfin.ui.screens.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.Text as TvText
import com.rpeters.jellyfin.BuildConfig
import com.rpeters.jellyfin.ui.components.tv.TvImmersiveBackground
import com.rpeters.jellyfin.ui.viewmodel.ServerConnectionViewModel

@Composable
fun TvSettingsScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    connectionViewModel: ServerConnectionViewModel = hiltViewModel(),
) {
    val connectionState by connectionViewModel.connectionState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        TvImmersiveBackground(backdropUrl = null)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 56.dp)
                .padding(top = 64.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            TvText(
                text = "Settings",
                style = TvMaterialTheme.typography.displaySmall,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Server info
            TvText(
                text = "Server",
                style = TvMaterialTheme.typography.titleLarge,
                color = TvMaterialTheme.colorScheme.primary,
            )
            TvText(
                text = connectionState.savedServerUrl ?: "Not connected",
                style = TvMaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Account info
            TvText(
                text = "Signed in as",
                style = TvMaterialTheme.typography.titleLarge,
                color = TvMaterialTheme.colorScheme.primary,
            )
            TvText(
                text = connectionState.savedUsername ?: "Unknown",
                style = TvMaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sign Out button
            Button(
                onClick = {
                    connectionViewModel.logout()
                    onSignOut()
                },
                modifier = Modifier.width(200.dp),
            ) {
                TvText("Sign Out")
            }

            Spacer(modifier = Modifier.weight(1f))

            // App version at bottom
            TvText(
                text = "Cinefin v${BuildConfig.VERSION_NAME}",
                style = TvMaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(bottom = 32.dp),
            )
        }
    }
}
```

**Step 2: Wire Settings route in `TvNavGraph.kt`**

Open `TvNavGraph.kt`. Find:
```kotlin
composable(TvRoutes.Settings) {
    // Placeholder for TV Settings Screen
    TvLibraryScreen(
        libraryId = "settings",
        onItemSelect = { itemId -> navController.navigate("tv_item/$itemId") },
    )
}
```

Replace with:
```kotlin
composable(TvRoutes.Settings) {
    TvSettingsScreen(
        onSignOut = {
            navController.navigate(TvRoutes.ServerConnection) {
                popUpTo(0) { inclusive = true }
            }
        },
    )
}
```

Add this import at the top of `TvNavGraph.kt`:
```kotlin
import com.rpeters.jellyfin.ui.screens.tv.TvSettingsScreen
```

**Step 3: Verify build**

```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL.

**Step 4: Commit**

```bash
git add app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvSettingsScreen.kt \
        app/src/main/java/com/rpeters/jellyfin/ui/tv/TvNavGraph.kt
git commit -m "feat: add TV settings screen with server info and sign out"
```

---

## Final Verification

**Step 1: Run all unit tests**

```bash
./gradlew testDebugUnitTest
```
Expected: All tests pass including the new `MainAppViewModelLoadItemTest`.

**Step 2: Run lint**

```bash
./gradlew lintDebug
```
Expected: No new errors. Any pre-existing warnings are acceptable.

**Step 3: Full build**

```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL.

---

## Summary of Changes

| Task | Files Changed | Risk |
|------|--------------|------|
| 1. Upgrade tv-material | `libs.versions.toml` | Low |
| 2. Brand colors | `TvTheme.kt`, `TvJellyfinApp.kt` | Low |
| 3. Hero Play button | `TvHomeScreen.kt`, `TvNavGraph.kt` | Low |
| 4. Library routes | `TvLibraryScreen.kt` | Low |
| 5. Detail screen fetch | `JellyfinRepository.kt`, `MainAppViewModel.kt`, `TvItemDetailScreen.kt` | Medium |
| 6. Settings screen | `TvSettingsScreen.kt` (new), `TvNavGraph.kt` | Low |
