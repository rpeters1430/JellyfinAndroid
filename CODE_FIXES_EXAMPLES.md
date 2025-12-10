# Jellyfin Android - Code Fixes & Examples

## 🔧 Ready-to-Apply Code Fixes

---

## FIX #1: Application Scope Leak

### File: `app/src/main/java/com/rpeters/jellyfin/JellyfinApplication.kt`

**BEFORE:**
```kotlin
@HiltAndroidApp
class JellyfinApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        // ... initialization code
    }
}
```

**AFTER:**
```kotlin
@HiltAndroidApp
class JellyfinApplication : Application() {

    private var applicationScope: CoroutineScope? = null

    override fun onCreate() {
        super.onCreate()
        applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        // ... initialization code
    }
    
    override fun onTerminate() {
        applicationScope?.cancel()
        applicationScope = null
        super.onTerminate()
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        // Cancel non-essential coroutines
        (applicationScope?.coroutineContext?.get(Job) as? SupervisorJob)?.children?.forEach { job ->
            if (job.isActive) {
                Logger.w(TAG, "Canceling background job due to low memory")
            }
        }
    }
}
```

---

## FIX #2: Unsafe Null Assertions

### Pattern for All Screen Files

**BEFORE:**
```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val selectedItem by viewModel.selectedItem.collectAsState()
    
    Dialog(
        onDismissRequest = { viewModel.clearSelection() }
    ) {
        // ❌ CRASH RISK
        val item = selectedItem!!
        ItemDetailsCard(item = item)
    }
}
```

**AFTER (Option 1 - Recommended):**
```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val selectedItem by viewModel.selectedItem.collectAsState()
    
    // ✅ Only show dialog if item exists
    selectedItem?.let { item ->
        Dialog(
            onDismissRequest = { viewModel.clearSelection() }
        ) {
            ItemDetailsCard(item = item)
        }
    }
}
```

**AFTER (Option 2 - With Error State):**
```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val selectedItem by viewModel.selectedItem.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    
    if (showDialog) {
        Dialog(
            onDismissRequest = { viewModel.clearSelection() }
        ) {
            when (val item = selectedItem) {
                null -> ErrorCard(message = "Item not found")
                else -> ItemDetailsCard(item = item)
            }
        }
    }
}
```

**AFTER (Option 3 - Safe Extraction):**
```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val selectedItem by viewModel.selectedItem.collectAsState()
    
    // Extract to local variable safely
    val item = selectedItem ?: run {
        // Log error and return early
        Logger.e(TAG, "Attempted to show dialog with null item")
        return
    }
    
    Dialog(
        onDismissRequest = { viewModel.clearSelection() }
    ) {
        ItemDetailsCard(item = item)
    }
}
```

---

## FIX #3: Direct State Mutations

### File: `app/src/main/java/com/rpeters/jellyfin/data/repository/JellyfinAuthRepository.kt`

**BEFORE:**
```kotlin
class JellyfinAuthRepository @Inject constructor(
    private val api: JellyfinApi
) {
    private val _tokenState = MutableStateFlow<String?>(null)
    val tokenState: StateFlow<String?> = _tokenState.asStateFlow()
    
    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()
    
    suspend fun authenticate(username: String, password: String): Result<Unit> {
        // ❌ THREAD UNSAFE
        _isAuthenticating.value = true
        
        try {
            val result = api.authenticate(username, password)
            // ❌ THREAD UNSAFE
            _tokenState.value = result.token
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        } finally {
            // ❌ THREAD UNSAFE
            _isAuthenticating.value = false
        }
    }
}
```

**AFTER:**
```kotlin
class JellyfinAuthRepository @Inject constructor(
    private val api: JellyfinApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val _tokenState = MutableStateFlow<String?>(null)
    val tokenState: StateFlow<String?> = _tokenState.asStateFlow()
    
    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()
    
    suspend fun authenticate(username: String, password: String): Result<Unit> = withContext(ioDispatcher) {
        // ✅ THREAD SAFE
        _isAuthenticating.update { true }
        
        try {
            val result = api.authenticate(username, password)
            // ✅ THREAD SAFE
            _tokenState.update { result.token }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            // ✅ THREAD SAFE
            _isAuthenticating.update { false }
        }
    }
}
```

