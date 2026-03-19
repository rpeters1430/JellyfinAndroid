# Cinefin TV Roadmap

> Repo-ready planning document for the Android TV / Google TV experience in Cinefin.
>
> Purpose: turn the current TV mode into a polished, TV-first product with predictable D-pad behavior, strong focus restoration, premium browsing surfaces, and a dedicated playback experience.

---

## Summary

Cinefin already has a real TV foundation in place:

- `MainActivity` branches into TV mode on television devices
- `TvJellyfinApp` provides a dedicated TV shell
- `TvNavGraph` defines TV routes
- TV-specific screens already exist for home, library, details, search, settings, server connection, and quick connect
- TV focus and remote helpers are already present

This means the TV app is **past the prototype stage**. The remaining work is mostly about:

1. tightening TV-specific architecture
2. fixing D-pad/focus consistency
3. making Home, Library, and Details feel premium on a 10-foot UI
4. rebuilding playback chrome to be TV-first
5. protecting the TV experience with tests

---

## Current assessment

### What is working

- Dedicated TV entry path exists
- Dedicated TV navigation exists
- Dedicated TV screens exist
- Immersive TV home concepts already exist
- Library browsing already works in TV mode
- TV sign-in and quick connect already exist
- There is already a reusable focus-memory system

### What still feels weak

- TV still appears to share too much infrastructure and behavior with the mobile app
- Some TV routes are still generic wrappers instead of truly content-specific surfaces
- Focus restoration likely is not yet fully consistent across drawer, rows, grids, details, search, and playback return
- Several screens work, but do not yet feel fully designed for a living-room / remote-first experience
- Playback UI likely still needs a dedicated TV overlay pass rather than incremental reuse of mobile playback controls

---

## Roadmap priorities

1. TV architecture cleanup
2. Focus and D-pad reliability
3. Home screen polish
4. Library browsing polish
5. Detail screen redesign
6. Search / settings / sign-in polish
7. TV playback overlay
8. TV UI tests and device validation

---

## Milestone 1 — TV architecture cleanup

### Goal
Create a clearer TV-only presentation layer so TV stops inheriting mobile design and navigation assumptions.

### Tasks
- [ ] Create a clearer package boundary for TV-only code
- [ ] Move TV helpers into a more consistent namespace
- [ ] Audit TV screens for accidental mobile-only UI dependencies
- [ ] Keep data/domain/viewmodel shared, but isolate TV presentation decisions
- [ ] Create a dedicated `CinefinTvTheme` if one does not already exist in a clean form
- [ ] Define TV-specific design tokens for spacing, typography, focus glow, scale, gutters, and content width
- [ ] Review whether TV should continue sharing the same launcher/activity path as mobile
- [ ] Re-evaluate whether Leanback support should remain optional for the current build strategy

### Suggested file targets
- `app/src/main/java/com/rpeters/jellyfin/MainActivity.kt`
- `app/src/main/java/com/rpeters/jellyfin/ui/tv/TvJellyfinApp.kt`
- `app/src/main/java/com/rpeters/jellyfin/ui/tv/TvNavGraph.kt`
- `app/src/main/java/com/rpeters/jellyfin/ui/theme/*`
- `app/src/main/AndroidManifest.xml`

### Definition of done
- TV code is easier to follow
- TV presentation is no longer accidentally driven by mobile assumptions
- Theme and layout decisions for TV are explicit rather than incidental

---

## Milestone 2 — Focus, D-pad, and restoration

### Goal
Make navigation on TV reliable and predictable on every screen.

### Screen-level contract to define everywhere
Each TV screen should define:

- initial focus target
- left-edge behavior
- right-edge behavior
- back behavior
- empty-state focus target
- focus restore behavior when returning from another route

### Tasks
- [ ] Write a focus contract for each TV route
- [ ] Standardize `TvScreenFocusScope` usage across all TV screens
- [ ] Ensure every screen has a stable `screenKey`
- [ ] Expand focus restore to include drawer items, grid items, hero items, action rows, and search results
- [ ] Persist focused index and scroll position by route/content source
- [ ] Restore the exact launching item after backing out of details
- [ ] Restore focus to the launching item after exiting playback
- [ ] Restore search query + result focus after returning from detail screens
- [ ] Audit `TvKeyboardHandler` against real remote behavior
- [ ] Confirm handling for D-pad center, enter, back, search, media keys, and menu keys where applicable

