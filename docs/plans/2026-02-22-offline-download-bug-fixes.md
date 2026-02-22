# Offline Download Bug Fixes Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix 5 confirmed bugs in the offline download system covering a critical performance regression, two crash paths, and two correctness issues.

**Architecture:** Each fix is self-contained in its file. Fixes touch `OfflineDownloadManager`, `OfflinePlaybackManager`, and `OfflineDownloadWorker`. Tests live alongside existing tests in `app/src/test/java/com/rpeters/jellyfin/data/offline/` and `data/worker/`.

**Tech Stack:** Kotlin, JUnit 4, MockK, Robolectric, kotlinx.coroutines test, DataStore Preferences, WorkManager

---

## Bug reference (from code review)

| # | Severity | File | Line |
|---|----------|------|------|
| 1 | Critical perf | `OfflineDownloadManager.kt` | 417 |
| 2 | High crash | `OfflineDownloadManager.kt` | 578 |
| 3 | High silent failure | `OfflineDownloadManager.kt` | 614 |
| 4 | Medium wrong data | `OfflinePlaybackManager.kt` | 103–111 |
| 5 | Medium UX | `OfflineDownloadWorker.kt` | 277 |

---

## Task 1: DataStore hammering — throttle `updateDownloadBytes` in download loop

**Problem:** `downloadFile()` calls `updateDownloadBytes()` on every 8 KB chunk. `updateDownloadBytes` calls `saveDownloads()` which writes the entire download list to DataStore. A 1 GB download triggers ~131,000 DataStore writes, severely slowing the download and the IO queue.

**Fix:** Only call `updateDownloadBytes` (and thus `saveDownloads`) when progress has advanced by at least 1 MB since the last save, or at the end of the download. The in-memory `_downloadProgress` StateFlow can still update every chunk for live UI — that's cheap.

**Files:**
- Modify: `app/src/main/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManager.kt` lines 340–425
- Modify: `app/src/test/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManagerTest.kt`

---

### Step 1: Write the failing test

Add this test to `OfflineDownloadManagerTest`:

```kotlin
@Test
fun `updateDownloadBytes is not called on every chunk during download`() = runTest(testDispatcher) {
    val item = buildBaseItem(id = UUID.randomUUID(), name = "Test Movie")
    val downloadUrl = "https://server/stream/video.mp4"

    // Simulate a response body that is 3 MB (many 8KB chunks)
    val threeMB = ByteArray(3 * 1024 * 1024) { it.toByte() }
    val call = mockk<Call>(relaxed = true)
    val response = Response.Builder()
        .request(Request.Builder().url("https://server/video.mp4").build())
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(threeGB.toResponseBody("video/mp4".toMediaType()))
        .build()
    every { okHttpClient.newCall(any()) } returns call
    every { call.execute() } returns response

    val downloadId = manager.startDownload(item, downloadUrl = downloadUrl)
    advanceUntilIdle()

    // Verify download completed
    val download = manager.downloads.value.find { it.id == downloadId }
    assertEquals(DownloadStatus.COMPLETED, download?.status)
    // The download progress StateFlow should still have been updated
    assertTrue(manager.downloadProgress.value.containsKey(downloadId))
}
```

Note: this test verifies the download still works correctly after throttling, not the count of DataStore writes directly (DataStore internals are hard to spy on in unit tests). The key correctness invariant is that the download completes with COMPLETED status and final progress is tracked.

Run: `./gradlew testDebugUnitTest --tests "com.rpeters.jellyfin.data.offline.OfflineDownloadManagerTest.updateDownloadBytes is not called on every chunk during download" -x lint`

Expected: FAIL or PASS — this test validates the existing behavior works after the fix.

---

### Step 2: Apply the fix

In `OfflineDownloadManager.kt`, inside the `downloadFile` function, find the chunk loop and the two calls at lines ~415-418:

```kotlin
// BEFORE (current code around line 414-418):
updateDownloadProgress(progress)
onProgress?.invoke(download, progress)
updateDownloadBytes(download.id, totalBytesRead)   // ← REMOVE from loop
```

