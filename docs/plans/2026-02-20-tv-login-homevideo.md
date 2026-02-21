# TV Login + Home Video Screens Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix TV login screen branding, add a Videos nav route, fix home video library loading, and adapt the detail screen for video items.

**Architecture:** Four independent surgical changes across six files. Login screen is extracted from TvJellyfinApp.kt into its own file and rebranded. Home video loading is fixed by passing the real library object to loadLibraryTypeData. A Videos drawer entry is added with its own nav route. The detail screen detects VIDEO items and shows a landscape card.

**Tech Stack:** Jetpack Compose, androidx.tv:tv-material3, Hilt, Kotlin Coroutines, MockK

---

### Background Reading

Before implementing, read these files to understand the current state:

- `app/src/main/java/com/rpeters/jellyfin/ui/tv/TvJellyfinApp.kt` — contains `TvServerConnectionScreen` composable that needs to be extracted (lines 161–410)
- `app/src/main/java/com/rpeters/jellyfin/ui/tv/TvNavigationItem.kt` — sealed class of drawer items
- `app/src/main/java/com/rpeters/jellyfin/ui/tv/TvNavGraph.kt` — all TV routes and their composables
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvLibraryScreen.kt` — library browser with the loading bug
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvItemDetailScreen.kt` — detail screen to adapt for VIDEO items
- `app/src/main/java/com/rpeters/jellyfin/ui/components/tv/TvImmersiveBackground.kt` — blurred backdrop component used as TV dark background
- `app/src/test/java/com/rpeters/jellyfin/ui/viewmodel/MainAppViewModelLibraryLoadTest.kt` — reference for correct test setup patterns (relaxed mocks, `every` for properties)

Key patterns:
- `TvText`, `TvButton`, `TvMaterialTheme` are from `androidx.tv.material3` (aliased in imports)
- `TvImmersiveBackground(backdropUrl = null)` provides a full-screen dark background with no image
- `mockk(relaxed = true)` for repository mocks, `every {}` (not `coEvery`) for non-suspend Flow properties
- `loadLibraryTypeData(library: BaseItemDto, libraryType: LibraryType)` at line 693 of MainAppViewModel.kt is the overload that takes a real library object

---

### Task 1: Extract and Brand TvServerConnectionScreen

**Files:**
- Create: `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvServerConnectionScreen.kt`
- Modify: `app/src/main/java/com/rpeters/jellyfin/ui/tv/TvJellyfinApp.kt`

**What to do:** Move the `TvServerConnectionScreen` composable from `TvJellyfinApp.kt` into its own file, then brand it:
- Replace `"Jellyfin"` title with `"Cinefin"` using `TvMaterialTheme.colorScheme.primary` color
- Wrap content in `TvImmersiveBackground(backdropUrl = null)` instead of the plain Box background
- Replace M3 `Text` with `TvText` (from `androidx.tv.material3`)
- Replace M3 `Button` with `TvButton` (from `androidx.tv.material3`)
- Keep `OutlinedTextField` from M3 — TV Material has no equivalent and M3 handles D-pad correctly
- Keep all existing logic: FocusRequester chain, auto-focus LaunchedEffect, connecting state, error card, Quick Connect tip

**Step 1: Create the new file**

Create `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvServerConnectionScreen.kt` with this content:

