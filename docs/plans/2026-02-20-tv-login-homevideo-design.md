# TV Login + Home Video Screens — Design Document

**Date**: 2026-02-20
**Author**: Claude Code + user
**Status**: Approved

---

## Context

Three follow-on fixes to the Android TV implementation, building on the surgical fixes committed earlier today. The login screen works but looks generic (uses Jellyfin branding, plain background, mixed M3/TV Material components). The home video ("Stuff") library shows empty because of a library-loading bug. The home video detail screen shows a portrait poster card that looks wrong for landscape video content.

---

## Fix 1: Login Screen Brand Refresh

**Root cause**: `TvServerConnectionScreen` lives inside `TvJellyfinApp.kt` (should be its own file), uses `"Jellyfin"` as the title (wrong brand), and mixes regular M3 `Text`/`Button` with TV Material components. The background is plain.

**Solution**:

1. Extract `TvServerConnectionScreen` composable from `TvJellyfinApp.kt` into a new file `ui/screens/tv/TvServerConnectionScreen.kt`.
2. Within the composable:
   - Wrap content in `TvImmersiveBackground(backdropUrl = null)` as the full-screen dark background
   - Change title from `"Jellyfin"` → `"Cinefin"`, styled with `TvMaterialTheme.colorScheme.primary`
   - Replace all `Text` → `TvText` (from `androidx.tv.material3`)
   - Replace both `Button` → `TvButton` (from `androidx.tv.material3`)
   - Keep `OutlinedTextField` (M3) — it handles D-pad/keyboard navigation correctly on TV and TV Material has no equivalent
   - Preserve all existing logic: `FocusRequester` chain, auto-focus `LaunchedEffect`, connecting state, error card, Quick Connect tip
3. Clean up `TvJellyfinApp.kt`: remove the now-extracted composable and its associated imports.

**Files touched**:
- `ui/screens/tv/TvServerConnectionScreen.kt` — **new file**
- `ui/tv/TvJellyfinApp.kt` — remove extracted composable + its imports

---

## Fix 2: Home Video Library Route + Loading Bug

**Root cause (loading bug)**: `TvLibraryScreen` calls `viewModel.loadHomeVideos(lib.id.toString())` for unknown collection types. This function creates a *synthetic* `BaseItemDto` with `collectionType = null`, losing the real library's `collectionType`. The `loadLibraryTypeData` function uses `collectionType` to decide item-type filters; with `null`, it sends `itemTypes = "Book,AudioBook,Video"` instead of the correct filter for a home video library, causing Jellyfin to return 0 items.

**Root cause (missing route)**: There is no dedicated `tv_homevideos` nav route or drawer item, so home video libraries are only reachable by UUID from the home screen carousel.

**Solution**:

1. **`TvNavigationItem.kt`** — Add `Videos` between Music and Search:
   ```kotlin
   object Videos : TvNavigationItem("tv_homevideos", "Videos", Icons.Default.VideoLibrary)
   ```
   Add to `items` companion list.

2. **`TvNavGraph.kt`** — Add `TvRoutes.HomeVideos = "tv_homevideos"` constant and a composable that renders `TvLibraryScreen(libraryId = "homevideos")`.

3. **`TvLibraryScreen.kt`** — three changes:
   - **Library lookup**: Add `"homevideos"` to the synthetic-ID fallback:
     ```kotlin
     "homevideos" -> appState.libraries.firstOrNull {
         it.collectionType == CollectionType.HOMEVIDEOS
     } ?: appState.libraries.firstOrNull { it.collectionType == null }
     ```
   - **Loading trigger**: Add explicit `CollectionType.HOMEVIDEOS` case:
     ```kotlin
     CollectionType.HOMEVIDEOS -> viewModel.loadLibraryTypeData(lib, LibraryType.STUFF)
     ```
   - **`else` branch (unknown type)**: Change from `viewModel.loadHomeVideos(lib.id.toString())` to `viewModel.loadLibraryTypeData(lib, LibraryType.STUFF)` — passes real library, preserving collectionType.

**Files touched**:
- `ui/tv/TvNavigationItem.kt`
- `ui/tv/TvNavGraph.kt`
- `ui/screens/tv/TvLibraryScreen.kt`

---

## Fix 3: Home Video Detail Screen Adaptation

**Root cause**: `TvItemDetailScreen` shows a portrait poster card (260×390dp) for all item types. Home videos (`BaseItemKind.VIDEO`) have landscape backdrops, not portrait posters, so the image looks wrong.

**Solution**: Detect `val isVideo = item?.type == BaseItemKind.VIDEO`. When true, render a landscape backdrop card (390×220dp, 16:9 ratio) using `viewModel.getBackdropUrl(item)` instead of the portrait poster. Fall back to `getImageUrl(item)` if no backdrop is available.

No other changes needed:
- The seasons/episodes section already guards `item.type == SERIES` — hides for VIDEO automatically
- The play button logic already handles VIDEO correctly (`playItem` defaults to `item` when not a series)
- The "Recently Added" fallback carousel is acceptable for video items

**Files touched**:
- `ui/screens/tv/TvItemDetailScreen.kt`

---

## Files Modified Summary

| File | Change |
|------|--------|
| `ui/screens/tv/TvServerConnectionScreen.kt` | **New file** — extracted + branded login screen |
| `ui/tv/TvJellyfinApp.kt` | Remove extracted composable and its imports |
| `ui/tv/TvNavigationItem.kt` | Add `Videos` nav item |
| `ui/tv/TvNavGraph.kt` | Add `HomeVideos` route + composable |
| `ui/screens/tv/TvLibraryScreen.kt` | Lookup + loading trigger + else-branch fix |
| `ui/screens/tv/TvItemDetailScreen.kt` | Landscape card for VIDEO items |

---

## What We Are NOT Changing

- `OutlinedTextField` in the login screen — works correctly on TV with D-pad, TV Material has no equivalent
- Quick Connect screen — already works
- Home video item card in the library grid — `TvContentCard` handles VIDEO items fine
- The "Recently Added" fallback carousel in the detail screen — acceptable for now