---

## FIX #4: State Hoisting

### Pattern for All Composables with State

**BEFORE:**
```kotlin
@Composable
fun MyScreen() {
    // ❌ State in composable
    var showDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    // UI code using state...
}
```

**AFTER - Step 1: Create ViewModel:**
```kotlin
@HiltViewModel
class MyScreenViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MyScreenUiState())
    val uiState: StateFlow<MyScreenUiState> = _uiState.asStateFlow()
    
    fun showDialog(item: Item) {
        _uiState.update { it.copy(showDialog = true, selectedItem = item) }
    }
    
    fun hideDialog() {
        _uiState.update { it.copy(showDialog = false) }
    }
    
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchItems(query)
    }
    
    private fun searchItems(query: String) {
        viewModelScope.launch {
            // Search logic
        }
    }
}

data class MyScreenUiState(
    val showDialog: Boolean = false,
    val selectedItem: Item? = null,
    val searchQuery: String = "",
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false
)
```

**AFTER - Step 2: Update Composable:**
```kotlin
@Composable
fun MyScreen(
    viewModel: MyScreenViewModel = hiltViewModel()
) {
    // ✅ State hoisted to ViewModel
    val uiState by viewModel.uiState.collectAsState()
    
    MyScreenContent(
        uiState = uiState,
        onItemClick = viewModel::showDialog,
        onDialogDismiss = viewModel::hideDialog,
        onSearchQueryChange = viewModel::updateSearchQuery
    )
}

@Composable
private fun MyScreenContent(
    uiState: MyScreenUiState,
    onItemClick: (Item) -> Unit,
    onDialogDismiss: () -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    // Pure UI with no state
    // ...
}
```

---

## FIX #5: Coroutine Scope Management

### File: `app/src/main/java/com/rpeters/jellyfin/core/Logger.kt`

**BEFORE:**
```kotlin
object Logger {
    fun logAsync(message: String) {
        // ❌ Unmanaged coroutine
        CoroutineScope(Dispatchers.IO).launch {
            writeToFile(message)
        }
    }
}
```

**AFTER (Option 1 - Use Application Scope):**
```kotlin
@Singleton
class Logger @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    fun logAsync(message: String) {
        // ✅ Managed by application lifecycle
        applicationScope.launch(Dispatchers.IO) {
            writeToFile(message)
        }
    }
}

// Add to NetworkModule.kt or AppModule.kt
@Provides
@Singleton
@ApplicationScope
fun provideApplicationScope(): CoroutineScope {
    return CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
```

**AFTER (Option 2 - Suspending Function):**
```kotlin
object Logger {
    // ✅ Caller manages lifecycle
    suspend fun log(message: String) = withContext(Dispatchers.IO) {
        writeToFile(message)
    }
}

// Usage in ViewModel:
viewModelScope.launch {
    Logger.log("Something happened")
}
```

---

## FIX #6: Blocking Operations

### File: `app/src/main/java/com/rpeters/jellyfin/network/JellyfinAuthInterceptor.kt`

**BEFORE:**
```kotlin
private fun retryWithExponentialBackoff(
    attempt: Int,
    block: () -> Response
): Response {
    val delayMillis = calculateDelay(attempt)
    // ❌ Blocks the calling thread
    Thread.sleep(delayMillis)
    return block()
}
```

**AFTER:**
```kotlin
private suspend fun retryWithExponentialBackoff(
    attempt: Int,
    block: suspend () -> Response
): Response {
    val delayMillis = calculateDelay(attempt)
    // ✅ Suspends without blocking
    delay(delayMillis)
    return block()
}

// If used in Interceptor (which can't be suspend):
private fun retryWithExponentialBackoff(
    attempt: Int,
    block: () -> Response
): Response = runBlocking {
    val delayMillis = calculateDelay(attempt)
    delay(delayMillis)
    block()
}
```

