# JellyfinRepository Refactor Status

**Date**: 2026-03-30

## Goal

Reduce coupling to the concrete `JellyfinRepository` by moving consumers to `IJellyfinRepository` where they only need the repository contract, while leaving session-heavy and cast-specific code concrete until the interface boundary is clearer.

## Current State

The refactor is in progress and the app still builds.

Build verification completed with:

```bash
GRADLE_USER_HOME=/tmp/gradle-home ./gradlew assembleDebug
```

Latest result during this session: `BUILD SUCCESSFUL`

## Completed In This Slice

### Interface expansion

`IJellyfinRepository` now includes a wider read/playback surface, including:

- `getLibraryItems`
- `getItemsByPerson`
- `getEpisodesForSeason`
- `getSeriesDetails`
- `getMovieDetails`
- `getEpisodeDetails`
- `searchItems`
- `getTranscodedStreamUrl`
- existing playback/image/auth methods already present

Files:

- [IJellyfinRepository.kt](/home/rpeters1428/Cinefin/app/src/main/java/com/rpeters/jellyfin/data/repository/IJellyfinRepository.kt)
- [JellyfinRepository.kt](/home/rpeters1428/Cinefin/app/src/main/java/com/rpeters/jellyfin/data/repository/JellyfinRepository.kt)

### Consumers converted to `IJellyfinRepository`

These now depend on the interface instead of the concrete repository:

- [EnhancedPlaybackManager.kt](/home/rpeters1428/Cinefin/app/src/main/java/com/rpeters/jellyfin/data/playback/EnhancedPlaybackManager.kt)
- [OfflineDownloadManager.kt](/home/rpeters1428/Cinefin/app/src/main/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManager.kt)
- [SeasonEpisodesViewModel.kt](/home/rpeters1428/Cinefin/app/src/main/java/com/rpeters/jellyfin/ui/viewmodel/SeasonEpisodesViewModel.kt)
- [MovieDetailViewModel.kt](/home/rpeters1428/Cinefin/app/src/main/java/com/rpeters/jellyfin/ui/viewmodel/MovieDetailViewModel.kt)
- [TVSeasonViewModel.kt](/home/rpeters1428/Cinefin/app/src/main/java/com/rpeters/jellyfin/ui/viewmodel/TVSeasonViewModel.kt)
- [PersonDetailViewModel.kt](/home/rpeters1428/Cinefin/app/src/main/java/com/rpeters/jellyfin/ui/viewmodel/PersonDetailViewModel.kt)
- [AiAssistantViewModel.kt](/home/rpeters1428/Cinefin/app/src/main/java/com/rpeters/jellyfin/ui/viewmodel/AiAssistantViewModel.kt)
- [TranscodingDiagnosticsViewModel.kt](/home/rpeters1428/Cinefin/app/src/main/java/com/rpeters/jellyfin/ui/viewmodel/TranscodingDiagnosticsViewModel.kt)
- [VideoPlayerViewModel.kt](/home/rpeters1428/Cinefin/app/src/main/java/com/rpeters/jellyfin/ui/player/VideoPlayerViewModel.kt)
- [VideoPlayerMetadataManager.kt](/home/rpeters1428/Cinefin/app/src/main/java/com/rpeters/jellyfin/ui/player/VideoPlayerMetadataManager.kt)
- [DownloadsViewModel.kt](/home/rpeters1428/Cinefin/app/src/main/java/com/rpeters/jellyfin/ui/downloads/DownloadsViewModel.kt)

### Test updates

The corresponding tests for the converted consumers were updated to mock `IJellyfinRepository` instead of `JellyfinRepository`.

Examples:

