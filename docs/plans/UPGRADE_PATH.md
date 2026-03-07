# Jellyfin Android - Dependency Upgrade Path

**Last Updated**: March 7, 2026

This document outlines the dependency upgrade strategy, current status, and roadmap for modernizing the Jellyfin Android client's dependencies. For current feature status, see [CURRENT_STATUS.md](CURRENT_STATUS.md). For known bugs, see [KNOWN_ISSUES.md](../features/KNOWN_ISSUES.md).

---

## Current Dependency Status

### ✅ Stable & Up-to-Date Dependencies

These dependencies are on stable releases and current versions:

| Dependency | Version | Category | Notes |
|------------|---------|----------|-------|
| **Kotlin** | 2.3.10 | Language | Latest stable |
| **Compose BOM** | 2026.02.01 | UI Framework | Latest BOM |
| **Hilt** | 2.59.2 | Dependency Injection | Latest stable |
| **Kotlin Coroutines** | 1.10.2 | Async | Latest stable |
| **Retrofit** | 3.0.0 | Networking | Latest stable |
| **OkHttp** | 5.3.2 | HTTP Client | Latest stable |
| **Coil** | 3.4.0 | Image Loading | Latest stable |
| **Jellyfin SDK** | 1.8.6 | API Client | Latest stable |
| **Navigation** | 2.9.7 | Navigation | Latest stable |
| **Kotlinx Serialization** | 1.10.0 | Serialization | Latest stable |
| **Desugar JDK Libs** | 2.1.5 | Java Compatibility | Latest stable |
| **JUnit** | 4.13.2 | Testing | Latest stable |
| **MockK** | 1.14.9 | Testing | Latest stable |
| **Paging** | 3.4.1 | Paging | Latest stable |

**Action**: ✅ **None** - These dependencies are current and stable.

---

### ⚠️ Alpha/Beta Versions (By Design)

These dependencies are intentionally on alpha/beta versions for specific reasons:

#### Material 3 Suite (Alpha Required for Expressive Components)

| Dependency | Current Version | Stable Version | Reason for Alpha | Upgrade Blocker |
|------------|----------------|----------------|------------------|-----------------|
| **Material 3** | 1.5.0-alpha15 | 1.4.0 | Expressive components | Expressive only in alpha |
| **Material 3 Adaptive Navigation Suite** | 1.5.0-alpha15 | 1.4.0 | Enhanced navigation | Coupled with M3 alpha |
| **Material 3 Adaptive** | 1.3.0-alpha09 | 1.1.0 | Adaptive layouts | Enhanced features in alpha |
| **Material 3 Window Size** | 1.5.0-alpha15 | 1.4.0 | Window size classes | Coupled with M3 alpha |
| **Material 3 Expressive Components** | 1.5.0-alpha02 | N/A | Expressive design | **Not available in stable** |
| **Google Material** | 1.14.0-alpha09 | 1.13.0 | Supporting library | Coupled with M3 alpha |

**Reason**: The app uses **Material 3 Expressive components** for enhanced design fidelity. These components are only available in alpha versions. See [MATERIAL3_EXPRESSIVE.md](MATERIAL3_EXPRESSIVE.md) for details.