### Suggested file targets
- `app/src/main/java/com/rpeters/jellyfin/ui/tv/TvFocusManager.kt`
- `app/src/main/java/com/rpeters/jellyfin/ui/tv/TvKeyboardHandler.kt`
- `app/src/main/java/com/rpeters/jellyfin/ui/tv/TvJellyfinApp.kt`
- `app/src/main/java/com/rpeters/jellyfin/ui/tv/TvNavGraph.kt`
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvHomeScreen.kt`
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvLibraryScreen.kt`
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvItemDetailScreen.kt`
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvSearchScreen.kt`
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvSettingsScreen.kt`

### Definition of done
- Focus never feels random
- Back navigation returns the user to the correct place
- Drawer/content handoff is consistent
- The app is usable comfortably with only a remote

---

## Milestone 3 — Home screen polish

### Goal
Make Home the flagship TV surface and the visual standard for the rest of the TV app.

### Tasks
- [ ] Improve hero / featured item selection logic
- [ ] Prefer meaningful featured candidates such as Continue Watching, Next Up, new additions, or high-value recommendations
- [ ] Add a better artwork fallback chain for backdrop/banner/logo/poster
- [ ] Smooth backdrop transitions when focused content changes
- [ ] Refine row ordering and overall content strategy
- [ ] Decide which rows are essential: Continue Watching, Next Up, Recent Movies, Recent Episodes, Stuff, Libraries, Favorites, Recommended
- [ ] Improve loading, error, and empty states with TV-friendly placeholders and retry handling
- [ ] Audit recomposition hotspots caused by focus and backdrop changes
- [ ] Prefetch nearby hero and backdrop images to reduce jank

### Suggested file targets
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvHomeScreen.kt`
- `app/src/main/java/com/rpeters/jellyfin/ui/tv/components/*`
- any shared image-loading / artwork helpers used by TV home

### Definition of done
- Home feels cinematic and intentional
- Moving through rows feels smooth
- Loading and error states feel designed rather than temporary

---

## Milestone 4 — Library browsing polish

### Goal
Turn Movies, TV Shows, Music, Stuff, and Favorites into purpose-built TV destinations instead of one generic grid with minor route differences.

### Tasks
- [ ] Define route-specific behavior for Movies, TV Shows, Music, Stuff, and Favorites
- [ ] Keep shared grid infrastructure, but customize headers, filters, sort modes, empty states, and card styles per route
- [ ] Add TV-friendly filter and sort UX
- [ ] Persist current sort/filter state by route
- [ ] Tune grid column counts for 1080p and 4K
- [ ] Support different card styles where appropriate
- [ ] Improve pagination UX with visible loading-more feedback
- [ ] Prevent focus jumps while more items append
- [ ] Preserve exact focus position after entering/exiting details

### Suggested file targets
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvLibraryScreen.kt`
- `app/src/main/java/com/rpeters/jellyfin/ui/tv/TvNavGraph.kt`
- TV grid/card/focus helper components

### Definition of done
- Each library route feels intentional
- Large libraries remain comfortable to browse with a remote
- Pagination does not break focus or user orientation

---

## Milestone 5 — Detail screen redesign

### Goal
Make TV details feel like a premium streaming app surface.

### Tasks
- [ ] Build a TV-first detail template with backdrop, logo/title area, metadata row, actions row, overview, and related rails
- [ ] Support content-type variants for movie, series, season, and episode
- [ ] Standardize primary action order: Play/Resume, Play from Beginning, Next Episode, Favorite, Watched, Audio/Subtitles, More
- [ ] Make resume progress obvious and useful
- [ ] Set dynamic default focus to the most useful action
- [ ] Add rails for seasons, episodes, related titles, cast, and next-up content where available
- [ ] Preserve focus correctly between actions and nested rails
- [ ] Improve transition flow from library/home → detail and between series → season → episode levels

### Suggested file targets
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvItemDetailScreen.kt`
- TV detail action/rail components
- related metadata or item mapping helpers if needed

### Definition of done
- Details feel premium rather than utilitarian
- Resume/play choices are obvious
- Users can move through detail content fluidly with a remote

---

## Milestone 6 — Search, settings, and sign-in polish

### Goal
Make all supporting flows feel designed specifically for TV.

### Search tasks
- [ ] Improve D-pad-first search flow
- [ ] Manage focus between search field, keyboard, and results more deliberately
- [ ] Restore query and result focus after returning from details
- [ ] Add TV-friendly loading / empty / no-results states
- [ ] Group result types where useful

### Settings tasks
- [ ] Rework settings into a TV-friendly settings hub
- [ ] Group settings into clear categories such as Account, Playback, Audio/Subtitles, Appearance, Diagnostics
- [ ] Improve focus and selected state affordances for rows, toggles, and list options

### Sign-in / quick connect tasks
- [ ] Improve visual hierarchy on server connection
- [ ] Improve validation and auth failure recovery
- [ ] Confirm quick connect survives recomposition / lifecycle changes cleanly
- [ ] Confirm post-login focus lands in the right place

