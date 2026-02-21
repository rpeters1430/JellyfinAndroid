# Transcoding Progress for Offline Downloads - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Show server-side transcoding progress when downloading video at lower-than-original quality, replacing the current indeterminate progress bar with actual percentage from the Jellyfin Sessions API.

**Architecture:** Poll `SessionApi.getSessions(deviceId)` every 3 seconds during transcoded downloads. Extract `TranscodingInfo.completionPercentage` from the matching session. Feed it into `DownloadProgress.transcodingProgress`. Display two-phase UI: transcoding progress (Phase 1) then download progress (Phase 2).

**Tech Stack:** Jellyfin Kotlin SDK 1.8.6 (`SessionApi`, `TranscodingInfo`, `SessionInfoDto`), Kotlin Coroutines, WorkManager, Jetpack Compose, NotificationCompat.

**Design Doc:** `docs/plans/2026-02-21-transcoding-progress-design.md`

---

### Task 1: Add `playSessionId` to OfflineDownload model

**Files:**
- Modify: `app/src/main/java/com/rpeters/jellyfin/data/offline/OfflineDownload.kt`

**Step 1: Add `playSessionId` field to `OfflineDownload`**

In `OfflineDownload.kt`, add a nullable `playSessionId` field after the `quality` field:

```kotlin
val quality: VideoQuality? = null,
val playSessionId: String? = null,  // For tracking server transcoding progress
val thumbnailUrl: String? = null,
```

This field is `@Serializable` and defaults to null, so it's backward-compatible with existing DataStore data.

**Step 2: Verify build compiles**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/rpeters/jellyfin/data/offline/OfflineDownload.kt
git commit -m "feat: add playSessionId field to OfflineDownload model"
```

---

### Task 2: Extract PlaySessionId from transcoded URLs

**Files:**
- Modify: `app/src/main/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManager.kt`

**Step 1: Add URL parsing helper**

Add this private method to `OfflineDownloadManager`:

```kotlin
/**
 * Extract PlaySessionId from a Jellyfin transcoded stream URL.
 * URLs contain PlaySessionId as a query parameter.
 */
private fun extractPlaySessionId(url: String): String? {
    return try {
        android.net.Uri.parse(url).getQueryParameter("PlaySessionId")
    } catch (e: Exception) {
        null
    }
}
```

**Step 2: Use it in `createDownload()`**

In the `createDownload()` method, extract the PlaySessionId before encrypting the URL. Modify the return statement to include `playSessionId`:

Find the existing return block in `createDownload()`:
```kotlin
return OfflineDownload(
    id = downloadId,
    jellyfinItemId = item.id.toString(),
    itemName = item.name ?: context.getString(R.string.unknown),
    itemType = item.type.toString(),
    downloadUrl = encryptedUrlKey,
    localFilePath = localPath,
    fileSize = 0L,
    quality = quality,
    downloadStartTime = System.currentTimeMillis(),
)
```

Replace with:
```kotlin
return OfflineDownload(
    id = downloadId,
    jellyfinItemId = item.id.toString(),
    itemName = item.name ?: context.getString(R.string.unknown),
    itemType = item.type.toString(),
    downloadUrl = encryptedUrlKey,
    localFilePath = localPath,
    fileSize = 0L,
    quality = quality,
    playSessionId = extractPlaySessionId(url),
    downloadStartTime = System.currentTimeMillis(),
)
```

**Step 3: Verify build compiles**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/com/rpeters/jellyfin/data/offline/OfflineDownload.kt
git add app/src/main/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManager.kt
git commit -m "feat: extract PlaySessionId from transcoded download URLs"
```

---

### Task 3: Add `getTranscodingProgress()` to JellyfinRepository

**Files:**
- Modify: `app/src/main/java/com/rpeters/jellyfin/data/repository/JellyfinRepository.kt`

**Step 1: Add import for sessionApi extension**

Add this import at the top of `JellyfinRepository.kt`:

```kotlin
import org.jellyfin.sdk.api.client.extensions.sessionApi
```

**Step 2: Add TranscodingProgressInfo data class**

Add this data class at the bottom of the file (outside the class):

