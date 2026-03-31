# Design Spec: Cinefin Unified Upgrade Plan (Stabilization & MVI Evolution)

**Date**: 2026-03-30  
**Status**: Draft  
**Target**: Stabilization (A) + Architectural Evolution (C)  
**Architectural Choice**: Strict MVI ("Clean Break") via Orbit MVI

---

## 1. Overview
This plan synthesizes existing stabilization efforts with a major architectural shift from MVVM to a strict MVI (Model-View-Intent) pattern. The goal is to resolve "documentation drift," stabilize critical dependencies, and address the maintenance burden of oversized composables (1000+ lines) by enforcing a predictable state-machine architecture.

---

## 2. Architecture: Orbit MVI (:core:architecture)

We will introduce a new `:core:architecture` module to house the foundation for the "Clean Break" migration.

### 2.1 Core Components
- **Orbit MVI**: Chosen for its balance of strictness and boilerplate reduction.
- **BaseMviViewModel**: An abstract class that implements `ContainerHost`. It will manage a single `UiState` and a stream of `SideEffects`.
- **Immutable State**: ViewModels will no longer use multiple `MutableStateFlow` variables. All UI state will reside in a single data class.
- **SideEffects**: One-time events (Navigation, Toasts, Haptics) will be handled via `postSideEffect` to prevent "double-fire" issues.

### 2.2 UI Integration
- **State Collection**: `collectAsStateWithLifecycle()` in Compose.
- **SideEffect Collection**: `collectSideEffect { ... }` for one-time events.
- **Intent Dispatch**: UI will call `viewModel.onIntent(Intent)` rather than calling multiple ViewModel functions directly.

---

## 3. High-Impact Migration (Home & Player)

The `HomeScreen` and `VideoPlayerScreen` are the primary targets for the MVI transition.

### 3.1 Forced Refactor of Oversized Composables
- **Stateless Sub-components**: The MVI transition will be used to break down 1000+ line screens into smaller, stateless components (e.g., `PlaybackOverlay.kt`, `QualitySelector.kt`).
- **Standardized Callbacks**: Sub-components will accept `UiState` and a single `(Intent) -> Unit` callback, drastically reducing lambda-drilling.

---

## 4. Phased Roadmap

### Phase 0: Documentation Alignment (Week 1)
- **Goal**: Create a single "Source of Truth" for app status.
- **Action**: Consolidate `CURRENT_STATUS.md`, `KNOWN_ISSUES.md`, and `ROADMAP.md` into a single table in `docs/plans/CURRENT_STATUS.md`.
- **Exit Criteria**: No contradictions between user-facing planning docs.

### Phase 1: Architecture Core (Week 2)
- **Goal**: Implement the MVI foundation.
- **Action**: Create `:core:architecture` and implement `BaseMviViewModel` with Orbit MVI.
- **Exit Criteria**: A working "Hello MVI" screen/test verifying the pattern.

### Phase 2: High-Impact Migration (Weeks 3-6)
- **Goal**: Migrate Home and Player screens.
- **Action**: 
  1. Refactor `VideoPlayerViewModel` and `HomeScreenViewModel` to Orbit MVI.
  2. Break down `VideoPlayerScreen.kt` and `HomeScreen.kt` into stateless components.
- **Exit Criteria**: Home and Player screens fully migrated with 100% functional parity.

### Phase 3: Dependency Stabilization (Weeks 7-8)
- **Goal**: Shrink the "Alpha Surface Area" by 30-40%.
- **Action**: Move Media3, Navigation, and Lifecycle libraries to stable versions.
- **Exit Criteria**: No critical alpha/RC dependencies in the core playback/navigation stack.

---

## 5. Testing & Verification Strategy

- **Playback Regression Matrix**: Every MVI refactor must pass a verification suite (Direct Play, Transcode Fallback, Subtitle/Audio Switching).
- **TV Focus Audit**: Any change to navigation or material libraries requires a D-pad audit on the Android TV home screen.
- **Screenshot Regression**: Capture baseline screenshots of Home/Player before migration to detect layout shifts.
- **Unit Testing**: 70%+ coverage for new MVI ViewModels, testing State and SideEffect streams in isolation.

---

## 6. Success Metrics
- **State Predictability**: Zero "double-toast" or "lost state" bugs on configuration changes.
- **Code Quality**: `VideoPlayerScreen.kt` and `HomeScreen.kt` reduced in size by at least 40% (via extraction).
- **Stability**: Dependency surface area reduced with zero regressions in core playback metrics.