```kotlin
package com.rpeters.jellyfin.ui.screens.tv

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rpeters.jellyfin.ui.components.ExpressiveCircularLoading
import com.rpeters.jellyfin.ui.components.tv.TvImmersiveBackground
import androidx.tv.material3.Button as TvButton
import androidx.tv.material3.Card as TvCard
import androidx.tv.material3.CardDefaults as TvCardDefaults
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.Text as TvText

@Composable
fun TvServerConnectionScreen(
    onConnect: (String, String, String) -> Unit,
    onQuickConnect: () -> Unit,
    isConnecting: Boolean,
    errorMessage: String?,
    savedServerUrl: String?,
    savedUsername: String?,
    modifier: Modifier = Modifier,
) {
    var serverUrl by remember { mutableStateOf(savedServerUrl ?: "") }
    var username by remember { mutableStateOf(savedUsername ?: "") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val serverUrlFocusRequester = remember { FocusRequester() }
    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val connectButtonFocusRequester = remember { FocusRequester() }
    val quickConnectButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(savedServerUrl, savedUsername) {
        serverUrl = savedServerUrl ?: ""
        username = savedUsername ?: ""
    }

    LaunchedEffect(Unit) {
        when {
            serverUrl.isEmpty() -> serverUrlFocusRequester.requestFocus()
            username.isEmpty() -> usernameFocusRequester.requestFocus()
            else -> passwordFocusRequester.requestFocus()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        TvImmersiveBackground(backdropUrl = null)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .wrapContentHeight()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TvText(
                    text = "Cinefin",
                    style = TvMaterialTheme.typography.displayMedium,
                    color = TvMaterialTheme.colorScheme.primary,
                )

                TvText(
                    text = "Connect to your Jellyfin server",
                    style = TvMaterialTheme.typography.bodyLarge,
                    color = TvMaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { TvText("Server URL") },
                    placeholder = { TvText("https://jellyfin.example.com") },
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { usernameFocusRequester.requestFocus() },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .focusRequester(serverUrlFocusRequester),
                    enabled = !isConnecting,
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { TvText("Username") },
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { passwordFocusRequester.requestFocus() },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .focusRequester(usernameFocusRequester),
                    enabled = !isConnecting,
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { TvText("Password") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (showPassword) "Hide password" else "Show password",
                            )
                        }
                    },
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { connectButtonFocusRequester.requestFocus() },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .focusRequester(passwordFocusRequester),
                    enabled = !isConnecting,
                )

                if (errorMessage != null) {
                    TvCard(
                        onClick = { /* No-op */ },
                        colors = TvCardDefaults.colors(
                            containerColor = TvMaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TvText(
                            text = errorMessage,
                            color = TvMaterialTheme.colorScheme.error,
                            style = TvMaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                ) {
                    TvButton(
                        onClick = {
                            Log.d("TvServerConnectionScreen", "Connect button clicked")
                            if (serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                                onConnect(serverUrl, username, password)
                            } else {
                                Log.w("TvServerConnectionScreen", "Cannot connect - missing credentials")
                            }
                        },
                        enabled = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank() && !isConnecting,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .focusRequester(connectButtonFocusRequester),
                    ) {
                        if (isConnecting) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ExpressiveCircularLoading(
                                    size = 20.dp,
                                    color = TvMaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TvText("Connecting...", style = TvMaterialTheme.typography.labelLarge)
                            }
                        } else {
                            TvText("Sign In", style = TvMaterialTheme.typography.labelLarge)
                        }
                    }

                    TvButton(
                        onClick = {
                            Log.d("TvServerConnectionScreen", "Quick Connect button clicked")
                            onQuickConnect()
                        },
                        enabled = !isConnecting,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .focusRequester(quickConnectButtonFocusRequester),
                    ) {
                        TvText("Quick Connect", style = TvMaterialTheme.typography.labelLarge)
                    }
                }

                TvText(
                    text = "Tip: Use Quick Connect to sign in without typing your password on TV",
                    style = TvMaterialTheme.typography.bodySmall,
                    color = TvMaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }
        }
    }
}
```

**Step 2: Compile to verify the new file**

