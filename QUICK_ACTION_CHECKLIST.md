# Jellyfin Android - Quick Action Checklist

## 🚨 CRITICAL FIXES (Do Today)

### 1. Fix Application Scope Leak
**File:** `app/src/main/java/com/rpeters/jellyfin/JellyfinApplication.kt`

- [ ] Line 31: Add cleanup for `applicationScope`
- [ ] Override `onTerminate()` and cancel scope
- [ ] Test that coroutines are properly canceled

```kotlin
// Add this method
override fun onTerminate() {
    applicationScope?.cancel()
    applicationScope = null
    super.onTerminate()
}
```

---

### 2. Fix Unsafe Null Assertions (!! operators)
**Priority:** CRITICAL - These can crash the app

- [ ] `HomeScreen.kt:171` - Replace `selectedItem!!` with safe call
- [ ] `LibraryTypeScreen.kt:254` - Replace `selectedItem!!` with safe call  
- [ ] `TVEpisodesScreen.kt:227` - Replace `selectedItem!!` with safe call
- [ ] `TVShowsScreen.kt:478` - Replace `selectedItem!!` with safe call
- [ ] `NavGraph.kt:659` - Replace `item!!` with safe call

**Pattern to use:**
```kotlin
// Option 1: Early return
val item = selectedItem ?: return

// Option 2: Safe handling
selectedItem?.let { item ->
    // Use item
}
```

---

### 3. Review Exception Handler Setup
**Files:**
- `JellyfinApplication.kt:123-130`
- `NetworkOptimizer.kt:42`

- [ ] Create centralized ExceptionHandlerManager
- [ ] Remove duplicate handler registrations
- [ ] Ensure proper chaining
- [ ] Test exception reporting

---

## ⚠️ HIGH PRIORITY (This Week)

### 4. Fix Direct State Mutations
**Scan these files:**

- [ ] `JellyfinAuthRepository.kt` - Lines 63, 94, 124
- [ ] All Repository classes
- [ ] All ViewModel classes

**Replace:**
```kotlin
// ❌ BAD
_state.value = newValue

// ✅ GOOD
_state.update { newValue }
```

**Quick Find:** Search for `\.value\s*=` pattern

---

### 5. Fix Coroutine Scope Leaks
**Files to check:**

- [ ] `Logger.kt:204`
- [ ] `NetworkOptimizer.kt:25`
- [ ] `MainThreadMonitor.kt:39`

**Action:** Replace with viewModelScope or properly managed scope

---

### 6. Fix Blocking Operations
**Files:**

- [ ] `JellyfinAuthInterceptor.kt:155` - Replace `Thread.sleep()` with `delay()`
- [ ] `OfflinePlaybackManager.kt:31` - Move file operations to IO dispatcher

---

## 🟡 MEDIUM PRIORITY (This Month)

### 7. State Hoisting Review
**71 violations found**

- [ ] Review `PerformanceOptimizations.kt:93`
- [ ] Review `TvPlayerControls_Backup.kt:73-74`
- [ ] Create ViewModel for each screen with state
- [ ] Move state out of composables

---

### 8. Add Lifecycle Awareness
**File:** `OfflineDownloadManager.kt:296`

- [ ] Replace raw `collect` with lifecycle-aware collection
- [ ] Use `collectAsState` in composables
- [ ] Use `repeatOnLifecycle` in fragments/activities

---

### 9. Material 3 Alpha Strategy
**Decision needed:**

- [ ] Stay on alpha and add feature flags
- [ ] Move to stable Material 3
- [ ] Create abstraction layer for alpha features

**Current:**
```toml
material3 = "1.5.0-alpha10"
composeBom = "2025.12.00"
```

---

## 📋 CODE QUALITY (Next Sprint)

### 10. Address TODO Comments
**42 TODOs found**

- [ ] Convert critical TODOs to GitHub issues
- [ ] Remove obsolete TODOs
- [ ] Add timeline for feature TODOs

---

### 11. Refactor Large Files
**Files to split:**

- [ ] `JellyfinRepository.kt` (767 lines)
- [ ] `LibraryTypeScreen.kt` (933 lines)
- [ ] `HomeScreen.kt` (573 lines)

---

### 12. Add Missing Tests
**Priority areas:**

- [ ] State management tests
- [ ] Null safety tests  
- [ ] Lifecycle tests
- [ ] Material 3 component rendering tests

---

## ✅ VERIFICATION CHECKLIST

After fixing critical issues, verify:

### App Stability
- [ ] No crashes on item selection
- [ ] No crashes on configuration changes (rotation)
- [ ] No memory leaks (run LeakCanary)
- [ ] No ANRs (test with StrictMode)

### State Management
- [ ] State survives configuration changes
- [ ] No race conditions in state updates
- [ ] Proper cleanup in ViewModels

### Material 3
- [ ] All alpha components render correctly
- [ ] Fallbacks work if alpha API fails
- [ ] Theme applies consistently

---

## 🔧 DEVELOPMENT SETUP

### Recommended Testing
1. Enable StrictMode in debug builds
2. Run with LeakCanary
3. Use Layout Inspector for recomposition
4. Profile with Android Studio Profiler

### Before Each Release
- [ ] Run all tests
- [ ] Check for memory leaks
- [ ] Verify no ANRs in testing
- [ ] Test on multiple Android versions
- [ ] Test with alpha feature flags off

---

## 📱 TESTING SCENARIOS

### Critical Path Testing
1. **App Launch**
   - [ ] Cold start
   - [ ] Warm start
   - [ ] With saved state

2. **Authentication**
   - [ ] First login
   - [ ] Remember me
   - [ ] Quick connect
   - [ ] Biometric auth

3. **Navigation**
   - [ ] Item selection
   - [ ] Deep linking
   - [ ] Back navigation
   - [ ] Configuration changes

4. **Playback**
   - [ ] Video playback
   - [ ] Audio playback
   - [ ] Cast
   - [ ] Background playback

---

## 🎯 QUICK WINS

These can be fixed in < 1 hour each:

1. ✅ Replace 6 `!!` operators (15 min)
2. ✅ Fix Application scope (10 min)
3. ✅ Add missing null checks (20 min)
4. ✅ Replace Thread.sleep with delay (5 min)
5. ✅ Add TODO to GitHub issues (15 min)

**Total Quick Wins:** ~65 minutes for major stability improvements!

---

## 📞 NEED HELP?

### Debugging Tools
- **Android Studio Profiler** - Memory, CPU, Network
- **LeakCanary** - Memory leak detection
- **Layout Inspector** - Compose hierarchy & recomposition
- **Logcat** - Filtered by app package

### Code Search Commands
```bash
# Find all !! operators
grep -rn "!!" app/src/main/java --include="*.kt" | grep -v "!="

# Find all TODO comments  
grep -rn "TODO" app/src/main/java --include="*.kt"

# Find all .value = assignments
grep -rn "\.value\s*=" app/src/main/java --include="*.kt"

# Find all Thread.sleep
grep -rn "Thread\.sleep" app/src/main/java --include="*.kt"
```

---

## 🚀 DONE!

Mark items as you complete them. Focus on Critical and High Priority items first.

Good luck! 🎉