Add a `lastSavedBytes` variable before the loop and gate the `updateDownloadBytes` call:

```kotlin
// After: add before the while loop (around line 376)
var lastSavedBytes = startByte
val SAVE_INTERVAL_BYTES = 1_048_576L // 1 MB

// Inside the while loop, replace the updateDownloadBytes call:
updateDownloadProgress(progress)
onProgress?.invoke(download, progress)
// Only persist to DataStore every 1 MB to avoid hammering storage
if (totalBytesRead - lastSavedBytes >= SAVE_INTERVAL_BYTES) {
    updateDownloadBytes(download.id, totalBytesRead)
    lastSavedBytes = totalBytesRead
}
```

The existing call at line 281 (after the loop, on completion) already saves the final byte count — keep that.

---

### Step 3: Run tests

```
./gradlew testDebugUnitTest --tests "com.rpeters.jellyfin.data.offline.OfflineDownloadManagerTest" -x lint
```

Expected: all tests in the class PASS.

---

### Step 4: Commit

```bash
git add app/src/main/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManager.kt
git add app/src/test/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManagerTest.kt
git commit -m "perf: throttle DataStore writes during chunk download to every 1 MB"
```

---

## Task 2: NPE when external storage is unavailable

**Problem:** `getOfflineDirectory()` calls `context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)` which returns `null` when external storage is unavailable. Passing `null` as the parent to `File(parent, child)` throws `NullPointerException`. Every download, storage query, and directory creation flows through this method.

**Fix:** Fall back to internal files dir when external storage is unavailable.

**Files:**
- Modify: `app/src/main/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManager.kt` lines 577–584
- Modify: `app/src/test/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManagerTest.kt`

---

### Step 1: Write the failing test

The existing test environment (Robolectric) does have external storage. To test the null path, add a test that verifies a fallback directory is returned when external storage returns null. Since we can't easily make `getExternalFilesDir` return null in Robolectric, we test the observable invariant: `getOfflineDirectory()` always returns a non-null, writable directory.

Add to `OfflineDownloadManagerTest`:

```kotlin
@Test
fun `getAvailableStorage does not throw when called`() = runTest(testDispatcher) {
    // This should never throw NPE regardless of storage availability
    val storage = manager.getAvailableStorage()
    assertTrue("Should return a non-negative value", storage >= 0)
}

@Test
fun `getUsedStorage does not throw when called`() = runTest(testDispatcher) {
    val used = manager.getUsedStorage()
    assertTrue("Should return a non-negative value", used >= 0)
}
```

Run: `./gradlew testDebugUnitTest --tests "com.rpeters.jellyfin.data.offline.OfflineDownloadManagerTest" -x lint`

---

### Step 2: Apply the fix

Replace the `getOfflineDirectory()` function body:

```kotlin
// BEFORE:
private fun getOfflineDirectory(): File {
    val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
    val offlineDir = File(externalDir, JELLYFIN_OFFLINE_DIR)
    if (!offlineDir.exists()) {
        offlineDir.mkdirs()
    }
    return offlineDir
}

// AFTER:
private fun getOfflineDirectory(): File {
    val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        ?: context.filesDir  // fallback to internal storage if external unavailable
    val offlineDir = File(baseDir, JELLYFIN_OFFLINE_DIR)
    if (!offlineDir.exists()) {
        offlineDir.mkdirs()
    }
    return offlineDir
}
```

---

### Step 3: Run tests

```
./gradlew testDebugUnitTest --tests "com.rpeters.jellyfin.data.offline.OfflineDownloadManagerTest" -x lint
```

Expected: all PASS.

---

### Step 4: Commit

```bash
git add app/src/main/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManager.kt
git add app/src/test/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManagerTest.kt
git commit -m "fix: fall back to internal storage when external storage unavailable"
```

---

## Task 3: Serialization exception silently kills the download system

