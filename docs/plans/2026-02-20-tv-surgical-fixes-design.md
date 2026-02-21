# TV Surgical Fixes — Design Document

**Date**: 2026-02-20
**Author**: Claude Code + user
**Status**: Approved

---

## Context

The Android TV implementation (`ui/tv/`, `ui/screens/tv/`, `ui/components/tv/`) has good structural bones — `ModalNavigationDrawer`, `TvHeroCarousel` with `androidx.tv.material3.Carousel`, `TvImmersiveBackground` with cross-fade blurred backdrop, `TvContentCard` with scale+glow focus effects, a detail screen, player, library browser, and search. However, six specific bugs prevent it from working correctly.

---

## What We Are Fixing

### Fix 1: Hero Carousel Play Button (currently a no-op)

**Root cause**: `TvHomeScreen` is invoked from `TvNavGraph` with only `onItemSelect` and `onLibrarySelect` callbacks. The `TvHeroCarousel.onPlayClick` lambda is hardcoded as `{ item -> /* Navigate to player */ }`.

**Solution**:
- Add `onPlay: (itemId: String, itemName: String, startMs: Long) -> Unit` parameter to `TvHomeScreen`
- In both `TvCarouselHomeContent` and `TabletHomeContent` header composable lambdas, forward `onPlay` to `TvHeroCarousel.onPlayClick` (converting `BaseItemDto` → play args)
- Update `TvNavGraph` composable block for `TvRoutes.Home` to pass `navController.navigate(TvRoutes.playerRoute(...))`

**Files touched**:
- `ui/screens/tv/TvHomeScreen.kt` — add `onPlay` param, wire through header lambdas
- `ui/tv/TvNavGraph.kt` — pass `onPlay` when invoking `TvHomeScreen`

---

### Fix 2: Movies / TV Shows / Music / Favorites Routes (hardcoded fake IDs)

**Root cause**: `TvNavGraph` routes `TvRoutes.Movies` → `TvLibraryScreen(libraryId = "movies")`. The screen does `appState.libraries.firstOrNull { it.id.toString() == libraryId }` — the string "movies" never matches a real UUID, so `library == null`, `items` stays empty, and loading never triggers.

**Solution**: In `TvLibraryScreen`, extend the library lookup with a collection-type fallback:

```kotlin
val library = appState.libraries.firstOrNull { it.id.toString() == libraryId }
    ?: when (libraryId) {
        "movies"   -> appState.libraries.firstOrNull { it.collectionType == CollectionType.MOVIES }
        "tvshows"  -> appState.libraries.firstOrNull { it.collectionType == CollectionType.TVSHOWS }
        "music"    -> appState.libraries.firstOrNull { it.collectionType == CollectionType.MUSIC }
        else       -> null
    }
```

For **Favorites**: add a special loading path in `TvLibraryScreen`. When `libraryId == "favorites"` and `library == null`, call `viewModel.loadFavorites()` and display `appState.favorites` instead of `appState.itemsByLibrary[libraryId]`.

**Files touched**:
- `ui/screens/tv/TvLibraryScreen.kt` — extend library lookup, add favorites path
- `ui/viewmodel/MainAppViewModel.kt` — add `loadFavorites()` if not already present

---

### Fix 3: Item Detail Screen — Fetch Item by ID

**Root cause**: `TvItemDetailScreen` searches `appState.allMovies`, `appState.allTVShows`, `appState.recentlyAdded`, and `appState.itemsByLibrary` for the item by ID. If the user navigates to an item that hasn't been loaded into state (e.g., from search → detail), `item == null` and the screen renders blank.

**Solution**:
- Add `suspend fun getItemDetails(itemId: String): ApiResult<BaseItemDto>` to `JellyfinRepository` as a public wrapper around the existing private `getItemDetailsById` (try with generic "item" type, or expose the private method as package-internal)
- Add `fun loadItemById(itemId: String)` to `MainAppViewModel` that calls the repository and emits the result into a `selectedItem: BaseItemDto?` field in `AppUiState`
- In `TvItemDetailScreen`, add a `LaunchedEffect(itemId)` that calls `viewModel.loadItemById(itemId)` when `item == null`; the existing item search logic still takes precedence when items are already loaded