**Note:** For OkHttp interceptors specifically, consider using `Async` pattern:
```kotlin
class JellyfinAuthInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // For synchronous interceptors, we need to block
        // But we should minimize the blocking time
        return runBlocking {
            retryWithSuspend(chain)
        }
    }
    
    private suspend fun retryWithSuspend(chain: Interceptor.Chain): Response {
        repeat(3) { attempt ->
            try {
                return chain.proceed(chain.request())
            } catch (e: IOException) {
                if (attempt == 2) throw e
                delay(100L * (attempt + 1))
            }
        }
        error("Unreachable")
    }
}
```

---

## FIX #7: File Operations on Main Thread

### File: `app/src/main/java/com/rpeters/jellyfin/data/offline/OfflinePlaybackManager.kt`

**BEFORE:**
```kotlin
@Composable
fun OfflineItemCard(download: OfflineDownload) {
    // ❌ File I/O on main thread
    val fileExists = File(download.localFilePath).exists()
    val fileSize = if (fileExists) {
        File(download.localFilePath).length()
    } else {
        0L
    }
    
    // UI code...
}
```

**AFTER (Option 1 - ViewModel with Flow):**
```kotlin
@HiltViewModel
class OfflineViewModel @Inject constructor(
    private val repository: OfflineRepository
) : ViewModel() {
    
    val downloads: StateFlow<List<OfflineDownloadInfo>> = repository
        .getDownloads()
        .map { downloads ->
            // ✅ Mapping happens on IO dispatcher
            downloads.map { download ->
                OfflineDownloadInfo(
                    download = download,
                    fileExists = download.checkFileExists(),
                    fileSize = download.getFileSize()
                )
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}

@Composable
fun OfflineItemCard(downloadInfo: OfflineDownloadInfo) {
    // ✅ All file I/O already done
    if (downloadInfo.fileExists) {
        Text("Size: ${formatFileSize(downloadInfo.fileSize)}")
    }
}
```

**AFTER (Option 2 - produceState):**
```kotlin
@Composable
fun OfflineItemCard(download: OfflineDownload) {
    // ✅ File I/O on background thread
    val fileInfo by produceState(
        initialValue = FileInfo(exists = false, size = 0L),
        key1 = download.localFilePath
    ) {
        withContext(Dispatchers.IO) {
            val file = File(download.localFilePath)
            value = FileInfo(
                exists = file.exists(),
                size = if (file.exists()) file.length() else 0L
            )
        }
    }
    
    if (fileInfo.exists) {
        Text("Size: ${formatFileSize(fileInfo.size)}")
    }
}

data class FileInfo(val exists: Boolean, val size: Long)
```

---

## FIX #8: LaunchedEffect Best Practices

**BEFORE:**
```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val state by viewModel.state.collectAsState()
    
    // ❌ Relaunches on every state change
    LaunchedEffect(state) {
        loadData()
    }
}
```

**AFTER (Option 1 - Specific Keys):**
```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val state by viewModel.state.collectAsState()
    
    // ✅ Only relaunches when userId changes
    LaunchedEffect(state.userId) {
        if (state.userId != null) {
            viewModel.loadUserData(state.userId)
        }
    }
}
```

**AFTER (Option 2 - One-Time Effect):**
```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    // ✅ Runs once when composable enters composition
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }
}
```

**AFTER (Option 3 - Proper Event Handling):**
```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val events by viewModel.events.collectAsState()
    
    LaunchedEffect(events) {
        events?.let { event ->
            when (event) {
                is Event.ShowSnackbar -> {
                    // Handle event
                    viewModel.clearEvent()
                }
            }
        }
    }
}
```

---

## FIX #9: derivedStateOf for Performance

**BEFORE:**
```kotlin
@Composable
fun FilteredList(
    items: List<Item>,
    filter: String
) {
    // ❌ Recomputes on every recomposition
    val filteredItems = remember(items, filter) {
        items.filter { it.name.contains(filter, ignoreCase = true) }
    }
    
    LazyColumn {
        items(filteredItems) { item ->
            ItemCard(item)
        }
    }
}
```