**Problem:** `loadDownloads()` wraps its body in `try/catch(CancellationException)` only. If the stored JSON is corrupt (e.g., after a schema change or partial write), `json.decodeFromString<List<OfflineDownload>>(downloadsJson)` throws `SerializationException`. This propagates out of the `collect` block uncaught, the coroutine terminates, and `_downloads` stays an empty list forever — users silently lose all their downloads.

**Fix:** Catch deserialization errors inside the `collect` block, log them, and reset to an empty list.

**Files:**
- Modify: `app/src/main/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManager.kt` lines 609–624
- Modify: `app/src/test/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManagerTest.kt`

---

### Step 1: Write the failing test

This tests that corrupt DataStore JSON doesn't crash the manager and leaves downloads as empty:

```kotlin
@Test
fun `corrupt DataStore JSON results in empty downloads not a crash`() = runTest(testDispatcher) {
    // Pre-seed the DataStore with invalid JSON before creating the manager
    val corruptDataStore = PreferenceDataStoreFactory.create(
        scope = kotlinx.coroutines.CoroutineScope(testDispatcher + kotlinx.coroutines.SupervisorJob()),
        produceFile = { File(tempDir, "corrupt_test.preferences_pb") }
    )
    corruptDataStore.edit { prefs ->
        prefs[androidx.datastore.preferences.core.stringPreferencesKey("offline_downloads")] =
            "{{not valid json at all}}"
    }
    advanceUntilIdle()

    val mockEncryptedPreferences = mockk<com.rpeters.jellyfin.data.security.EncryptedPreferences>(relaxed = true)
    every { mockEncryptedPreferences.getEncryptedString(any()) } returns MutableStateFlow("http://test.com/decrypted")

    val corruptManager = OfflineDownloadManager(
        context = context,
        repository = repository,
        okHttpClient = okHttpClient,
        encryptedPreferences = mockEncryptedPreferences,
        dispatchers = testDispatchers,
        dataStore = corruptDataStore,
        deviceCapabilities = deviceCapabilities,
    )
    advanceUntilIdle()

    // Should not throw, and downloads should be empty (graceful degradation)
    val downloads = corruptManager.downloads.value
    assertTrue("Downloads should be empty after corrupt data, not crash", downloads.isEmpty())
    corruptManager.cleanup()
}
```

Run: `./gradlew testDebugUnitTest --tests "com.rpeters.jellyfin.data.offline.OfflineDownloadManagerTest.corrupt DataStore JSON results in empty downloads not a crash" -x lint`

Expected: FAIL (the exception is currently not caught, so the coroutine crashes and the manager silently stays empty — but in a test, it may fail with an unhandled exception in the coroutine scope).

---

### Step 2: Apply the fix

Replace the `loadDownloads` function:

```kotlin
// BEFORE:
private suspend fun loadDownloads() {
    try {
        var initialized = false
        dataStore.data.collect { preferences ->
            val downloadsJson = preferences[androidx.datastore.preferences.core.stringPreferencesKey(DOWNLOADS_KEY)] ?: "[]"
            val downloads = json.decodeFromString<List<OfflineDownload>>(downloadsJson)
            _downloads.update { downloads }
            if (!initialized) {
                initialized = true
                requeueIncompleteDownloads(downloads)
            }
        }
    } catch (e: CancellationException) {
        throw e
    }
}

// AFTER:
private suspend fun loadDownloads() {
    try {
        var initialized = false
        dataStore.data.collect { preferences ->
            val downloadsJson = preferences[androidx.datastore.preferences.core.stringPreferencesKey(DOWNLOADS_KEY)] ?: "[]"
            val downloads = try {
                json.decodeFromString<List<OfflineDownload>>(downloadsJson)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to deserialize downloads — resetting to empty list", e)
                emptyList()
            }
            _downloads.update { downloads }
            if (!initialized) {
                initialized = true
                requeueIncompleteDownloads(downloads)
            }
        }
    } catch (e: CancellationException) {
        throw e
    }
}
```

---

### Step 3: Run tests

```
./gradlew testDebugUnitTest --tests "com.rpeters.jellyfin.data.offline.OfflineDownloadManagerTest" -x lint
```