```kotlin
data class TranscodingProgressInfo(
    val completionPercentage: Double,
    val isActive: Boolean,
    val bitrate: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
)
```

**Step 3: Add `getTranscodingProgress()` method**

Add this method to `JellyfinRepository`, near the other streaming methods (around line 1300):

```kotlin
/**
 * Get transcoding progress for active sessions on this device.
 * Polls the Sessions API to get TranscodingInfo.completionPercentage.
 *
 * @param deviceId The device ID to filter sessions by
 * @param jellyfinItemId Optional item ID to match specific transcoding session
 * @return TranscodingProgressInfo if an active transcoding session is found, null otherwise
 */
suspend fun getTranscodingProgress(
    deviceId: String,
    jellyfinItemId: String? = null,
): TranscodingProgressInfo? {
    return try {
        val client = getCurrentAuthenticatedClient() ?: return null
        val response = withIo {
            client.sessionApi.getSessions(deviceId = deviceId)
        }
        val sessions = response.content

        // Find session with active transcoding
        val session = if (jellyfinItemId != null) {
            sessions.find { session ->
                session.transcodingInfo != null &&
                    session.nowPlayingItem?.id?.toString() == jellyfinItemId
            }
        } else {
            sessions.find { it.transcodingInfo != null }
        }

        session?.transcodingInfo?.let { info ->
            TranscodingProgressInfo(
                completionPercentage = info.completionPercentage ?: 0.0,
                isActive = true,
                bitrate = info.bitrate,
                width = info.width,
                height = info.height,
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Failed to get transcoding progress: ${e.message}")
        }
        null
    }
}
```

Note: Make sure the class has a `TAG` constant. Check if one exists; if not, add `private const val TAG = "JellyfinRepository"` to the companion object.