- [SeasonEpisodesViewModelTest.kt](/home/rpeters1428/Cinefin/app/src/test/java/com/rpeters/jellyfin/ui/viewmodel/SeasonEpisodesViewModelTest.kt)
- [MovieDetailViewModelTest.kt](/home/rpeters1428/Cinefin/app/src/test/java/com/rpeters/jellyfin/ui/viewmodel/MovieDetailViewModelTest.kt)
- [TVSeasonViewModelTest.kt](/home/rpeters1428/Cinefin/app/src/test/java/com/rpeters/jellyfin/ui/viewmodel/TVSeasonViewModelTest.kt)
- [VideoPlayerViewModelTest.kt](/home/rpeters1428/Cinefin/app/src/test/java/com/rpeters/jellyfin/ui/player/VideoPlayerViewModelTest.kt)
- [VideoPlayerViewModelInitTest.kt](/home/rpeters1428/Cinefin/app/src/test/java/com/rpeters/jellyfin/ui/player/VideoPlayerViewModelInitTest.kt)
- [DownloadsViewModelTest.kt](/home/rpeters1428/Cinefin/app/src/test/java/com/rpeters/jellyfin/ui/downloads/DownloadsViewModelTest.kt)

### Additional cleanup

Unused concrete repository dependencies were removed from:

- [VideoPlayerPlaybackManager.kt](/home/rpeters1428/Cinefin/app/src/main/java/com/rpeters/jellyfin/ui/player/VideoPlayerPlaybackManager.kt)
- [VideoPlayerCastManager.kt](/home/rpeters1428/Cinefin/app/src/main/java/com/rpeters/jellyfin/ui/player/VideoPlayerCastManager.kt)

## Not Yet Converted

### Keep concrete for now

These still rely on concrete-only session/state/quick-connect APIs and should stay on `JellyfinRepository` until the abstraction is intentionally widened:

- [MainAppViewModel.kt](/home/rpeters1428/Cinefin/app/src/main/java/com/rpeters/jellyfin/ui/viewmodel/MainAppViewModel.kt)
- [ServerConnectionViewModel.kt](/home/rpeters1428/Cinefin/app/src/main/java/com/rpeters/jellyfin/ui/viewmodel/ServerConnectionViewModel.kt)

Reasons:

- use of `currentServer` and `isConnected` flows
- direct access to `connectivityChecker`
- session restoration / expiry checks
- quick connect methods
- token refresh helpers
- server connection test helpers

### Likely next hard boundary

This cast-specific code still depends on concrete `JellyfinRepository` behavior or nested repository types:

- [CastMediaLoadBuilder.kt](/home/rpeters1428/Cinefin/app/src/main/java/com/rpeters/jellyfin/ui/player/cast/CastMediaLoadBuilder.kt)

Reason:

- it uses `JellyfinRepository.CastReceiverProfile`
- it calls cast-specific playback APIs that are not part of `IJellyfinRepository`

## Recommended Next Step

Do **not** immediately dump every remaining `JellyfinRepository` method into `IJellyfinRepository`.

Instead, choose one of these two directions deliberately:

1. Keep the auth/session/connection layer concrete.
2. Extract a second, narrower interface for connection/session concerns.

Recommended tomorrow:

1. Leave [ServerConnectionViewModel.kt](/home/rpeters1428/Cinefin/app/src/main/java/com/rpeters/jellyfin/ui/viewmodel/ServerConnectionViewModel.kt) concrete for now.
2. Leave [MainAppViewModel.kt](/home/rpeters1428/Cinefin/app/src/main/java/com/rpeters/jellyfin/ui/viewmodel/MainAppViewModel.kt) concrete for now unless you want to introduce a dedicated session-state abstraction.
3. Decide whether cast belongs on:
   - `IJellyfinRepository`
   - a separate `ICastPlaybackRepository`
   - or the concrete repository intentionally

The cleanest next design is probably a separate cast-facing abstraction rather than expanding `IJellyfinRepository` with cast-only concerns.

## Known Caveat

The full unit test suite is not green overall, but the failures are broader than this refactor and the app build is currently passing. This refactor status should be treated as a compile/build handoff, not a “tests fully clean” handoff.

## Quick Resume Checklist

When resuming:

1. Rebuild with `GRADLE_USER_HOME=/tmp/gradle-home ./gradlew assembleDebug`
2. Inspect remaining concrete `JellyfinRepository` consumers with:

```bash
rg -n "JellyfinRepository\\b" app/src/main/java app/src/test/java
```

3. Decide whether the next slice is:
   - session/connection abstraction
   - cast abstraction
   - or stopping the refactor at the current boundary