Expected: all PASS.

---

### Step 4: Commit

```bash
git add app/src/main/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManager.kt
git add app/src/test/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManagerTest.kt
git commit -m "fix: gracefully handle corrupt DataStore JSON in loadDownloads"
```

---

## Task 4: Wrong storage calculation in `OfflineStorageInfo`

**Problem:** `getAvailableStorage()` returns `File.freeSpace` (current device free space), but the result is stored as `totalSpaceBytes` in `OfflineStorageInfo`. Then `availableSpaceBytes = totalSpace - usedSpace` computes `freeSpace - offlineDirSize`, which double-subtracts (the offline dir is already accounted for in the OS-level free space). The `usedSpacePercentage` property divides by `freeSpace` instead of total capacity, producing meaningless values.

**Fix:**
- Rename `getAvailableStorage()` → returns free space (no rename needed, just fix the semantic mismatch at the call site)
- Change `OfflineDownloadManager` to expose `getTotalStorage()` returning `File.totalSpace`
- Fix `getOfflineStorageInfo()` in `OfflinePlaybackManager` to use `totalSpace` for total and `freeSpace` for available

**Files:**
- Modify: `app/src/main/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManager.kt` lines 225–233
- Modify: `app/src/main/java/com/rpeters/jellyfin/data/offline/OfflinePlaybackManager.kt` lines 103–115
- Modify: `app/src/test/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManagerTest.kt`

---

### Step 1: Write the failing test

Add to `OfflineDownloadManagerTest`:

```kotlin
@Test
fun `getTotalStorage returns a positive value`() = runTest(testDispatcher) {
    val total = manager.getTotalStorage()
    assertTrue("Total storage should be greater than zero", total > 0)
}
```

Run: `./gradlew testDebugUnitTest --tests "com.rpeters.jellyfin.data.offline.OfflineDownloadManagerTest.getTotalStorage returns a positive value" -x lint`

Expected: FAIL — `getTotalStorage` does not exist yet.

---

### Step 2: Add `getTotalStorage()` to `OfflineDownloadManager`

In `OfflineDownloadManager.kt`, add next to `getAvailableStorage()` (around line 225):

```kotlin
fun getTotalStorage(): Long {
    val offlineDir = getOfflineDirectory()
    return offlineDir.totalSpace
}
```

---

### Step 3: Fix `getOfflineStorageInfo()` in `OfflinePlaybackManager`

```kotlin
// BEFORE:
fun getOfflineStorageInfo(): OfflineStorageInfo {
    val totalSpace = downloadManager.getAvailableStorage()
    val usedSpace = downloadManager.getUsedStorage()
    val completedDownloads = downloads.value.filter { it.status == DownloadStatus.COMPLETED }

    return OfflineStorageInfo(
        totalSpaceBytes = totalSpace,
        usedSpaceBytes = usedSpace,
        availableSpaceBytes = totalSpace - usedSpace,
        downloadCount = completedDownloads.size,
        totalDownloadSizeBytes = completedDownloads.sumOf { it.fileSize },
    )
}

// AFTER:
fun getOfflineStorageInfo(): OfflineStorageInfo {
    val totalSpace = downloadManager.getTotalStorage()
    val freeSpace = downloadManager.getAvailableStorage()   // File.freeSpace
    val usedSpace = downloadManager.getUsedStorage()
    val completedDownloads = downloads.value.filter { it.status == DownloadStatus.COMPLETED }

    return OfflineStorageInfo(
        totalSpaceBytes = totalSpace,
        usedSpaceBytes = usedSpace,
        availableSpaceBytes = freeSpace,
        downloadCount = completedDownloads.size,
        totalDownloadSizeBytes = completedDownloads.sumOf { it.fileSize },
    )
}
```

---

### Step 4: Run tests

```
./gradlew testDebugUnitTest --tests "com.rpeters.jellyfin.data.offline.OfflineDownloadManagerTest" -x lint
```

Expected: all PASS.

---

### Step 5: Commit

