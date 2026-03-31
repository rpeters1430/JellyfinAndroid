# Cinefin Unified Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Execute a unified upgrade of the Cinefin app focusing on documentation alignment, core MVI architecture implementation, and stabilization of key dependencies.

**Architecture:** We are moving from a standard MVVM pattern to a strict MVI (Model-View-Intent) pattern using the **Orbit MVI** library. This involves creating a new `:core:architecture` module to house base classes and migrating complex screens (Home/Player) to this pattern.

**Tech Stack:** 
- **Language:** Kotlin
- **Architecture:** Orbit MVI, Hilt
- **UI:** Jetpack Compose (Material 3 Expressive)
- **Media:** Media3 (ExoPlayer)

---

### Task 0: Documentation Alignment (Phase 0)

**Files:**
- Modify: `docs/plans/CURRENT_STATUS.md`
- Modify: `docs/plans/KNOWN_ISSUES.md`
- Modify: `docs/plans/ROADMAP.md`

- [ ] **Step 1: Consolidate Status into CURRENT_STATUS.md**
  Update `docs/plans/CURRENT_STATUS.md` to be the single source of truth for the project state.

- [ ] **Step 2: Update KNOWN_ISSUES.md and ROADMAP.md with links**
  Replace duplicate status sections in `KNOWN_ISSUES.md` and `ROADMAP.md` with links to `CURRENT_STATUS.md`.

- [ ] **Step 3: Commit Documentation Changes**
```bash
git add docs/plans/
git commit -m "docs: consolidate project status into single source of truth"
```

---

### Task 1: Initialize :core:architecture Module (Phase 1)

**Files:**
- Modify: `settings.gradle.kts`
- Create: `core/architecture/build.gradle.kts`
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1: Add Orbit MVI to Version Catalog**
  Add Orbit MVI dependencies to `gradle/libs.versions.toml`.
```toml
[versions]
orbit = "9.0.0"

[libraries]
orbit-core = { group = "org.orbit-mvi", name = "orbit-core", version.ref = "orbit" }
orbit-viewmodel = { group = "org.orbit-mvi", name = "orbit-viewmodel", version.ref = "orbit" }
orbit-compose = { group = "org.orbit-mvi", name = "orbit-compose", version.ref = "orbit" }
orbit-test = { group = "org.orbit-mvi", name = "orbit-test", version.ref = "orbit" }
```

- [ ] **Step 2: Include :core:architecture in Settings**
  Add `include(":core:architecture")` to `settings.gradle.kts`.

- [ ] **Step 3: Create core/architecture build script**
  Create `core/architecture/build.gradle.kts` with Android library configuration and Orbit dependencies.

- [ ] **Step 4: Sync Gradle**
  Run `./gradlew help` to ensure the new module is recognized.

- [ ] **Step 5: Commit Module Creation**
```bash
git add settings.gradle.kts core/architecture/build.gradle.kts gradle/libs.versions.toml
git commit -m "build: initialize :core:architecture module with Orbit MVI"
```

---

### Task 2: Implement Base MVI Infrastructure

**Files:**
- Create: `core/architecture/src/main/java/com/rpeters/jellyfin/core/architecture/BaseMviViewModel.kt`
- Create: `core/architecture/src/main/java/com/rpeters/jellyfin/core/architecture/MviIntent.kt`

- [ ] **Step 1: Define BaseMviViewModel**
```kotlin
package com.rpeters.jellyfin.core.architecture

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

abstract class BaseMviViewModel<S : Any, SE : Any, I : Any>(
    initialState: S
) : ViewModel(), ContainerHost<S, SE> {
    override val container = container<S, SE>(initialState)

    abstract fun onIntent(intent: I)
}
```

- [ ] **Step 2: Create unit test for BaseMviViewModel**
  Ensure the container initializes correctly and handles a mock intent.

- [ ] **Step 3: Commit Base Infrastructure**
```bash
git add core/architecture/src/main/java/
git commit -m "feat: implement BaseMviViewModel foundation"
```

---

### Task 3: Migrate VideoPlayer to MVI (Pilot Migration)

**Files:**
- Modify: `app/src/main/java/com/rpeters/jellyfin/ui/screens/video/VideoPlayerViewModel.kt`
- Modify: `app/src/main/java/com/rpeters/jellyfin/ui/screens/video/VideoPlayerScreen.kt`

- [ ] **Step 1: Define VideoPlayerUiState and VideoPlayerIntent**
  Create sealed classes/data classes for state and intents.

- [ ] **Step 2: Refactor VideoPlayerViewModel to extend BaseMviViewModel**
  Migrate individual StateFlows into the single `VideoPlayerUiState`.

- [ ] **Step 3: Refactor VideoPlayerScreen to use collectAsStateWithLifecycle**
  Update the Compose UI to observe the single state object and dispatch intents.

- [ ] **Step 4: Verify Playback Parity**
  Run manual playback tests to ensure no regressions in seeking, subtitles, or quality switching.

- [ ] **Step 5: Commit Migration**
```bash
git add app/src/main/java/com/rpeters/jellyfin/ui/screens/video/
git commit -m "refactor: migrate VideoPlayer to strict MVI pattern"
```

---

### Task 4: Dependency Stabilization (Phase 3)

**Files:**
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1: Identify and Update Alpha/RC Dependencies**
  Update Media3, Navigation, and Lifecycle to stable versions in `libs.versions.toml`.

- [ ] **Step 2: Run CI Verification**
  Run `./gradlew test lint` to ensure no breaking changes from dependency bumps.

- [ ] **Step 3: Commit Stabilization**
```bash
git add gradle/libs.versions.toml
git commit -m "build: stabilize core dependencies (Media3, Navigation, Lifecycle)"
```