**Upgrade Path**: Wait for Material 3 Expressive to reach stable release (estimated mid-2026). Monitor [Compose releases](https://developer.android.com/jetpack/compose/setup#version) for stable announcements.

**Risk**: Low - Material 3 alphas are generally stable, and the app has been tested thoroughly with these versions.

**Action**: 🔒 **Block upgrade** until Expressive components reach stable.

---

#### AndroidX Alpha Dependencies

| Dependency | Current Version | Stable Version | Reason for Alpha | Can Upgrade? |
|------------|----------------|----------------|------------------|--------------|
| **Core KTX** | 1.18.0-rc01 | 1.17.0 | Latest features | ⚠️ Evaluate |
| **Activity Compose** | 1.13.0-rc01 | 1.12.1 | Latest features | ⚠️ Evaluate |
| **Biometric** | 1.4.0-alpha05 | 1.1.0 | Enhanced auth | ⚠️ Evaluate |
| **DataStore** | 1.3.0-alpha06 | 1.1.1 | Performance improvements | ⚠️ Wait for stable |
| **Window** | 1.6.0-alpha01 | 1.3.0 | Foldable support | ⚠️ Evaluate |
| **AndroidX TV Material** | 1.1.0-alpha01 | 1.0.0 | TV enhancements | ⚠️ Evaluate |
| **Lifecycle Runtime** | 2.11.0-alpha01 | 2.10.x | New lifecycle/runtime APIs | ⚠️ Evaluate |
| **Media3** | 1.10.0-beta01 | 1.9.x | New playback/cast/session updates | ⚠️ Beta rollout validation |

**Reason**: These dependencies are on alpha versions for:
- **Core KTX, Activity Compose**: Latest feature set for modern Android development
- **Biometric**: Enhanced biometric authentication APIs
- **DataStore**: Performance improvements and bug fixes
- **Window**: Better foldable device support
- **TV Material**: Enhanced TV UI components

**Upgrade Path**: Evaluate each dependency individually:
1. **Core KTX, Activity Compose**: Monitor for stability issues; consider downgrading to stable if problems occur
2. **Biometric**: Evaluate if alpha-only features are being used; downgrade if not needed
3. **DataStore**: Wait for 1.3.0 stable release (expected Q1 2026)
4. **Window**: Evaluate if alpha-only features are needed for foldables
5. **TV Material**: Keep alpha for TV feature development; evaluate when 1.1.0 stable releases

**Risk**: Low-Medium - These are AndroidX libraries with good stability, but alpha versions may have breaking changes.

**Action**: 📋 **Evaluate on case-by-case basis** - See Phase 2 below.

---

### ✅ Recently Upgraded to Stable

The following upgrades are now complete:

| Dependency | Previous Version | Current Version | Notes |
|------------|------------------|-----------------|-------|
| **Paging** | 3.4.0-rc01 | 3.4.1 | Stable release now in use |

**Action**: ✅ **No immediate upgrades pending** in this category.

---

### ✅ Resolved: Material 3 Carousel Dependency

**Status**: The app uses the official Material 3 carousel API (`HorizontalUncontainedCarousel`) via the Material 3 dependency. `ExpressiveHeroCarousel` is a wrapper around it.

**Impact**: No separate carousel dependency is required; carousel usage is fully aligned with Material 3 APIs.

**Action**: ✅ Resolved.

---

## Upgrade Strategy

### Phase 1: Quick Wins (Low Risk)

**Timeline**: Immediate
**Effort**: Minimal
**Risk**: Low

#### Upgrade Paging to Stable (Completed)

**Current**: `3.4.1` (stable)

**Status**: ✅ Completed (Jan 2026). No further action required.

---

### Phase 2: Stabilize Core Libraries (Medium Risk)

**Timeline**: Q1 2026 (as stable versions become available)
**Effort**: Medium
**Risk**: Medium

#### 2.1: Evaluate DataStore Upgrade

**Current**: `1.3.0-alpha06` → **Target**: `1.3.0` (when available)

**Justification**: DataStore is critical for app settings and credentials. Alpha versions may have bugs.

**Steps**:
1. Monitor [DataStore releases](https://developer.android.com/jetpack/androidx/releases/datastore) for 1.3.0 stable
2. Review release notes for breaking changes
3. Update dependency and run full test suite
4. Test credential storage and settings thoroughly
5. Monitor for any performance regressions

**Breaking Change Risk**: Medium - DataStore APIs may change slightly.

---

#### 2.2: Evaluate Biometric Library

**Current**: `1.4.0-alpha05` → **Target**: `1.1.0` (stable) or `1.4.0` (when available)

**Investigation Required**:
1. Search codebase for biometric API usage
2. Determine if alpha-only features are being used
3. If no alpha-specific features: downgrade to 1.1.0 stable
4. If alpha features required: wait for 1.4.0 stable

**Steps**:
```bash
# Search for biometric usage
grep -r "BiometricPrompt" app/src/main/java
grep -r "androidx.biometric" app/src/main/java
```

**Decision**: Downgrade if possible, otherwise wait for stable.

---

#### 2.3: Evaluate Window Library

**Current**: `1.6.0-alpha01` → **Target**: `1.3.0` (stable) or `1.6.0` (when available)

**Investigation Required**:
1. Determine if alpha-only features are needed for foldable support
2. Test on foldable emulators with stable version
3. If no regressions: downgrade to 1.3.0 stable

**Foldable Testing**:
- Test on Pixel Fold emulator
- Test window size class detection
- Test adaptive navigation

**Decision**: Downgrade if foldable support works with stable.

---

#### 2.4: Evaluate Core KTX and Activity Compose

**Current**:
- Core KTX: `1.18.0-rc01` → **Target**: `1.18.0` (stable)
- Activity Compose: `1.13.0-rc01` → **Target**: `1.13.0` (stable)

**Investigation Required**:
1. Review changelog for alpha-only features being used
2. Run full test suite with stable versions
3. Test app launch, activity lifecycle, and compose integration

**Decision**: Downgrade if no alpha-specific features are needed.

---

### Phase 3: Material 3 Stabilization (High Risk)

**Timeline**: Mid-2026 (estimated)
**Effort**: High
**Risk**: High

#### 3.1: Wait for Material 3 Expressive Stable

**Blocker**: Material 3 Expressive components are **not available in stable releases**.

**Tracking**:
- Monitor [Compose releases](https://developer.android.com/jetpack/compose/setup#version)
- Monitor [Material 3 roadmap](https://m3.material.io/)
- Watch for announcements in [Android Developers Blog](https://android-developers.googleblog.com/)

**When Available**:
1. Review Expressive components release notes
2. Check for breaking changes in APIs
3. Update all Material 3 dependencies together (coordinated upgrade)
4. Run extensive UI testing across all screens
5. Test themes (dark, light, AMOLED)
6. Test adaptive navigation
7. Regression test all Material 3 components used:
   - Carousels (custom implementation)
   - Pull-to-refresh
   - Navigation suite
   - Adaptive layouts
   - Window size classes

**Expected Effort**: 1-2 weeks (full UI regression testing required)

**Breaking Change Risk**: High - Material 3 alpha to stable may have API changes.

---

#### 3.2: Material 3 Adaptive Libraries

**Dependencies**:
- Material 3 Adaptive: `1.3.0-alpha09` → `TBD`
- Material 3 Adaptive Navigation Suite: `1.5.0-alpha15` → `TBD`

**Strategy**: Upgrade together with Material 3 main library in Phase 3.1.

**Testing Focus**:
- Adaptive navigation (phone, tablet, foldable)
- Window size class detection
- Navigation rail vs bottom bar switching
- Multi-pane layouts on large screens

---

### Phase 4: Dependency Cleanup

**Timeline**: After Phase 3 completion
**Effort**: Low-Medium
**Risk**: Low

#### 4.1: Material 3 Carousel Usage (Resolved)

**Status**: Official Material 3 carousel API (`androidx.compose.material3.carousel`) is in use via `ExpressiveHeroCarousel`.

**Action**: No migration required. Keep the wrapper and monitor future API changes.

---

#### 4.2: Monitor Deprecated Dependencies

**Watch for deprecation notices**:
- OkHttp logging interceptor (may be replaced by platform tools)
- Cast framework (may be replaced by Media3 Cast)
- Any AndroidX libraries moving to stable after long alpha periods

**Process**:
1. Review Gradle sync warnings for deprecations
2. Check release notes for migration guides
3. Plan migration when deprecated libraries reach end-of-life

---

## SDK Version Strategy

### Current SDK Configuration

| SDK Type | Current Version | Strategy |
|----------|----------------|----------|
| **compileSdk** | 36 (Android 16 Preview) | Use preview for latest features |
| **targetSdk** | 35 (Android 15) | Use stable for runtime compatibility |
| **minSdk** | 26 (Android 8.0) | Maintain for broad device support |

### Upgrade Path

#### compileSdk Upgrade Strategy
**Approach**: Aggressive - Use preview/beta SDKs for latest features
**Rationale**: Allows early adoption of new APIs, testing on upcoming Android versions
**Risk**: Low - Only affects compile-time, not runtime behavior

**Next Upgrade**: Android 16 stable (when available)

---

#### targetSdk Upgrade Strategy
**Approach**: Conservative - Use stable SDKs only
**Rationale**: `targetSdk` affects runtime behavior and Play Store requirements
**Risk**: Medium - Changes app behavior on newer Android versions

**Current**: 35 (Android 15 stable)
**Next Upgrade**: 36 when Android 16 reaches stable release

**Migration Checklist** (for targetSdk 36 upgrade):
1. Review Android 16 behavior changes
2. Test on Android 16 emulator/device
3. Update permission handling if needed
4. Test background activity restrictions
5. Verify notification behavior
6. Test storage access patterns
7. Update CI/CD to test on Android 16

---

#### minSdk Strategy
**Approach**: Conservative - Android 8.0+ (API 26)
**Rationale**: Broad device compatibility, covers 95%+ of active devices
**Risk**: N/A - No plans to change

**Decision**: Keep at 26 for foreseeable future
**Justification**:
- Core library desugaring enables Java 21 features on API 26+
- Android 8.0 released in 2017, good balance of features vs compatibility
- Lowering to API 21-25 would require significant backporting effort
- Raising to API 28+ would exclude many active devices

---

## Breaking Change Watchlist

### High-Risk Dependencies (Monitor for Breaking Changes)

| Dependency | Current | Risk | Watch For |
|------------|---------|------|-----------|
| **Material 3** | Alpha | High | API changes when moving to stable |
| **DataStore** | Alpha | Medium | Preferences API changes |
| **Compose BOM** | Latest | Medium | Composable signature changes |
| **Hilt** | Stable | Low | Annotation processor changes |
| **Kotlin** | Stable | Low | Language changes, coroutine updates |

### Breaking Change Response Process

When a breaking change is detected:

1. **Assess Impact**:
   - Review release notes
   - Check deprecation warnings in build output
   - Search codebase for affected APIs

2. **Plan Migration**:
   - Estimate effort (hours/days)
   - Identify affected files
   - Determine if migration can be gradual or must be atomic

3. **Implement**:
   - Create feature branch
   - Update code incrementally
   - Update tests
   - Verify no regressions

4. **Validate**:
   - Run full test suite
   - Perform manual testing
   - Test on multiple Android versions

5. **Document**:
   - Update UPGRADE_PATH.md with migration notes
   - Update CHANGELOG.md
   - Document any workarounds needed

---

## Dependency Update Process

### Regular Maintenance Schedule

**Weekly** (Automated via Dependabot):
- Review dependency update PRs
- Approve low-risk patch updates
- Test minor updates before merging

**Monthly**:
- Review all alpha/beta dependencies for stable releases
- Check Material 3 progress toward stable
- Review breaking change watchlist

**Quarterly**:
- Review UPGRADE_PATH.md and update status
- Re-evaluate alpha dependency usage
- Plan major version upgrades

### Update Testing Checklist

For each dependency update:

1. **Sync and Build**:
   ```bash
   ./gradlew.bat clean
   ./gradlew.bat assembleDebug
   ```

2. **Run Tests**:
   ```bash
   ./gradlew.bat testDebugUnitTest
   ./gradlew.bat connectedAndroidTest
   ```

3. **Run Lint**:
   ```bash
   ./gradlew.bat lintDebug
   ```

4. **Manual Testing** (Critical paths):
   - [ ] Login and authentication
   - [ ] Library browsing
   - [ ] Video playback
   - [ ] Search
   - [ ] Favorites
   - [ ] Settings
   - [ ] Chromecast
   - [ ] Picture-in-Picture

5. **Regression Testing** (if updating UI libraries):
   - [ ] Material 3 components render correctly
   - [ ] Dark/Light/AMOLED themes work
   - [ ] Adaptive navigation works
   - [ ] Carousel displays correctly

6. **Performance Testing** (if updating core libraries):
   - [ ] App launch time
   - [ ] Screen transition smoothness
   - [ ] Scroll performance
   - [ ] Memory usage

---

## Known Upgrade Blockers

### Cannot Upgrade Until Resolved

| Blocker | Affects | Resolution Path | ETA |
|---------|---------|----------------|-----|
| **Material 3 Expressive not stable** | All Material 3 dependencies | Wait for stable release | Mid-2026 (estimated) |
| **DataStore 1.3.0 not stable** | DataStore upgrade | Wait for stable release | Q1 2026 (estimated) |

---

## Upgrade History

### January 2026
- ✅ Kotlin: 2.2.10 → 2.3.0
- ✅ Compose BOM: 2025.08.01 → 2026.01.01
- ✅ Hilt: 2.57.1 → 2.59
- ✅ Coroutines: 1.9.0 → 1.10.2
- ✅ Media3: 1.8.0 → 1.9.1
- ✅ Navigation: 2.9.3 → 2.9.7
- ✅ Paging: 3.4.0-rc01 → 3.4.0
- ✅ Jellyfin SDK: 1.8.2 → 1.8.6
- ✅ OkHttp: 5.3.0 → 5.3.2
- ✅ Lifecycle: 2.8.0 → 2.10.0

### December 2025
- ✅ Target SDK: 34 → 35 (Android 15)
- ✅ Min SDK: 31 → 26 (broader compatibility)

---

## Quick Reference Commands

### Check for Outdated Dependencies
```bash
./gradlew.bat dependencyUpdates
```

### View Dependency Tree
```bash
./gradlew.bat app:dependencies
```

### Search for Specific Library Usage
```bash
# Example: Find Material 3 Carousel usage
grep -r "Carousel" app/src/main/java
```

### Test After Upgrade
```bash
./gradlew.bat clean assembleDebug testDebugUnitTest lintDebug
```

---

## Related Documentation

- **[CURRENT_STATUS.md](CURRENT_STATUS.md)** - Current dependency versions in use
- **[ROADMAP.md](ROADMAP.md)** - Feature development roadmap
- **[KNOWN_ISSUES.md](../features/KNOWN_ISSUES.md)** - Known bugs and workarounds
- **[CONTRIBUTING.md](../development/CONTRIBUTING.md)** - Contribution guidelines
- **[MATERIAL3_EXPRESSIVE.md](../features/MATERIAL3_EXPRESSIVE.md)** - Material 3 Expressive usage justification

---

## Summary

**Current State**: The app uses a mix of stable and alpha dependencies. All alpha dependencies are intentional and justified:
- **Material 3 alphas**: Required for Expressive components (not available in stable)
- **AndroidX alphas**: Early access to enhanced features (evaluated individually)
- **Paging**: Stable 3.4.1 now in use

**Upgrade Priority**:
1. ✅ **Phase 1**: Upgrade Paging to 3.4.x stable (completed)
2. 📋 **Phase 2**: Evaluate AndroidX alpha dependencies, downgrade to stable where possible (Q1 2026)
3. 🔒 **Phase 3**: Wait for Material 3 Expressive stable, then upgrade all M3 dependencies (mid-2026)
4. ✅ **Phase 4**: Carousel dependency resolved (official Material 3 carousel in use)

**Risk Assessment**: Overall low risk. Material 3 alphas are stable in practice, and stable fallback versions exist for most AndroidX libraries.

**Maintenance**: Regular review of dependency updates via Dependabot, monthly check for stable releases, quarterly UPGRADE_PATH review.