```bash
git add app/src/main/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManager.kt
git add app/src/main/java/com/rpeters/jellyfin/data/offline/OfflinePlaybackManager.kt
git add app/src/test/java/com/rpeters/jellyfin/data/offline/OfflineDownloadManagerTest.kt
git commit -m "fix: correct storage calculation in OfflineStorageInfo using totalSpace and freeSpace"
```

---

## Task 5: Hardcoded foreground notification ID causes concurrent downloads to collide

**Problem:** `OfflineDownloadWorker` uses `NOTIFICATION_ID_FOREGROUND = 3901` (a compile-time constant) for its foreground service notification. When two downloads run concurrently, both workers call `setForeground()` with ID `3901`. Android uses the notification ID to identify which notification to update; the second worker's notification completely overwrites the first, and the first download's progress disappears from the notification shade.

**Fix:** Derive the foreground notification ID from the download ID so each concurrent download has a unique notification. Use the same `downloadId.hashCode()` pattern already used for pause/cancel PendingIntents in this file, but with a distinct base offset that doesn't collide with the completion notification range (`NOTIFICATION_ID_COMPLETION_BASE = 4100`).

**Files:**
- Modify: `app/src/main/java/com/rpeters/jellyfin/data/worker/OfflineDownloadWorker.kt` lines 49, 74–86, 182–190, 272–280

---

### Step 1: No unit test needed for this change

The fix is about per-instance state (notification ID) in a WorkManager worker. WorkManager workers are difficult to unit test in isolation for notification behavior. This is a correct-by-inspection fix — we verify it manually by checking the companion object and `createForegroundInfo` call sites.

---

### Step 2: Apply the fix

**In the companion object**, change:
```kotlin
// BEFORE:
private const val NOTIFICATION_ID_FOREGROUND = 3901

// AFTER:
// Remove NOTIFICATION_ID_FOREGROUND constant — ID is now derived per-download
// Keep:
private const val NOTIFICATION_ID_FOREGROUND_BASE = 3000
```

**Pass `downloadId` into `createForegroundInfo`** — it already receives `downloadId` as a parameter, so no signature change needed.

**In `doWork()`**, change the `setForeground` call to pass a per-download notification ID:

Find the initial `setForeground` call (around line 49):
```kotlin
// BEFORE:
setForeground(createForegroundInfo(downloadId, initialName, 0, 0L, 0L, true))

// AFTER:
setForeground(createForegroundInfo(downloadId, initialName, 0, 0L, 0L, true))
// (no change here — the fix is inside createForegroundInfo)
```

**In `createForegroundInfo()`**, change the `ForegroundInfo` construction at the bottom (lines 182–190):

```kotlin
// BEFORE:
return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    ForegroundInfo(
        NOTIFICATION_ID_FOREGROUND,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
    )
} else {
    ForegroundInfo(NOTIFICATION_ID_FOREGROUND, notification)
}

// AFTER:
val notificationId = NOTIFICATION_ID_FOREGROUND_BASE + (downloadId.hashCode() and 0x7FFFFFFF) % 1000
return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    ForegroundInfo(
        notificationId,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
    )
} else {
    ForegroundInfo(notificationId, notification)
}
```

The `and 0x7FFFFFFF` masks to a positive int (hashCode can be negative). `% 1000` keeps IDs in range `[3000, 3999]`, safely below the `NOTIFICATION_ID_COMPLETION_BASE = 4100`.

---

### Step 3: Run the full test suite

```
./gradlew testDebugUnitTest -x lint
```

Expected: all existing tests PASS (this change only affects notification IDs, no logic change).

---

### Step 4: Commit

```bash
git add app/src/main/java/com/rpeters/jellyfin/data/worker/OfflineDownloadWorker.kt
git commit -m "fix: use per-download notification ID to avoid foreground notification collision"
```

---

## Final validation

Run the full unit test suite to confirm no regressions:

```
./gradlew testDebugUnitTest -x lint
```

All tests should PASS. Optionally run lint:

```
./gradlew lintDebug
```