**Step 4: Verify build compiles**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/java/com/rpeters/jellyfin/data/repository/JellyfinRepository.kt
git commit -m "feat: add getTranscodingProgress() using Sessions API"
```

---

### Task 4: Add DeviceCapabilities dependency to OfflineDownloadManager

**Files:**
- Modify: `app/src/main/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManager.kt`

**Step 1: Add DeviceCapabilities to constructor**

Add `DeviceCapabilities` to the `OfflineDownloadManager` constructor:

```kotlin
@Singleton
class OfflineDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: JellyfinRepository,
    private val okHttpClient: OkHttpClient,
    private val encryptedPreferences: com.rpeters.jellyfin.data.security.EncryptedPreferences,
    private val dispatchers: com.rpeters.jellyfin.data.common.DispatcherProvider,
    private val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
    private val deviceCapabilities: com.rpeters.jellyfin.data.DeviceCapabilities,
) {
```

Add the import if needed:
```kotlin
import com.rpeters.jellyfin.data.DeviceCapabilities
```

**Step 2: Verify build compiles**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL (DeviceCapabilities is already provided by Hilt via NetworkModule)

**Step 3: Commit**

```bash
git add app/src/main/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManager.kt
git commit -m "feat: inject DeviceCapabilities into OfflineDownloadManager"
```

---

### Task 5: Integrate transcoding progress polling into download loop

**Files:**
- Modify: `app/src/main/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManager.kt`

**Step 1: Add required imports**

Add these imports to `OfflineDownloadManager.kt`:

```kotlin
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.delay
```

**Step 2: Add transcoding polling helper method**

Add this method to the class:

```kotlin
/**
 * Poll server for transcoding progress in a background coroutine.
 * Updates the provided AtomicReference with the latest progress.
 * Stops when transcoding completes or coroutine is cancelled.
 */
private fun CoroutineScope.launchTranscodingPoller(
    download: OfflineDownload,
    progressRef: AtomicReference<com.rpeters.jellyfin.data.repository.TranscodingProgressInfo?>,
): Job? {
    // Only poll for non-original quality downloads
    val quality = download.quality ?: return null
    if (quality.id == "original") return null

    val deviceId = deviceCapabilities.getDeviceId()

    return launch {
        try {
            while (isActive) {
                delay(TRANSCODING_POLL_INTERVAL_MS)
                val progress = repository.getTranscodingProgress(
                    deviceId = deviceId,
                    jellyfinItemId = download.jellyfinItemId,
                )
                progressRef.set(progress)

                // Stop polling once transcoding is complete
                if (progress != null && progress.completionPercentage >= 100.0) {
                    break
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Transcoding poller stopped: ${e.message}")
            }
        }
    }
}
```

**Step 3: Add constant for polling interval**

In the `companion object`, add:

```kotlin
private const val TRANSCODING_POLL_INTERVAL_MS = 3000L
```

**Step 4: Modify `downloadFile()` to use transcoding poller**

Replace the existing `downloadFile()` method. The key changes are:
1. Launch a transcoding poller coroutine at the start
2. Read from the `AtomicReference` when building `DownloadProgress`
3. Cancel the poller when done

```kotlin
private suspend fun downloadFile(
    response: Response,
    download: OfflineDownload,
    startByte: Long,
    append: Boolean,
    onProgress: (suspend (OfflineDownload, DownloadProgress) -> Unit)?,
): Long {
    val body = response.body

    val bodyLength = body.contentLength()
    val totalBytesExpected = if (bodyLength > 0L) startByte + bodyLength else bodyLength
    val outputFile = File(download.localFilePath)

    // Create parent directories if they don't exist
    outputFile.parentFile?.mkdirs()

    // Start transcoding progress poller for non-original quality downloads
    val transcodingRef = AtomicReference<com.rpeters.jellyfin.data.repository.TranscodingProgressInfo?>(null)
    val pollerJob = downloadScope.launchTranscodingPoller(download, transcodingRef)

    try {
        body.byteStream().use { inputStream ->
            FileOutputStream(outputFile, append).use { outputStream ->

                val buffer = ByteArray(CHUNK_SIZE)
                var totalBytesRead = startByte
                var bytesRead: Int
                val startTime = System.currentTimeMillis()

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (!currentCoroutineContext().isActive) break

                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    // Update progress
                    val currentTime = System.currentTimeMillis()
                    val elapsedTime = currentTime - startTime
                    val speed = if (elapsedTime > 0) ((totalBytesRead - startByte) * 1000L) / elapsedTime else 0L
                    val remainingBytes = totalBytesExpected - totalBytesRead
                    val remainingTime = if (speed > 0) (remainingBytes * 1000L) / speed else null

                    // Read latest transcoding progress
                    val transcodingInfo = transcodingRef.get()
                    val isNonOriginal = download.quality != null && download.quality.id != "original"
                    val transcodingActive = isNonOriginal &&
                        (transcodingInfo == null || transcodingInfo.completionPercentage < 100.0)
                    val isTranscoding = totalBytesExpected <= 0L && transcodingActive

                    val progress = DownloadProgress(
                        downloadId = download.id,
                        downloadedBytes = totalBytesRead,
                        totalBytes = totalBytesExpected,
                        progressPercent = if (totalBytesExpected > 0) {
                            (totalBytesRead.toFloat() / totalBytesExpected * 100f)
                        } else {
                            // Use transcoding progress as the displayed percentage when no content length
                            transcodingInfo?.completionPercentage?.toFloat() ?: 0f
                        },
                        downloadSpeedBps = speed,
                        remainingTimeMs = remainingTime,
                        isTranscoding = isTranscoding,
                        transcodingProgress = transcodingInfo?.completionPercentage?.toFloat(),
                    )

                    updateDownloadProgress(progress)
                    onProgress?.invoke(download, progress)
                    updateDownloadBytes(download.id, totalBytesRead)
                }
                return totalBytesRead
            }
        }
    } finally {
        pollerJob?.cancel()
    }
}
```

**Step 5: Verify build compiles**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add app/src/main/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManager.kt
git commit -m "feat: poll server transcoding progress during transcoded downloads"
```

---

### Task 6: Update notification to show transcoding phase

**Files:**
- Modify: `app/src/main/java/com/rpeters/jellyfin/data/worker/OfflineDownloadWorker.kt`

**Step 1: Update `createForegroundInfo()` to accept transcoding info**

Add two parameters to `createForegroundInfo()`:

```kotlin
private fun createForegroundInfo(
    downloadId: String,
    itemName: String,
    progressPercent: Int,
    downloadedBytes: Long,
    totalBytes: Long,
    indeterminate: Boolean,
    isTranscoding: Boolean = false,
    transcodingProgress: Float? = null,
): ForegroundInfo {
```

**Step 2: Update the notification builder in `createForegroundInfo()`**

Replace the `.setContentTitle`, `.setProgress`, and `.setSubText` chain with transcoding-aware logic:

```kotlin
val notification = NotificationCompat.Builder(applicationContext, CHANNEL_DOWNLOADS)
    .setSmallIcon(R.drawable.ic_launcher_monochrome)
    .setContentTitle("Downloading")
    .setContentText(itemName)
    .setOnlyAlertOnce(true)
    .setOngoing(true)
    .setCategory(NotificationCompat.CATEGORY_PROGRESS)
    .setPriority(NotificationCompat.PRIORITY_LOW)
    .apply {
        if (isTranscoding && transcodingProgress != null) {
            // Phase 1: Server is transcoding
            val tcPercent = transcodingProgress.toInt().coerceIn(0, 100)
            setProgress(100, tcPercent, false)
            setSubText("Server transcoding: $tcPercent% \u00b7 Downloaded: ${formatBytes(downloadedBytes)}")
        } else if (totalBytes > 0L) {
            // Phase 2: Normal download with known size
            setProgress(100, progressPercent, false)
            setSubText("${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}")
        } else {
            // Fallback: indeterminate
            setProgress(100, 0, true)
            setSubText("Preparing download")
        }
    }
    .addAction(R.drawable.ic_launcher_monochrome, "Pause", pausePendingIntent)
    .addAction(R.drawable.ic_launcher_monochrome, "Cancel", cancelPendingIntent)
    .build()
```

**Step 3: Update the progress callback in `doWork()` to pass transcoding info**

In the `doWork()` method, update the `setForeground` call inside the progress callback:

```kotlin
setForeground(
    createForegroundInfo(
        downloadId = downloadId,
        itemName = download.itemName,
        progressPercent = percent,
        downloadedBytes = progress.downloadedBytes,
        totalBytes = progress.totalBytes,
        indeterminate = progress.totalBytes <= 0L && !progress.isTranscoding,
        isTranscoding = progress.isTranscoding,
        transcodingProgress = progress.transcodingProgress,
    ),
)
```

**Step 4: Verify build compiles**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/java/com/rpeters/jellyfin/data/worker/OfflineDownloadWorker.kt
git commit -m "feat: show server transcoding progress in download notification"
```

---

### Task 7: Update DownloadsScreen UI for transcoding progress

**Files:**
- Modify: `app/src/main/java/com/rpeters/jellyfin/ui/downloads/DownloadsScreen.kt`

**Step 1: Update `DownloadProgressIndicator` composable**

Replace the existing `DownloadProgressIndicator` function:

```kotlin
@Composable
fun DownloadProgressIndicator(progress: DownloadProgress) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.Spacing4)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                when {
                    progress.isTranscoding && progress.transcodingProgress != null ->
                        "Server transcoding: ${progress.transcodingProgress.roundToInt()}%"
                    progress.isTranscoding ->
                        "Transcoding..."
                    else ->
                        "${progress.progressPercent.roundToInt()}%"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (progress.totalBytes > 0L) {
                    "${formatBytes(progress.downloadedBytes)} / ${formatBytes(progress.totalBytes)}"
                } else {
                    formatBytes(progress.downloadedBytes)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when {
            progress.isTranscoding && progress.transcodingProgress != null -> {
                // Determinate progress bar using transcoding percentage
                LinearProgressIndicator(
                    progress = { progress.transcodingProgress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            progress.isTranscoding || progress.totalBytes <= 0L -> {
                // Indeterminate fallback
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            else -> {
                // Normal download progress
                LinearProgressIndicator(
                    progress = { progress.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "${formatBytes(progress.downloadSpeedBps)}/s",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!progress.isTranscoding) {
                progress.remainingTimeMs?.let { remaining ->
                    Text(
                        formatDuration(remaining),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
```

**Step 2: Verify build compiles**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/rpeters/jellyfin/ui/downloads/DownloadsScreen.kt
git commit -m "feat: show determinate transcoding progress in downloads UI"
```

---

### Task 8: Update DownloadButton for transcoding status

**Files:**
- Modify: `app/src/main/java/com/rpeters/jellyfin/ui/components/DownloadButton.kt`

**Step 1: Update the `DownloadingButton` text display**

In the `DownloadButton` composable, update the `DOWNLOADING` case to pass transcoding info to `DownloadingButton`:

Replace:
```kotlin
DownloadStatus.DOWNLOADING -> {
    DownloadingButton(
        progress = progress?.progressPercent ?: 0f,
        onPause = { downloadsViewModel.pauseDownload(currentDownload.id) },
        showText = showText,
    )
}
```

With:
```kotlin
DownloadStatus.DOWNLOADING -> {
    DownloadingButton(
        progress = progress?.progressPercent ?: 0f,
        isTranscoding = progress?.isTranscoding == true,
        transcodingProgress = progress?.transcodingProgress,
        onPause = { downloadsViewModel.pauseDownload(currentDownload.id) },
        showText = showText,
    )
}
```

**Step 2: Update `DownloadingButton` signature and text**

Update the `DownloadingButton` composable:

```kotlin
@Composable
private fun DownloadingButton(
    progress: Float,
    isTranscoding: Boolean = false,
    transcodingProgress: Float? = null,
    onPause: () -> Unit,
    showText: Boolean,
) {
    val displayText = when {
        isTranscoding && transcodingProgress != null ->
            "Transcoding... ${transcodingProgress.roundToInt()}%"
        isTranscoding ->
            "Transcoding..."
        else ->
            "Downloading... ${progress.roundToInt()}%"
    }
    val displayProgress = when {
        isTranscoding && transcodingProgress != null -> transcodingProgress / 100f
        else -> progress / 100f
    }

    if (showText) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                IconButton(
                    onClick = onPause,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.Pause,
                        contentDescription = "Pause",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            LinearProgressIndicator(
                progress = { displayProgress },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { displayProgress },
                modifier = Modifier.size(40.dp),
                strokeWidth = 3.dp,
            )
            IconButton(
                onClick = onPause,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Default.Pause,
                    contentDescription = "Pause",
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
```

**Step 3: Verify build compiles**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/com/rpeters/jellyfin/ui/components/DownloadButton.kt
git commit -m "feat: show transcoding progress in download button"
```

---

### Task 9: Update existing tests

**Files:**
- Modify: `app/src/test/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManagerTest.kt`

**Step 1: Update test setUp to provide DeviceCapabilities mock**

In the test's `setUp()`, add a mock for `DeviceCapabilities`:

```kotlin
private lateinit var deviceCapabilities: com.rpeters.jellyfin.data.DeviceCapabilities
```

In `setUp()`:
```kotlin
deviceCapabilities = mockk(relaxed = true)
every { deviceCapabilities.getDeviceId() } returns "test-device-id"
```

Update the `OfflineDownloadManager` constructor call to include the new parameter:

```kotlin
manager = OfflineDownloadManager(
    context = context,
    repository = repository,
    okHttpClient = okHttpClient,
    encryptedPreferences = encryptedPreferences,
    dispatchers = testDispatchers,
    dataStore = dataStore,
    deviceCapabilities = deviceCapabilities,
)
```

**Step 2: Run existing tests**

Run: `./gradlew testDebugUnitTest --tests "com.rpeters.jellyfin.data.offline.OfflineDownloadManagerTest" 2>&1 | tail -10`
Expected: All existing tests PASS

**Step 3: Commit**

```bash
git add app/src/test/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManagerTest.kt
git commit -m "test: update OfflineDownloadManagerTest for DeviceCapabilities dependency"
```

---

### Task 10: Run full test suite and verify

**Files:** None (verification only)

**Step 1: Run unit tests**

Run: `./gradlew testDebugUnitTest 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL with all tests passing

**Step 2: Run lint check**

Run: `./gradlew lintDebug 2>&1 | tail -10`
Expected: No new errors (warnings are acceptable)

**Step 3: Run full build**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 4: Final commit with all changes verified**

If any fixes were needed, commit them:
```bash
git commit -m "fix: address build/test issues from transcoding progress feature"
```