### Suggested file targets
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvSearchScreen.kt`
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvSettingsScreen.kt`
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvServerConnectionScreen.kt`
- `app/src/main/java/com/rpeters/jellyfin/ui/screens/tv/TvQuickConnectScreen.kt`

### Definition of done
- Search is comfortable using only a remote
- Settings feel native to TV
- Sign-in and quick connect feel polished and resilient

---

## Milestone 7 — TV playback overlay

### Goal
Keep the playback engine stable, but rebuild playback chrome so it is truly remote-first.

### Tasks
- [ ] Keep playback internals as-is unless there is a specific bug requiring deeper work
- [ ] Build a TV-first overlay with large focusable transport controls
- [ ] Add clearly focusable controls for play/pause, seek, timeline, subtitles, audio, speed, next episode, and more actions where supported
- [ ] Add an Up Next panel
- [ ] Make resume / continue progress obvious
- [ ] Ensure exiting playback restores focus to the launching item
- [ ] Ensure autoplay-next integrates properly with TV details and home rows
- [ ] Verify remote keys in playback on real devices

### Suggested file targets
- TV player screen / overlay files in the project
- any player controls UI used by TV mode
- navigation return logic related to player exit

### Definition of done
- Playback controls feel designed for TV rather than adapted from touch UI
- Exiting playback never feels disorienting

---

## Milestone 8 — TV tests and device validation

### Goal
Protect the TV experience from regressions as the UI evolves.

### Tasks
- [ ] Add Compose UI tests for TV launch, sign-in, drawer nav, home → detail → back, library restore, and search restore
- [ ] Add focus-specific tests where practical
- [ ] Add tests for drawer/content handoff and edge navigation
- [ ] Validate on emulator and at least one real TV / Google TV device
- [ ] Validate on 1080p and 4K
- [ ] Validate with simple remotes and keyboard-enabled remotes where possible

### Suggested file targets
- `app/src/androidTest/...`
- test helpers for TV focus/nav behavior
- CI workflow files if TV test automation is added later

### Definition of done
- TV regressions are caught earlier
- Focus/navigation breakage is less likely to ship unnoticed

---

## File-by-file implementation checklist

### `MainActivity.kt`
- [ ] Keep TV dispatch logic clear and minimal
- [ ] Remove mobile-only startup assumptions that affect TV
- [ ] Review intent/shortcut behavior on TV

### `TvJellyfinApp.kt`
- [ ] Harden drawer behavior
- [ ] Centralize route/focus handoff rules
- [ ] Improve drawer ↔ content focus transitions

### `TvNavGraph.kt`
- [ ] Replace thin route wrappers with richer, route-specific surfaces where needed
- [ ] Audit start-destination and auth transitions
- [ ] Add explicit focus restore hooks per route

### `TvFocusManager.kt`
- [ ] Expand from row memory into full route-level focus restoration
- [ ] Add debug logging hooks if useful
- [ ] Consider test coverage for restoration behavior

### `TvKeyboardHandler.kt`
- [ ] Separate global shortcuts from per-screen key handling
- [ ] Audit against real remote inputs

### `TvHomeScreen.kt`
- [ ] Upgrade featured selection logic
- [ ] Refine row strategy
- [ ] Improve placeholders and backdrop transitions
- [ ] Reduce jank during rapid focus changes

### `TvLibraryScreen.kt`
- [ ] Add TV-native filter/sort UX
- [ ] Improve route-specific presentation
- [ ] Improve pagination and restore behavior

### `TvItemDetailScreen.kt`
- [ ] Prioritize action row and related rails
- [ ] Improve resume/play behavior
- [ ] Improve season/episode drill-down UX

### `TvSearchScreen.kt`
- [ ] Improve D-pad-first flow and restoration
- [ ] Improve grouping and empty states

### `TvSettingsScreen.kt`
- [ ] Rework into a clearer TV settings hub
- [ ] Improve affordances for toggles and list rows

### `TvServerConnectionScreen.kt`
- [ ] Improve validation, loading, and error hierarchy

### `TvQuickConnectScreen.kt`
- [ ] Verify full polling/retry/error lifecycle
- [ ] Preserve readable 10-foot layout at all times

---

## Suggested sprint order

### Sprint 1
- [ ] Milestone 1 — architecture cleanup
- [ ] Milestone 2 — focus contracts and restoration
- [ ] Home screen focus/restore fixes

### Sprint 2
- [ ] Home screen polish
- [ ] Library screen sort/filter UX
- [ ] Library restore/pagination polish

### Sprint 3
- [ ] Detail screen redesign
- [ ] Search polish
- [ ] Settings polish

### Sprint 4
- [ ] TV playback overlay
- [ ] TV UI tests
- [ ] Device validation pass

---

## Definition of done: “TV is no longer wonky”

The TV app can be considered stable when:

- launching on TV feels intentional
- the drawer and content hand off focus correctly every time
- backing out of details returns to the exact item the user came from
- Home feels cinematic and stable
- library browsing is predictable and remote-friendly
- detail pages feel like a streaming app, not a utility screen
- search and sign-in are comfortable with only a remote
- playback controls are clearly TV-first

---

## Best immediate next task

If only one task should be tackled first, do this:

**Run a focused TV navigation + focus restoration cleanup pass across `TvJellyfinApp`, `TvNavGraph`, `TvHomeScreen`, and `TvLibraryScreen`.**

That should remove a large share of the current “wonky” feeling before deeper visual redesign work begins.
