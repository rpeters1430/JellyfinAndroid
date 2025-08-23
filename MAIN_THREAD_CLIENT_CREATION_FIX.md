# Critical Performance Fix - Main Thread Client Creation

## 🎯 **Root Cause Analysis**
The primary performance issue was **Jellyfin client creation happening on the main thread**, causing:
- **StrictMode DiskReadViolation** during Ktor/ServiceLoader static initialization
- **51+ frame drops** at startup
- **ZIP/JAR file I/O operations** blocking the UI thread

### **Call Stack Analysis:**
```
ServerConnectionViewModel.connectToServer()
  → JellyfinClientFactory.getClient()  ← Main thread!
    → jellyfin.createApi()
      → HttpClientJvmKt.<clinit>()  ← Static init with file I/O
        → ServiceLoader reading from JAR/ZIP
          → DiskReadViolation!
```

## ✅ **Fix 1: Async Client Creation**

### **Modified Files:**
1. **NetworkModule.kt** - JellyfinClientFactory
2. **BaseJellyfinRepository.kt** - Base client method  
3. **JellyfinAuthRepository.kt** - Auth client method
4. **JellyfinSystemRepository.kt** - System client method
5. **JellyfinRepository.kt** - Main client method

### **Key Changes:**
```kotlin
// BEFORE: Blocking main thread
fun getClient(baseUrl: String, accessToken: String?): ApiClient {
    return jellyfin.createApi(baseUrl, accessToken) // File I/O on main!
}

// AFTER: Background thread creation
suspend fun getClient(baseUrl: String, accessToken: String?): ApiClient = 
    withContext(Dispatchers.IO) {
        synchronized(clientLock) {
            if (currentToken != accessToken || currentBaseUrl != normalizedUrl || currentClient == null) {
                // Static initialization now happens on background thread
                currentClient = jellyfin.createApi(baseUrl, accessToken)
                currentBaseUrl = normalizedUrl
                currentToken = accessToken
            }
            return@synchronized currentClient ?: throw IllegalStateException("Failed to create client")
        }
    }
```

### **Repository Updates:**
All repositories now use suspend client creation:
```kotlin
// BaseJellyfinRepository.kt
protected suspend fun getClient(serverUrl: String, accessToken: String?): ApiClient =
    clientFactory.getClient(serverUrl, accessToken)

// JellyfinAuthRepository.kt  
private suspend fun getClient(serverUrl: String, accessToken: String?): ApiClient {
    return clientFactory.getClient(serverUrl, accessToken)
}
```

## ✅ **Fix 2: Stable Runtime Target**

### **Modified Files:**
- **app/build.gradle.kts** - Changed `targetSdk` from 36 to 35
- **gradle.properties** - Added compile SDK suppression

### **Configuration:**
```kotlin
// app/build.gradle.kts
android {
    compileSdk = 36  // Keep for dependencies that need it
    defaultConfig {
        targetSdk = 35  // Stable runtime behavior
    }
}
```

```properties
# gradle.properties
android.suppressUnsupportedCompileSdk=36
```

### **Benefits:**
- **Stable runtime behavior** instead of preview API changes
- **Consistent behavior** across different Android versions
- **Reduced API surface** for potential breaking changes

## 📈 **Expected Performance Improvements**

### **Before Fix:**
- ❌ **Frame drops**: "Skipped 51 frames" at startup
- ❌ **StrictMode violations**: DiskReadViolation on main thread
- ❌ **Blocking UI**: Ktor client static init on main thread
- ❌ **Preview target**: SDK 36 runtime instability

### **After Fix:**
- ✅ **Smooth startup**: No frame drops from client creation
- ✅ **Background I/O**: All file operations on Dispatchers.IO
- ✅ **Non-blocking UI**: Client creation off main thread
- ✅ **Stable runtime**: SDK 35 target with 36 compile

## 🔧 **Technical Implementation Details**

### **Thread Safety:**
- **Synchronized client creation** prevents race conditions
- **Volatile variables** ensure thread-safe visibility
- **Mutex-based locking** in auth operations

### **Background Processing:**
```kotlin
// All heavy operations moved to background
withContext(Dispatchers.IO) {
    // Jellyfin client creation
    // Ktor static initialization  
    // ServiceLoader file operations
    // JAR/ZIP reading
}
```

### **Graceful Degradation:**
- **Error handling** for client creation failures
- **Fallback mechanisms** for network issues
- **Proper cleanup** on exception scenarios

## 🚀 **Validation Steps**

### **To Verify Fix:**
1. **Check logcat** for elimination of DiskReadViolation
2. **Monitor frame drops** - should be minimal at startup
3. **Test navigation** - should be smooth and responsive
4. **Verify target SDK** - should show `target_sdk_version=35`

### **Expected Metrics:**
- **Frame drops**: <10 frames during normal startup
- **StrictMode violations**: No DiskReadViolation from client creation
- **Memory usage**: Stable baseline (no leaks from client creation)
- **Startup time**: Faster due to non-blocking initialization

## 📱 **User Experience Impact**

### **Immediate Benefits:**
- ⚡ **Instant app responsiveness** - No UI blocking during server connection
- 🔄 **Smooth transitions** - Background client initialization
- 📱 **Better performance** - Proper thread utilization
- 🎬 **Stable playback** - No initialization delays

### **Technical Benefits:**
- 🛡️ **StrictMode compliance** - Clean development experience
- 💾 **Efficient threading** - Proper separation of concerns
- 🌐 **Reliable networking** - Background client preparation
- 🔧 **Easier debugging** - Clear performance metrics

## 🎯 **Root Cause Resolution**

This fix addresses the **core architectural issue** where heavy initialization was blocking the main thread. By moving Jellyfin client creation to background threads with proper suspend functions, we:

1. **Eliminate UI blocking** during server connections
2. **Remove StrictMode violations** from file I/O operations  
3. **Improve startup performance** with async initialization
4. **Provide stable runtime** with SDK 35 targeting

The solution maintains **thread safety**, **proper error handling**, and **graceful degradation** while significantly improving app responsiveness and user experience.