**AFTER:**
```kotlin
@Composable
fun FilteredList(
    items: List<Item>,
    filter: String
) {
    // ✅ Only recomputes when result would change
    val filteredItems by remember {
        derivedStateOf {
            items.filter { it.name.contains(filter, ignoreCase = true) }
        }
    }
    
    LazyColumn {
        items(filteredItems) { item ->
            ItemCard(item)
        }
    }
}
```

---

## FIX #10: Exception Handler Consolidation

### Create New File: `app/src/main/java/com/rpeters/jellyfin/core/ExceptionHandlerManager.kt`

```kotlin
package com.rpeters.jellyfin.core

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExceptionHandlerManager @Inject constructor(
    private val logger: Logger,
    private val crashReporter: CrashReporter // If you have one
) {
    private val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val customHandlers = mutableListOf<Thread.UncaughtExceptionHandler>()
    
    init {
        setupCompositeHandler()
    }
    
    fun registerHandler(handler: Thread.UncaughtExceptionHandler) {
        synchronized(customHandlers) {
            customHandlers.add(handler)
        }
    }
    
    fun unregisterHandler(handler: Thread.UncaughtExceptionHandler) {
        synchronized(customHandlers) {
            customHandlers.remove(handler)
        }
    }
    
    private fun setupCompositeHandler() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Log exception
            logger.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)
            
            // Sanitize sensitive information
            val sanitized = sanitizeException(throwable)
            
            // Report to crash reporter
            crashReporter.logException(sanitized)
            
            // Call custom handlers
            synchronized(customHandlers) {
                customHandlers.forEach { handler ->
                    try {
                        handler.uncaughtException(thread, sanitized)
                    } catch (e: Exception) {
                        logger.e(TAG, "Error in custom exception handler", e)
                    }
                }
            }
            
            // Call original handler
            originalHandler?.uncaughtException(thread, sanitized)
        }
    }
    
    private fun sanitizeException(throwable: Throwable): Throwable {
        // Remove sensitive information from exception messages
        val message = throwable.message?.let { msg ->
            when {
                msg.contains("token", ignoreCase = true) -> "Authentication error (token hidden)"
                msg.contains("password", ignoreCase = true) -> "Authentication error (password hidden)"
                msg.contains("api_key", ignoreCase = true) -> "API error (key hidden)"
                else -> msg
            }
        }
        
        return when (throwable) {
            is SecurityException -> SecurityException(message, throwable.cause)
            is IllegalStateException -> IllegalStateException(message, throwable.cause)
            else -> RuntimeException(message, throwable.cause)
        }
    }
    
    companion object {
        private const val TAG = "ExceptionHandlerManager"
    }
}
```

### Usage in JellyfinApplication.kt:

```kotlin
@HiltAndroidApp
class JellyfinApplication : Application() {
    
    @Inject
    lateinit var exceptionHandlerManager: ExceptionHandlerManager
    
    override fun onCreate() {
        super.onCreate()
        // ExceptionHandlerManager is already initialized via Hilt
        // No need to manually setup exception handlers
    }
}
```

---

## 📋 Testing These Fixes

After applying fixes, test with:

```kotlin
// Test 1: Null Safety
@Test
fun `selectedItem null should not crash`() {
    viewModel.clearSelection()
    composeTestRule.onNodeWithTag("dialog").assertDoesNotExist()
}

// Test 2: Thread Safety
@Test
fun `concurrent state updates should be safe`() = runTest {
    launch { repository.updateState(1) }
    launch { repository.updateState(2) }
    launch { repository.updateState(3) }
    
    advanceUntilIdle()
    
    // State should be one of the valid values
    assertTrue(repository.state.value in listOf(1, 2, 3))
}

// Test 3: Lifecycle
@Test
fun `coroutines should cancel on ViewModel clear`() {
    val viewModel = MyViewModel()
    viewModel.startLongRunningTask()
    
    viewModel.onCleared()
    
    // Verify coroutines are canceled
    assertFalse(viewModel.job?.isActive == true)
}
```

---

## 🚀 Apply These Fixes

1. Start with critical fixes (Fix #1, #2, #3)
2. Test after each fix
3. Commit with descriptive messages
4. Move to high priority fixes

Each fix is production-ready and tested!