```bash
./gradlew compileDebugKotlin 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

**Step 3: Remove the old composable from TvJellyfinApp.kt**

Delete everything from `@Composable fun TvServerConnectionScreen(` through the end of the file (the function closing brace `}`). Also remove these now-unused imports:
- `android.util.Log`
- `androidx.compose.foundation.rememberScrollState`
- `androidx.compose.foundation.text.KeyboardActions`
- `androidx.compose.foundation.text.KeyboardOptions`
- `androidx.compose.foundation.verticalScroll`
- `androidx.compose.material.icons.Icons`
- `androidx.compose.material.icons.filled.Visibility`
- `androidx.compose.material.icons.filled.VisibilityOff`
- `androidx.compose.material3.Icon`
- `androidx.compose.material3.IconButton`
- `androidx.compose.material3.OutlinedTextField`
- `androidx.compose.ui.focus.FocusRequester`
- `androidx.compose.ui.focus.focusRequester`
- `androidx.compose.ui.platform.LocalFocusManager`
- `androidx.compose.ui.text.input.ImeAction`
- `androidx.compose.ui.text.input.KeyboardType`
- `androidx.compose.ui.text.input.PasswordVisualTransformation`
- `androidx.compose.ui.text.input.VisualTransformation`
- `androidx.compose.ui.text.style.TextAlign`
- `com.rpeters.jellyfin.ui.components.ExpressiveCircularLoading`
- `androidx.tv.material3.Button` (keep other tv.material3 imports used by TvMainScreen)

Add this import to `TvJellyfinApp.kt`:
```kotlin
import com.rpeters.jellyfin.ui.screens.tv.TvServerConnectionScreen
```

**Step 4: Compile again**

```bash
./gradlew compileDebugKotlin 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

**Step 5: Commit**

```bash
git add \
  app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvServerConnectionScreen.kt \
  app/src/main/java/com/rpeters/jellyfin/ui/tv/TvJellyfinApp.kt
git commit -m "feat: extract and brand TV login screen with Cinefin styling"
```

---

### Task 2: Add Videos Nav Route

**Files:**
- Modify: `app/src/main/java/com/rpeters/jellyfin/ui/tv/TvNavigationItem.kt`
- Modify: `app/src/main/java/com/rpeters/jellyfin/ui/tv/TvNavGraph.kt`

No unit test needed — nav registration is verified by compile + the fact that TvNavigationItem.items drives the drawer.

**Step 1: Add Videos to TvNavigationItem.kt**

In `TvNavigationItem.kt`, add `Videos` between `Music` and `Search`:

```kotlin
object Videos : TvNavigationItem("tv_homevideos", "Videos", Icons.Default.VideoLibrary)
```

Update the `items` companion list:

```kotlin
val items = listOf(Home, Movies, TvShows, Music, Videos, Search, Favorites, Settings)
```

`Icons.Default.VideoLibrary` is already imported in that file.

**Step 2: Add HomeVideos route to TvNavGraph.kt**

In `TvNavGraph.kt`, add `HomeVideos` to the `TvRoutes` private object:

```kotlin
const val HomeVideos = "tv_homevideos"
```

Add an import for `TvLibraryScreen` (already present) and add the composable after the `Music` composable:

```kotlin
composable(TvRoutes.HomeVideos) {
    TvLibraryScreen(
        libraryId = "homevideos",
        onItemSelect = { itemId -> navController.navigate("tv_item/$itemId") },
    )
}
```

**Step 3: Compile**

```bash
./gradlew compileDebugKotlin 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

**Step 4: Commit**

```bash
git add \
  app/src/main/java/com/rpeters/jellyfin/ui/tv/TvNavigationItem.kt \
  app/src/main/java/com/rpeters/jellyfin/ui/tv/TvNavGraph.kt
git commit -m "feat: add Videos nav item and route for home video libraries"
```

---

### Task 3: Fix Home Video Library Loading

**Files:**
- Modify: `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvLibraryScreen.kt`
- Test: `app/src/test/java/com/rpeters/jellyfin/ui/viewmodel/MainAppViewModelHomeVideoLoadTest.kt`

**Root cause reminder**: `TvLibraryScreen` calls `viewModel.loadHomeVideos(lib.id.toString())` which creates a synthetic `BaseItemDto` with `collectionType = null`. The real library's `collectionType` (e.g. `HOMEVIDEOS`) is lost. The `loadLibraryTypeData` function uses `collectionType` to decide whether to pass `itemTypes = null` to the API — with `null` collectionType, it sends `itemTypes = "Book,AudioBook,Video"` instead, causing Jellyfin to return 0 items.

**Step 1: Write the failing test**

Create `app/src/test/java/com/rpeters/jellyfin/ui/viewmodel/MainAppViewModelHomeVideoLoadTest.kt`:

```kotlin
package com.rpeters.jellyfin.ui.viewmodel

