# Library Screen HTTP 400 Error - Enhanced Fix

## Problem Analysis

The user reports that clicking on Library buttons (Movies, Music, Home Videos) in the bottom navigation shows:
- **Movies**: "No Movies found"  
- **Music**: "No music libraries available"
- **Home Videos**: "Home Videos Screen and no media"
- **HTTP 400 errors** appear when loading specific library data

## Root Causes Identified

### Issue 1: Missing Item Types for Music Libraries
- Music library requests were not specifying proper item types
- Jellyfin API returns HTTP 400 when item types are missing for specific library collections

### Issue 2: Missing Item Types for "Other" Libraries (Home Videos, Books, etc.)  
- `loadOtherLibraryItems()` wasn't specifying item types based on collection type
- Home Videos, Books, Photos libraries need specific item type filtering

### Issue 3: Insufficient Debugging for Library Discovery
- Limited visibility into library filtering and identification process
- Difficult to diagnose why libraries aren't being found

## Implemented Fixes

### Fix 1: Enhanced Music Library Loading ✅
**File**: `app/src/main/java/com/rpeters/jellyfin/ui/viewmodel/MainAppViewModel.kt`

```kotlin
// BEFORE - Missing item types
when (val result = mediaRepository.getLibraryItems(
    parentId = musicLibraryId,
    startIndex = 0,
    limit = 50
)) {

// AFTER - Music-specific item types
when (val result = mediaRepository.getLibraryItems(
    parentId = musicLibraryId,
    itemTypes = "MusicAlbum,MusicArtist,Audio", // ✅ Fixed!
    startIndex = 0,
    limit = 50
)) {
```

### Fix 2: Smart Item Type Detection for Other Libraries ✅
**File**: `app/src/main/java/com/rpeters/jellyfin/ui/viewmodel/MainAppViewModel.kt`

```kotlin
// BEFORE - No item types specified
when (val result = mediaRepository.getLibraryItems(
    parentId = libraryId,
    startIndex = 0,
    limit = 50
)) {

// AFTER - Collection-type-specific item types
val library = _appState.value.libraries.find { it.id.toString() == libraryId }
val itemTypes = when (library?.collectionType) {
    org.jellyfin.sdk.model.api.CollectionType.HOMEVIDEOS -> "Video"
    org.jellyfin.sdk.model.api.CollectionType.BOOKS -> "Book,AudioBook"
    org.jellyfin.sdk.model.api.CollectionType.PHOTOS -> "Photo"
    else -> null // Let server determine for mixed content libraries
}

when (val result = mediaRepository.getLibraryItems(
    parentId = libraryId,
    itemTypes = itemTypes, // ✅ Fixed!
    startIndex = 0,
    limit = 50
)) {
```

### Fix 3: Enhanced Library Discovery Debugging ✅
**File**: `app/src/main/java/com/rpeters/jellyfin/ui/viewmodel/MainAppViewModel.kt`

```kotlin
// Added comprehensive debugging for Movies
if (BuildConfig.DEBUG) {
    Log.d("MainAppViewModel", "loadAllMovies: Found ${movieLibraries.size} movie libraries")
    movieLibraries.forEachIndexed { index, library ->
        Log.d("MainAppViewModel", "loadAllMovies: Movie library $index: ${library.name} (ID: ${library.id})")
    }
}

if (movieLibraries.isEmpty()) {
    if (BuildConfig.DEBUG) {
        Log.w("MainAppViewModel", "loadAllMovies: No movie libraries found in ${_appState.value.libraries.size} total libraries")
        _appState.value.libraries.forEach { lib ->
            Log.d("MainAppViewModel", "Available library: ${lib.name} (Type: ${lib.collectionType}, ID: ${lib.id})")
        }
    }
    // Show helpful error message
}
```

### Fix 4: Enhanced TV Shows Debugging ✅
Same debugging pattern applied to `loadAllTVShows()` for better diagnostics.

## Expected Results

### After These Fixes:
1. **Music Screen**: ✅ Should load music albums, artists, and audio files without HTTP 400 errors
2. **Home Videos Screen**: ✅ Should load video files from home video libraries
3. **Books Screen**: ✅ Should load books and audiobooks correctly
4. **Photo Screen**: ✅ Should load photo libraries correctly
5. **Movies/TV Shows**: ✅ Enhanced debugging will show exactly what libraries are found
6. **HTTP 400 Errors**: ✅ Should be eliminated by proper item type specification

### Debug Information Available:
- Detailed library discovery logging
- Item type specification logging
- Clear error messages when libraries are not found
- Complete library listing when issues occur

## Technical Details

### Item Type Mapping:
- **MOVIES**: `"Movie"`
- **TV SHOWS**: `"Series"`  
- **MUSIC**: `"MusicAlbum,MusicArtist,Audio"`
- **HOME VIDEOS**: `"Video"`
- **BOOKS**: `"Book,AudioBook"`
- **PHOTOS**: `"Photo"`
- **MIXED LIBRARIES**: `null` (let server determine)

### Error Prevention:
- ✅ All API calls now include appropriate `parentId` to avoid server-level filtering errors
- ✅ Item types specified based on collection type to prevent invalid requests
- ✅ Fallback handling for mixed content libraries
- ✅ Enhanced error messages for troubleshooting

## Testing Instructions

1. **Build and Run**: Code compiles successfully ✅
2. **Navigate to Library Screens**: Click Movies, Music, Home Videos in bottom nav
3. **Check Logs**: Look for detailed debugging information about library discovery
4. **Verify Content**: Each library type should show appropriate content instead of empty states

## Expected Log Output (Debug Mode)

```
D MainAppViewModel: loadAllMovies: Found 1 movie libraries
D MainAppViewModel: loadAllMovies: Movie library 0: Movies (ID: f137a2dd-21bb-c1b9-9aa5-c0f6bf02a805)
D JellyfinMediaRepository: getLibraryItems called with parentId=f137a2dd-21bb-c1b9-9aa5-c0f6bf02a805, itemTypes=Movie, startIndex=0, limit=50
D MainAppViewModel: loadAllMovies: Successfully loaded 50 movies for page 0
```

This comprehensive fix should resolve the HTTP 400 errors and empty library screens that the user is experiencing.
