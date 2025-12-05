# Jetpack Compose December 2025 Update - Improvements Checklist

This document tracks improvements and updates based on the [Jetpack Compose December 2025 release](https://android-developers.googleblog.com/2025/12/whats-new-in-jetpack-compose-december.html).

## 🎯 High Priority Improvements

### ✅ Automatic Performance Improvements (No Action Required)
- [x] **Pausable Composition in Lazy Prefetch** - Now enabled by default
- [x] **Modifier Optimizations** - Automatic improvements to `onPlaced`, `onVisibilityChanged`
- [x] **No Deprecated API Usage** - Confirmed no usage of `Modifier.onFirstVisible`

### ✅ HIGH PRIORITY: Prevent ExoPlayer Recreation (COMPLETED)
- [x] **Task**: Prevent unnecessary ExoPlayer recreation in VideoPlayerViewModel
- **Status**: ✅ **COMPLETED** (2025-12-04)
- **Priority**: HIGH
- **Impact**: Prevents playback interruption during configuration changes (screen rotation)
- **Effort**: Low
- **Implementation**: Smart initialization logic instead of `retain()` API

#### Implementation Details
**File**: `app/src/main/java/com/rpeters/jellyfin/ui/player/VideoPlayerViewModel.kt`

**What Was Changed**:
Added smart initialization logic to `initializePlayer()` method:
1. **Reuse existing player** if already playing the same item (just seek to position)
2. **Properly release** old player when switching to a different item
3. **Prevent recreation** during configuration changes (ViewModel already survives)

**Implementation Note**:
Instead of using `retain()` API (which is for Composables), we leveraged the fact that ViewModels already survive configuration changes. The issue was that `initializePlayer()` was creating new players unnecessarily. Our fix prevents this by checking if a player already exists for the current item.

**Benefits**:
- ✅ Playback continues smoothly during screen rotation
- ✅ No recreation overhead (better performance)
- ✅ Improved user experience
- ✅ Proper resource cleanup when switching items
- ✅ Leverages existing ViewModel lifecycle

**Testing Status**:
- ⚠️ **Pending**: Manual testing required (automated tests blocked by Coil 3.x migration issues)
- [ ] Video playback continues during screen rotation
- [ ] Audio playback continues during screen rotation
- [ ] Playback position is maintained
- [ ] No memory leaks (verify with LeakCanary)
- [ ] Player state (play/pause) is preserved
- [ ] Subtitle/audio track selection is preserved

---

## ⚠️ Medium Priority Improvements

### ✅ MEDIUM PRIORITY: Update Test Configuration for Future Compatibility (COMPLETED)
- [x] **Task**: Update Compose test rules to use StandardTestDispatcher explicitly
- **Status**: ✅ **COMPLETED** (2025-12-04)
- **Priority**: MEDIUM
- **Impact**: Prevents breaking changes in future Compose versions
- **Effort**: Low

#### Implementation Details

**Files Updated**:
1. ✅ `app/src/androidTest/java/com/rpeters/jellyfin/ui/screens/settings/AppearanceSettingsScreenTest.kt`
2. ✅ `app/src/androidTest/java/com/rpeters/jellyfin/ui/components/MediaCardsTest.kt`

**Changes Made**:
```kotlin
import kotlinx.coroutines.test.StandardTestDispatcher

@get:Rule
val composeTestRule = createComposeRule(
    effectContext = StandardTestDispatcher()
)
```

**Additional Updates**:
- Added import for `StandardTestDispatcher`
- Updated class documentation to note Compose December 2025 compatibility
- Tests already use proper `waitForIdle()` patterns via ComposeTestRule

**Benefits**:
- ✅ Future-proof tests against Compose API changes
- ✅ More predictable test behavior
- ✅ Better test debugging (deterministic execution)
- ✅ Explicit dispatcher configuration

**Testing Status**:
- ⚠️ **Pending**: Automated test execution blocked by Coil 3.x migration issues in main codebase
- [ ] All existing tests still pass (blocked)
- [ ] Test execution time is acceptable (blocked)
- [ ] No flaky test behavior introduced (blocked)
- [ ] CI/CD pipeline tests pass (blocked)

---

## 💡 Nice-to-Have Improvements

### ✅ Implement SecureTextField for Password Input (COMPLETED)
- [x] **Task**: Replace OutlinedTextField with OutlinedSecureTextField for password fields
- **Status**: ✅ **COMPLETED** (2025-12-05)
- **Priority**: LOW
- **Impact**: Improved security and built-in password handling
- **Effort**: Low

#### Implementation Details

**File**: `app/src/main/java/com/rpeters/jellyfin/ui/screens/ServerConnectionScreen.kt`

**Implementation note (updated 2025-12-05)**:
- Using `OutlinedSecureTextField` with `rememberTextFieldState()` and IME “Done” submits `onConnect`.
- Manual visibility toggle and `PasswordVisualTransformation` boilerplate removed (handled by component).

**Current Code** (Around Line 33):
```kotlin
var showPassword by remember { mutableStateOf(false) }

OutlinedTextField(
    value = password,
    onValueChange = { password = it },
    visualTransformation = if (showPassword)
        VisualTransformation.None
    else
        PasswordVisualTransformation(),
    trailingIcon = {
        IconButton(onClick = { showPassword = !showPassword }) {
            Icon(
                imageVector = if (showPassword)
                    Icons.Filled.Visibility
                else
                    Icons.Filled.VisibilityOff,
                contentDescription = if (showPassword)
                    "Hide password"
                else
                    "Show password"
            )
        }
    }
)
```

**Updated Code**:
```kotlin
import androidx.compose.material3.OutlinedSecureTextField

OutlinedSecureTextField(
    value = password,
    onValueChange = { password = it },
    label = { Text("Password") },
    // Built-in secure handling with visibility toggle
)
```

**Benefits**:
- ✅ Less boilerplate code
- ✅ Consistent Material 3 password handling
- ✅ Built-in security best practices
- ✅ Automatic visibility toggle

**Note**: Verify that `OutlinedSecureTextField` is available in Material 3 v1.4+. Check if we need to update Material3 version in `gradle/libs.versions.toml`.

**Testing Checklist**:
- [ ] Password visibility toggle works
- [ ] Password is properly masked
- [ ] Accessibility works correctly
- [ ] Biometric authentication still works
- [ ] Remember password functionality intact

---

### 🔵 CONSIDER: Use HorizontalCenteredHeroCarousel for Featured Content
- [ ] **Task**: Evaluate and implement HorizontalCenteredHeroCarousel for home screen hero content
- **Status**: Not Started
- **Priority**: LOW
- **Impact**: Better visual presentation of featured/hero content
- **Effort**: Medium

#### Implementation Details

**File**: `app/src/main/java/com/rpeters/jellyfin/ui/screens/home/HomeCarousel.kt`

**Current Code** (Line 59):
```kotlin
HorizontalUncontainedCarousel(
    state = carouselState,
    modifier = Modifier
        .fillMaxWidth()
        .height(240.dp),
    itemWidth = 280.dp,
    itemSpacing = 12.dp,
    contentPadding = PaddingValues(horizontal = 16.dp),
) { index ->
    // Content
}
```

**Alternative Implementation**:
```kotlin
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel

HorizontalCenteredHeroCarousel(
    state = carouselState,
    modifier = Modifier
        .fillMaxWidth()
        .height(320.dp), // Hero content typically larger
    itemWidth = 360.dp,
    itemSpacing = 16.dp,
) { index ->
    // Content - will be centered and emphasized
}
```

**Benefits**:
- ✅ Better visual hierarchy for featured content
- ✅ Centered presentation draws attention
- ✅ Modern Material 3 design pattern

**Considerations**:
- Evaluate if hero presentation fits current UX
- May need to adjust item sizing
- Consider different carousel types for different sections

**Testing Checklist**:
- [ ] Visual design matches Material 3 guidelines
- [ ] Smooth scrolling and animations
- [ ] Works well on different screen sizes
- [ ] Accessibility for carousel navigation
- [ ] Performance with large image loading

---

### 🔵 CONSIDER: Implement Material Text AutoSize
- [ ] **Task**: Evaluate using Material Text autoSize for responsive text scaling
- **Status**: Not Started
- **Priority**: LOW
- **Impact**: Better text responsiveness across device sizes
- **Effort**: Low

#### Implementation Details

Material 3 v1.4 now supports autoSize behavior in Material Text composable.

**Use Cases**:
- Card titles that need to fit in fixed-height containers
- Media item titles with varying lengths
- Responsive labels across different screen sizes

**Example Implementation**:
```kotlin
import androidx.compose.material3.MaterialText

MaterialText(
    text = movieTitle,
    autoSize = true,
    minTextSize = 12.sp,
    maxTextSize = 20.sp,
    style = MaterialTheme.typography.titleMedium
)
```

**Files to Review**:
- `app/src/main/java/com/rpeters/jellyfin/ui/components/MediaCards.kt`
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/home/HomeCarousel.kt`
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/LibraryItemGrid.kt`

**Testing Checklist**:
- [ ] Text scales appropriately on different devices
- [ ] Accessibility font scaling still works
- [ ] No text truncation issues
- [ ] Performance is acceptable

---

## 🎨 Future Enhancements

### 🟣 FUTURE: Implement Shared Element Transitions
- [ ] **Task**: Implement shared element transitions for detail screens
- **Status**: Not Started
- **Priority**: FUTURE
- **Impact**: Modern, polished navigation UX
- **Effort**: High

#### New Animation APIs Available

**1. Dynamic Shared Elements**
Control transitions conditionally using `SharedContentConfig.isEnabled`:
```kotlin
SharedElement(
    key = movieId,
    config = SharedContentConfig(
        isEnabled = isForwardNavigation // Only animate forward
    )
) {
    MoviePoster(...)
}
```

**2. Modifier.skipToLookaheadPosition()**
Maintains final position during animations for "reveal" effects:
```kotlin
Box(
    Modifier
        .sharedElement(...)
        .skipToLookaheadPosition()
) {
    // Content
}
```

**3. Veiled Transitions**
Semi-opaque overlay during enter/exit animations:
```kotlin
AnimatedVisibility(
    visible = isVisible,
    veilConfig = VeilConfig(
        enabled = true,
        alpha = 0.5f
    )
) {
    // Content
}
```

#### Implementation Plan

**Phase 1: Basic Shared Elements**
- [ ] Poster image transitions (Library → Detail)
- [ ] Title transitions
- [ ] Basic fade animations

**Phase 2: Advanced Transitions**
- [ ] Dynamic transitions based on navigation direction
- [ ] Reveal-type transitions for detail screens
- [ ] Veiled transitions for modal content

**Phase 3: Polish**
- [ ] Gesture velocity integration with `prepareTransitionWithInitialVelocity()`
- [ ] Performance optimization
- [ ] Accessibility considerations

**Benefits**:
- ✅ Modern, iOS-like navigation experience
- ✅ Visual continuity between screens
- ✅ Professional polish to the app

**Files to Modify**:
- `app/src/main/java/com/rpeters/jellyfin/ui/navigation/NavGraph.kt`
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/MediaDetailScreen.kt`
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/home/HomeCarousel.kt`
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/LibraryItemGrid.kt`

**Testing Checklist**:
- [ ] Smooth transitions on all devices
- [ ] No frame drops during animations
- [ ] Works with back navigation
- [ ] Accessibility mode compatibility
- [ ] Deep linking compatibility
- [ ] State restoration works correctly

---

### 🟣 FUTURE: Add TimePicker Mode Switching
- [ ] **Task**: Implement TimePicker with picker/input mode switching
- **Status**: Not Started
- **Priority**: FUTURE
- **Impact**: Better time selection UX if/when needed
- **Effort**: Low

Material 3 v1.4 now supports switching between picker and input modes in TimePicker.

**Potential Use Cases**:
- Scheduled recording/download times
- Playback timer/sleep timer
- Reminder settings

**Note**: Only implement if time selection features are added to the app.

---

### 🟣 FUTURE: Explore VerticalDragHandle for Adaptive Panes
- [ ] **Task**: Evaluate VerticalDragHandle for resizing adaptive layout panes
- **Status**: Not Started
- **Priority**: FUTURE
- **Impact**: Better tablet/large screen experience
- **Effort**: Medium

Material 3 v1.4 introduces VerticalDragHandle for resizing adaptive panes.

**Potential Use Cases**:
- Tablet layout with resizable library/detail panes
- TV interface with adjustable side panels
- Foldable device optimizations

**Files to Review**:
- `app/src/main/java/com/rpeters/jellyfin/ui/adaptive/AdaptiveLayoutManager.kt`
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/adaptive/TvAdaptiveHomeContent.kt`

---

## 📊 Progress Summary

### Completed ✅
- [x] Verified no deprecated API usage (Modifier.onFirstVisible)
- [x] Automatic performance improvements (enabled by default)
- [x] **✅ Prevent ExoPlayer recreation** (High Priority - COMPLETED 2025-12-04)
- [x] **✅ Update test configuration** for StandardTestDispatcher (Medium Priority - COMPLETED 2025-12-04)

### In Progress
- [ ] 0/5 remaining improvements started

### Blocked ⚠️
- **Coil 3.x Migration Issues**: Pre-existing compilation errors block automated testing
  - `ImageLoaderFactory` API changes
  - `MemoryCache.Builder` constructor changes
  - `okHttpClient` configuration changes
  - File vs Path API changes
  - Need separate fix/PR for Coil 3.x migration

### High Priority (Do First)
1. [x] ~~Implement ExoPlayer retention~~ ✅ COMPLETED
2. [x] ~~Update test configuration for StandardTestDispatcher~~ ✅ COMPLETED
3. [ ] **Fix Coil 3.x migration issues** (blocking tests)

### Medium Priority (Do Next)
3. [ ] Evaluate SecureTextField for password input
4. [ ] Evaluate HorizontalCenteredHeroCarousel

### Low Priority (Nice to Have)
5. [ ] Implement Material Text autoSize
6. [ ] Shared element transitions
7. [ ] Other future enhancements

---

## 📝 Notes

### Dependencies
- Current Material 3 version: `1.5.0-alpha10` ✅ (Includes v1.4 features)
- Current Compose BOM: `2025.12.00` ✅ (Latest)

### Testing Strategy
- Test each improvement in isolation
- Verify on multiple device sizes (phone, tablet, TV)
- Check accessibility with TalkBack
- Profile performance with Android Profiler
- Run full test suite after each change

### Rollback Plan
- Each improvement should be implemented in a separate commit
- Use feature flags for larger changes
- Monitor crash reports after deployment

---

## 🔗 References

- [Jetpack Compose December 2025 Release Blog Post](https://android-developers.googleblog.com/2025/12/whats-new-in-jetpack-compose-december.html)
- [Material 3 Components Documentation](https://developer.android.com/develop/ui/compose/components)
- [Compose Animation Documentation](https://developer.android.com/develop/ui/compose/animation)
- [Compose Testing Documentation](https://developer.android.com/develop/ui/compose/testing)

---

**Last Updated**: 2025-12-04
**Next Review**: After each improvement is completed