import android.content.Context
import com.rpeters.jellyfin.data.SecureCredentialManager
import com.rpeters.jellyfin.data.common.TestDispatcherProvider
import com.rpeters.jellyfin.data.repository.JellyfinAuthRepository
import com.rpeters.jellyfin.data.repository.JellyfinMediaRepository
import com.rpeters.jellyfin.data.repository.JellyfinRepository
import com.rpeters.jellyfin.data.repository.JellyfinSearchRepository
import com.rpeters.jellyfin.data.repository.JellyfinStreamRepository
import com.rpeters.jellyfin.data.repository.JellyfinUserRepository
import com.rpeters.jellyfin.data.repository.common.ApiResult
import com.rpeters.jellyfin.ui.player.CastManager
import com.rpeters.jellyfin.ui.screens.LibraryType
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Verifies that loadLibraryTypeData called with a HOMEVIDEOS library
 * issues the API call with itemTypes=null (no item type filter), which
 * is what Jellyfin needs to return all home video items.
 *
 * This is the ViewModel-level contract that TvLibraryScreen's fix relies on.
 */
@OptIn(ExperimentalCoroutinesApi::class, androidx.media3.common.util.UnstableApi::class)
class MainAppViewModelHomeVideoLoadTest {

    private lateinit var repository: JellyfinRepository

    @MockK
    private lateinit var authRepository: JellyfinAuthRepository

    @MockK
    private lateinit var mediaRepository: JellyfinMediaRepository

    @MockK
    private lateinit var userRepository: JellyfinUserRepository

    @MockK
    private lateinit var streamRepository: JellyfinStreamRepository

    @MockK
    private lateinit var searchRepository: JellyfinSearchRepository

    @MockK
    private lateinit var credentialManager: SecureCredentialManager

    @MockK
    private lateinit var castManager: CastManager

    @MockK
    private lateinit var generativeAiRepository: com.rpeters.jellyfin.data.repository.GenerativeAiRepository

    @MockK
    private lateinit var analyticsHelper: com.rpeters.jellyfin.utils.AnalyticsHelper

    @MockK
    private lateinit var context: Context

