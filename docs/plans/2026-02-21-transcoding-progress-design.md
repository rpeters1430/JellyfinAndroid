# Transcoding Progress for Offline Downloads

## Problem

When downloading video at a quality lower than original (e.g., 720p from a 1080p source), the Jellyfin server transcodes on-the-fly. The HTTP response has no `Content-Length`, so the app shows an indeterminate progress bar with just "Transcoding..." and downloaded bytes. Users have no visibility into how far along the server is.

## Solution

Poll the Jellyfin Sessions API during transcoded downloads to get the server's `TranscodingInfo.completionPercentage` and display it in a two-phase notification/UI.

## Architecture

### Data Flow

```
DownloadsViewModel.startDownload(quality=720p)
  -> JellyfinStreamRepository.getTranscodedStreamUrl(...)  // URL has PlaySessionId
  -> OfflineDownloadManager.startDownload(item, quality, url)
    -> Extracts PlaySessionId from URL, stores in OfflineDownload
    -> WorkManager -> OfflineDownloadWorker -> executeDownload()
      -> downloadFile() loop:
          1. Read bytes from HTTP stream (existing)
          2. Every ~3s, poll JellyfinRepository.getTranscodingProgress(deviceId)
             -> SessionApi.getSessions(deviceId=deviceId)
             -> Find session with active TranscodingInfo
             -> Return completionPercentage
          3. Populate DownloadProgress with transcodingProgress
          4. Notification & UI show two-phase display
```

### Session Matching

Filter `getSessions(deviceId=deviceId)` to get only this device's sessions. If multiple exist, match by `nowPlayingItem.id` against `jellyfinItemId`.

## Model Changes

**OfflineDownload** - add field:
- `playSessionId: String?` - extracted from transcoded URL query params

**DownloadProgress** - existing fields used:
- `isTranscoding: Boolean` - already exists
- `transcodingProgress: Float?` - already exists

**New data class** in repository layer:
```kotlin
data class TranscodingProgressInfo(
    val completionPercentage: Double,
    val isActive: Boolean,
    val bitrate: Int?,
    val width: Int?,
    val height: Int?,
)
```

## Repository Layer

Add `getTranscodingProgress(deviceId: String)` to `JellyfinRepository`, which calls `sessionApi.getSessions(deviceId)` and extracts `transcodingInfo.completionPercentage`.

## Download Manager Changes

1. `createDownload()` - parse `PlaySessionId` from URL query params
2. `downloadFile()` - launch concurrent polling coroutine (every 3s) that updates a shared `AtomicReference<TranscodingProgressInfo?>`. Main loop reads it when building `DownloadProgress`. Stops when completionPercentage >= 100.
3. Skip polling for original quality downloads.

## Two-Phase Notification Display

**Phase 1** (server transcoding, `completionPercentage < 100`):
- Title: "Downloading"
- SubText: "Server transcoding: 45% · Downloaded: 120 MB"
- Progress bar: transcoding percentage (determinate)

**Phase 2** (transcoding done, `totalBytes > 0` or `completionPercentage >= 100`):
- Normal download display with bytes/total, speed, ETA

## UI Changes

- `DownloadProgressIndicator` - show determinate bar with "Server transcoding: X%" when transcoding is active with known progress
- `DownloadButton` - show transcoding percentage instead of download percentage during Phase 1

## Error Handling

- Sessions API failure: fall back to indeterminate progress (current behavior)
- No active transcoding session found: fall back to indeterminate
- Polling errors: log and continue, don't fail the download
- Original quality: skip polling entirely

## SDK References

- `org.jellyfin.sdk.api.client.extensions.sessionApi`
- `SessionApi.getSessions(deviceId: String?)` -> `Response<List<SessionInfoDto>>`
- `SessionInfoDto.transcodingInfo` -> `TranscodingInfo?`
- `TranscodingInfo.completionPercentage` -> `Double?`
