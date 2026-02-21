# Android TV Modernization Plan (Material TV 3)

This document outlines the strategy and step-by-step plan to modernize the Cinefin Android TV experience using the latest `androidx.tv.material3` components. The goal is to create a cinematic, fluid, and immersive UI that matches the "Material 3 Expressive" design language used in the mobile/tablet versions while adhering to TV-specific ergonomics.

## 🎯 High-Level Goals
1.  **Immersive Experience:** Use high-quality backdrops and smooth transitions to make the app feel alive.
2.  **Navigation Excellence:** Implement a standard TV Side Navigation Drawer.
3.  **Component Modernization:** Replace custom TV components with official `androidx.tv.material3` ones (Carousel, StandardCardLayout, etc.).
4.  **D-pad Optimization:** Ensure perfect focus management and intuitive remote control navigation.
5.  **Visual Polish:** Add shadows, glow effects on focus, and consistent spacing.

---

## 🛠 Phase 1: Foundation & Navigation
*Goal: Establish the core layout and navigation structure.*

- [x] **Dependency Check:** Verify and update `androidx.tv:tv-material` to the latest stable/alpha version.
- [x] **Side Navigation Drawer:**
    - [x] Implement `androidx.tv.material3.NavigationDrawer` in `TvJellyfinApp.kt`.
    - [x] Define main destinations: Home, Movies, TV Shows, Music, Search, Settings.
- [x] **Global Focus Styles:**
    - [x] Define a consistent "Focus Scale" (e.g., 1.1x) and "Focus Border/Glow".
    - [x] Update `JellyfinAndroidTheme` to provide TV-specific Material 3 color schemes.
- [x] **App Structure Refactor:**
    - [x] Ensure `TvNavGraph` supports seamless switching between drawer items.

## 🏠 Phase 2: Home Screen (The Cinema Hall)
*Goal: Create a stunning first impression with featured content.*

- [x] **Hero Carousel:**
    - [x] Implement `androidx.tv.material3.Carousel` at the top of the Home screen.
    - [x] Populate with "Featured" or "Recently Added" items with large backdrops.
    - [x] Add "Play" and "More Info" buttons over the carousel items.
- [x] **Immersive Backgrounds:**
    - [x] Implement a global background state that updates when an item in a row is focused.
    - [x] Use a blurred/dimmed version of the focused item's backdrop as the screen background.
- [x] **Modern Content Rows:**
    - [x] Refactor `TvContentCarousel` to use `StandardCardLayout`.
    - [x] Implement horizontal scrolling rows with proper "overscan" padding.
    - [x] Add "Section Headers" with Material 3 Typography.

## 🎬 Phase 3: Detail Screen Overhaul
*Goal: Provide rich information and quick actions for media.*

- [x] **Immersive Layout:**
    - [x] Use a full-screen backdrop with a gradient overlay.
    - [x] Display large Title, Metadata (Year, Rating, Duration), and Overview.
- [x] **Action Bar:**
    - [x] Modern TV Buttons for Play, Trailer, Favorite, and Mark as Watched.
    - [x] Clear focus indicators for all actions.
- [x] **Related Content:**
    - [x] Add "Similar Items" or "Cast" rows below the main info section.
    - [x] For TV Shows: Implement an Episode/Season selector optimized for D-pad.

## 📺 Phase 4: Video Player Experience
*Goal: Minimalist yet powerful controls.*

- [x] **TV Player Controls:**
    - [x] Implement larger, TV-optimized seek bar and buttons.
    - [x] Add a "Quick Settings" menu (Subtitles, Audio Tracks, Quality) using a slide-in side drawer.
    - [x] Support D-pad "Down" for settings.
- [x] **Playback UI:**
    - [x] Show clear progress, remaining time, and currently playing title.
    - [x] Implement modern control surfaces with glassmorphism effects.

## 🔍 Phase 5: Library, Search & Polish
*Goal: Ease of discovery and final refinements.*

- [x] **Library Grids:**
    - [x] Optimize `TvLibraryScreen` with a responsive grid that handles hundreds of items smoothly.
    - [x] Integrated immersive backgrounds into library browsing.
- [x] **Search:**
    - [x] TV-friendly search interface with large input and content filters.
    - [x] Implemented dedicated `TvSearchScreen`.
- [x] **Animations & Transitions:**
    - [x] Added scale animations (1.1x) and glow effects on focus across all cards.
    - [x] Smooth cross-fade transitions for immersive backgrounds.

---

## 📈 Tracking Progress

| Component | Status | Priority | Notes |
| :--- | :--- | :--- | :--- |
| **Side Navigation** | ✅ Done | High | Implemented with ModalNavigationDrawer. |
| **Hero Carousel** | ✅ Done | High | Uses Material 3 TV Carousel. |
| **Immersive Background** | ✅ Done | Medium | Cross-fading blurred backdrops. |
| **StandardCardLayout** | ✅ Done | Medium | Official M3 TV cards implemented. |
| **Detail Screen Redesign** | ✅ Done | High | Immersive layout with season/episode selector. |
| **Video Player** | ✅ Done | High | Modern controls with side settings drawer. |
| **Library & Search** | ✅ Done | Medium | Optimized grids and TV-friendly search. |
| **D-pad Navigation Audit** | ✅ Done | Critical | Verified across all new screens. |

---

## 📝 Final Implementation Notes
- **Performance:** Used lower-resolution images for blurred backgrounds to maintain 60fps.
- **Visuals:** Followed "Material 3 Expressive" guidelines while respecting TV "Lean-back" principles.
- **Future Work:** Consider adding Voice Search integration and Live TV guide improvements.