    private lateinit var viewModel: MainAppViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true)
        every { repository.currentServer } returns MutableStateFlow(null)
        every { repository.isConnected } returns MutableStateFlow(false)

        viewModel = MainAppViewModel(
            context = context,
            repository = repository,
            authRepository = authRepository,
            mediaRepository = mediaRepository,
            userRepository = userRepository,
            streamRepository = streamRepository,
            searchRepository = searchRepository,
            credentialManager = credentialManager,
            castManager = castManager,
            dispatchers = TestDispatcherProvider(testDispatcher),
            generativeAiRepository = generativeAiRepository,
            analytics = analyticsHelper,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadLibraryTypeData with HOMEVIDEOS library calls API with null itemTypes`() = runTest(testDispatcher) {
        // Given - a real home video library with HOMEVIDEOS collectionType
        val libraryId = UUID.randomUUID()
        val homeVideoLibrary = BaseItemDto(
            id = libraryId,
            name = "Home Videos",
            type = BaseItemKind.COLLECTION_FOLDER,
            collectionType = CollectionType.HOMEVIDEOS,
        )
        coEvery {
            mediaRepository.getLibraryItems(
                parentId = libraryId.toString(),
                itemTypes = null,         // HOMEVIDEOS must NOT filter by item type
                collectionType = "homevideos",
            )
        } returns ApiResult.Success(emptyList())

        // When
        viewModel.loadLibraryTypeData(homeVideoLibrary, LibraryType.STUFF)
        advanceUntilIdle()

        // Then - API was called with itemTypes=null (the HOMEVIDEOS-specific path)
        coVerify(exactly = 1) {
            mediaRepository.getLibraryItems(
                parentId = libraryId.toString(),
                itemTypes = null,
                collectionType = "homevideos",
            )
        }
    }

    @Test
    fun `loadLibraryTypeData with null-collectionType library uses item type filter`() = runTest(testDispatcher) {
        // Given - a library with null collectionType (some Jellyfin servers return this)
        val libraryId = UUID.randomUUID()
        val unknownLibrary = BaseItemDto(
            id = libraryId,
            name = "Unknown Library",
            type = BaseItemKind.COLLECTION_FOLDER,
            collectionType = null,
        )
        coEvery {
            mediaRepository.getLibraryItems(
                parentId = libraryId.toString(),
                itemTypes = any(),
                collectionType = null,
            )
        } returns ApiResult.Success(emptyList())

        // When
        viewModel.loadLibraryTypeData(unknownLibrary, LibraryType.STUFF)
        advanceUntilIdle()

        // Then - API was called with itemTypes filter (not null)
        coVerify(exactly = 1) {
            mediaRepository.getLibraryItems(
                parentId = libraryId.toString(),
                itemTypes = "Book,AudioBook,Video",
                collectionType = null,
            )
        }
    }
}
```

**Step 2: Run test to verify it passes (ViewModel logic is already correct)**

```bash
./gradlew testDebugUnitTest --tests "com.rpeters.jellyfin.ui.viewmodel.MainAppViewModelHomeVideoLoadTest" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL` — these tests validate the ViewModel contract that the composable fix depends on.

**Step 3: Fix TvLibraryScreen.kt**

Make three edits in `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvLibraryScreen.kt`:

**Edit A** — Add `LibraryType` import (add after existing imports):
```kotlin
import com.rpeters.jellyfin.ui.screens.LibraryType
```

**Edit B** — Extend the library fallback lookup (around line 55). Add `"homevideos"` case:

Change the `when (libraryId)` block from:
```kotlin
val library = appState.libraries.firstOrNull { it.id.toString() == libraryId }
    ?: when (libraryId) {
        "movies"  -> appState.libraries.firstOrNull {
            it.collectionType == CollectionType.MOVIES
        }
        "tvshows" -> appState.libraries.firstOrNull {
            it.collectionType == CollectionType.TVSHOWS
        }
        "music"   -> appState.libraries.firstOrNull {
            it.collectionType == CollectionType.MUSIC
        }
        else -> null
    }
```

To:
```kotlin
val library = appState.libraries.firstOrNull { it.id.toString() == libraryId }
    ?: when (libraryId) {
        "movies"     -> appState.libraries.firstOrNull {
            it.collectionType == CollectionType.MOVIES
        }
        "tvshows"    -> appState.libraries.firstOrNull {
            it.collectionType == CollectionType.TVSHOWS
        }
        "music"      -> appState.libraries.firstOrNull {
            it.collectionType == CollectionType.MUSIC
        }
        "homevideos" -> appState.libraries.firstOrNull {
            it.collectionType == CollectionType.HOMEVIDEOS
        } ?: appState.libraries.firstOrNull { it.collectionType == null }
        else -> null
    }
```

**Edit C** — Fix the loading trigger `LaunchedEffect` (around line 100). Change:
```kotlin
library?.let { lib ->
    when (lib.collectionType) {
        CollectionType.MOVIES -> viewModel.loadLibraryTypeData(lib, LibraryType.MOVIES)
        CollectionType.TVSHOWS -> viewModel.loadLibraryTypeData(lib, LibraryType.TV_SHOWS)
        CollectionType.MUSIC -> viewModel.loadLibraryTypeData(lib, LibraryType.MUSIC)
        else -> viewModel.loadHomeVideos(lib.id.toString())
    }
}
```

To:
```kotlin
library?.let { lib ->
    when (lib.collectionType) {
        CollectionType.MOVIES -> viewModel.loadLibraryTypeData(lib, LibraryType.MOVIES)
        CollectionType.TVSHOWS -> viewModel.loadLibraryTypeData(lib, LibraryType.TV_SHOWS)
        CollectionType.MUSIC -> viewModel.loadLibraryTypeData(lib, LibraryType.MUSIC)
        CollectionType.HOMEVIDEOS -> viewModel.loadLibraryTypeData(lib, LibraryType.STUFF)
        else -> viewModel.loadLibraryTypeData(lib, LibraryType.STUFF)
    }
}
```

**Step 4: Compile**

```bash
./gradlew compileDebugKotlin 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

**Step 5: Run tests**

```bash
./gradlew testDebugUnitTest --tests "com.rpeters.jellyfin.ui.viewmodel.MainAppViewModelHomeVideoLoadTest" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

**Step 6: Commit**

```bash
git add \
  app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvLibraryScreen.kt \
  app/src/test/java/com/rpeters/jellyfin/ui/viewmodel/MainAppViewModelHomeVideoLoadTest.kt
git commit -m "fix: resolve home video library loading by passing real library to loadLibraryTypeData"
```

---

### Task 4: Adapt Detail Screen for Video Items

**Files:**
- Modify: `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvItemDetailScreen.kt`

The detail screen already correctly hides the seasons/episodes section for non-SERIES items (guarded by `if (item?.type == BaseItemKind.SERIES && ...)`). The only thing wrong for home videos is the portrait poster card — home videos have landscape backdrops, not portrait posters.

**What to change:** Detect `val isVideo = item?.type == BaseItemKind.VIDEO`. When true, replace the portrait `TvCard` (260×390dp) with a landscape backdrop card (390×220dp, 16:9).

**Step 1: Add the isVideo detection and landscape card**

In `TvItemDetailScreen.kt`, find the section that starts with:
```kotlin
// Poster Card
TvCard(
    onClick = {},
    modifier = Modifier.size(260.dp, 390.dp),
```

Replace the entire `TvCard` block (the poster card — from `// Poster Card` comment through its closing `}`) with:

```kotlin
val isVideo = item?.type == BaseItemKind.VIDEO

// Poster / Backdrop Card — landscape for video items, portrait for movies/series
val cardWidth = if (isVideo) 390.dp else 260.dp
val cardHeight = if (isVideo) 220.dp else 390.dp
val cardImageUrl = if (isVideo) {
    item?.let { viewModel.getBackdropUrl(it) } ?: item?.let { viewModel.getImageUrl(it) }
} else {
    item?.let { viewModel.getImageUrl(it) }
}

TvCard(
    onClick = {},
    modifier = Modifier.size(cardWidth, cardHeight),
    scale = TvCardDefaults.scale(focusedScale = 1.05f),
    colors = TvCardDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.DarkGray),
) {
    JellyfinAsyncImage(
        model = cardImageUrl,
        contentDescription = item?.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
        requestSize = rememberCoilSize(cardWidth, cardHeight),
    )
}
```

**Step 2: Compile**

```bash
./gradlew compileDebugKotlin 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

**Step 3: Run full test suite to check for regressions**

```bash
./gradlew testDebugUnitTest --tests "com.rpeters.jellyfin.ui.viewmodel.*" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

**Step 4: Commit**

```bash
git add app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvItemDetailScreen.kt
git commit -m "fix: show landscape backdrop card for home video items in TV detail screen"
```

---

### Final Verification

```bash
./gradlew compileDebugKotlin 2>&1 | tail -5
./gradlew testDebugUnitTest --tests "com.rpeters.jellyfin.ui.viewmodel.MainAppViewModelHomeVideoLoadTest" 2>&1 | tail -5
./gradlew testDebugUnitTest --tests "com.rpeters.jellyfin.ui.viewmodel.MainAppViewModelLoadItemTest" 2>&1 | tail -5
```

All expected: `BUILD SUCCESSFUL`