**Files touched**:
- `data/repository/JellyfinRepository.kt` — expose `getItemDetails(itemId: String)`
- `ui/viewmodel/MainAppViewModel.kt` (or `SharedAppStateManager`) — add `loadItemById()`
- `ui/screens/tv/TvItemDetailScreen.kt` — add `LaunchedEffect` for on-demand fetch

---

### Fix 4: TV Settings Screen

**Root cause**: `TvNavGraph` routes `TvRoutes.Settings` → `TvLibraryScreen(libraryId = "settings")`. This renders a library browser for a nonexistent library — wrong screen entirely.

**Solution**: Create `TvSettingsScreen` composable in `ui/screens/tv/` and wire it into `TvNavGraph`:

Content:
- Display current server URL and signed-in username (from `ServerConnectionViewModel`)
- "Sign Out" button that calls `connectionViewModel.disconnect()` and navigates back to `TvRoutes.ServerConnection`
- App version string (from `BuildConfig.VERSION_NAME`)

This is intentionally minimal — a full settings screen is a separate workstream.

**Files created**:
- `ui/screens/tv/TvSettingsScreen.kt`

**Files touched**:
- `ui/tv/TvNavGraph.kt` — replace `TvLibraryScreen` with `TvSettingsScreen` for `TvRoutes.Settings`

---

### Fix 5: TV Brand Colors

**Root cause**: `TvJellyfinApp.kt` wraps content in `TvMaterialTheme { }` with no custom color scheme, so all TV components render with the default grey/white TV palette instead of Cinefin's purple/blue/teal.

**Solution**: Extend `TvTheme.kt` with a `cinefinTvColorScheme()` function:

```kotlin
fun cinefinTvColorScheme() = darkColorScheme(
    primary    = Color(0xFF6200EE),  // Jellyfin Purple
    secondary  = Color(0xFF2962FF),  // Jellyfin Blue
    tertiary   = Color(0xFF00695C),  // Jellyfin Teal
    background = Color(0xFF0A0A0A),
    surface    = Color(0xFF121212),
    // ... error, onX colors from existing JellyfinColors
)
```

Apply in `TvJellyfinApp.kt`:
```kotlin
TvMaterialTheme(colorScheme = cinefinTvColorScheme()) { ... }
```

**Files touched**:
- `ui/theme/TvTheme.kt` — add `cinefinTvColorScheme()`
- `ui/tv/TvJellyfinApp.kt` — pass color scheme to `TvMaterialTheme`

---

### Fix 6: Upgrade `androidx.tv:tv-material`

**Root cause**: `libs.versions.toml` pins `androidxTvMaterial = "1.1.0-alpha01"` — very early alpha that predates API stability fixes.

**Solution**: Update to the latest published version in `gradle/libs.versions.toml`. Verify no API breakage by checking `ExperimentalTvMaterial3Api` usage (all callers are already opted-in). Run `./gradlew assembleDebug` to confirm no compilation errors.

**Files touched**:
- `gradle/libs.versions.toml` — bump `androidxTvMaterial`

---

## What We Are NOT Changing

- Navigation architecture (double-NavHost pattern) — deferred; the current approach works for the primary flows
- Search keyboard UI — the system keyboard is acceptable for now
- Continue Watching row — deferred to a later session
- Full TV settings screen — the stub created in Fix 4 is intentionally minimal

---

## Files Modified Summary

| File | Change |
|------|--------|
| `ui/screens/tv/TvHomeScreen.kt` | Add `onPlay` param, wire through headers |
| `ui/tv/TvNavGraph.kt` | Pass `onPlay` to Home, wire Settings route |
| `ui/screens/tv/TvLibraryScreen.kt` | Collection-type fallback lookup + favorites path |
| `ui/viewmodel/MainAppViewModel.kt` | Add `loadItemById()` and `loadFavorites()` |
| `data/repository/JellyfinRepository.kt` | Expose public `getItemDetails()` |
| `ui/screens/tv/TvItemDetailScreen.kt` | Add `LaunchedEffect` for on-demand fetch |
| `ui/screens/tv/TvSettingsScreen.kt` | **New file** — simple settings screen |
| `ui/theme/TvTheme.kt` | Add `cinefinTvColorScheme()` |
| `ui/tv/TvJellyfinApp.kt` | Apply `cinefinTvColorScheme()` to TvMaterialTheme |
| `gradle/libs.versions.toml` | Bump `androidxTvMaterial` |
