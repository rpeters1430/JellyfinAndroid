# 🎯 Jellyfin Android App - Improvement Plan & Bug Report
**Date:** December 10, 2025  
**Status Update:** December 13, 2025  
**Analyzed Version:** Material 3 Expressive (1.5.0-alpha10) + Compose BOM 2025.12.00  
**Review Focus:** M3 Expressive compatibility, performance, architecture, and bugs

---

## 🔎 Executive Summary

Your Jellyfin Android app remains well-architected with Hilt DI, modern Compose patterns, and solid security practices. Since the December 10 review, several critical items were already addressed in the codebase.

**Key Findings (updated):**
- Critical fixes now present: stable LazyList keys, themed gradients, `AspectRatioMode.entries`, press-state wiring, typed motion tweens, and filled cards.
- NavGraph modularized into feature graphs (auth, home/library, media, profile/settings, detail); root graph is minimal (31 lines), but Media (~322 lines) and Detail (~456 lines) could be further trimmed.
- Image loading is consistent (`OptimizedImage` in ExpressiveCarousel).
- Experimental API opt-ins remain scattered.
- Technical debt persists: hardcoded `"Unknown"` strings, magic dimensions, duplicate `PerformanceMonitor` implementations.

**Overall Health Score:** 7.2/10 (up from 6.5/10 in October 2025)

---

## 🚦 Active Bugs & Issues (current)

### HIGH (Address This Sprint)
1) **Scattered experimental opt-ins**  
   - Central opt-in wrapper added (`OptInAppExperimentalApis`); remaining screens should adopt it to replace local `@OptIn` usages.

### LOW (Technical Debt)
2) Hardcoded `"Unknown"` strings across UI/utilities -> use `stringResource(R.string.unknown)`.  
3) Magic numbers for spacing (`16.dp`, `12.dp`, `8.dp`) -> centralize in a Dimensions object.  
4) Duplicate `PerformanceMonitor` implementations (`utils/PerformanceMonitor.kt`, `ui/utils/PerformanceMonitor.kt`) -> merge into one Hilt-injectable.

---

## ✅ Fixed Since December 10 Review
- **Stable keys added** to flagged LazyList/LazyRow usages (Home, Music, TV, Library, Offline, Season, Movie detail, etc.).
- **Gradients themed**: overlays now use `MaterialTheme.colorScheme.scrim` in carousel/cards.
- **Aspect ratio enum** uses `AspectRatioMode.entries` (`VideoPlayerViewModel`).
- **Press-state wiring**: `onPressedChange` now used in expressive cards.
- **Empty Row removed** from ExpressiveHeroCard.
- **Motion tokens** include typed tween variants (Dp, Color, Int) in `Motion.kt`.
- **Filled cards** implemented with M3 colors; duplicate `MediaType` enum removed.
- **ExpressiveCarousel** now uses `OptimizedImage` (was `AsyncImage`) and indicators observe `PagerState` directly.
- **NavGraph** partially modularized (auth, home/library, profile/settings) and reduced to ~813 lines.

---

## ✨ Material 3 Expressive Improvements (still relevant)

1) Use M3 **MotionScheme** when available; align motion tokens with official easing.  
2) Implement **expressive emphasized easing** set (accelerate/decelerate variants).  
3) Add **wide FAB** support for tablets/foldables.  
4) Adopt **expressive loading states** instead of generic `CircularProgressIndicator`.

---

## 📌 Improvement Priorities

### Phase 1: Code Quality (1-2 Days)
1. Add centralized experimental `@OptIn` file; refactor usages.  
2. Simplify carousel indicators to observe `PagerState` directly.

### Phase 2: Technical Debt (3-5 Days)
1. Localize `"Unknown"` strings.  
2. Centralize common dimensions.  
3. Merge duplicate `PerformanceMonitor` implementations.

### Phase 3: M3 Expressive Polish (1-2 Weeks)
1. Align MotionScheme/easing tokens with latest guidance.  
2. Enhance loading states with M3 expressive patterns.  
3. Add wide FAB support for large screens.

### Phase 4: Feature Completion (2-4 Weeks)
- Finish TODOs for play, queue, download, cast, favorite, and share functionality.

---

## 📊 Metrics Summary

| Metric | Current | Target |
|--------|---------|--------|
| LazyList items with keys | ~100% | 100% |
| Hardcoded colors | 0 (gradients fixed) | 0 |
| TODO items | 40+ | 0 |
| NavGraph.kt lines | 31 (root; Media 322, Detail 456) | <300 per file |
| Test coverage | ~5% | 30%+ |
| M3 Expressive utilization | ~65% | 90% |

---

## 🧭 Recommended Next Steps

1. Open issues for the outstanding High/Low items above.  
2. Add centralized experimental `@OptIn` file and refactor usages.  
3. Refine carousel indicators to observe `PagerState` directly.  
4. Track M3 Expressive API changes and update motion/expressive patterns accordingly.

