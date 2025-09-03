# Repository Guidelines

## Project Structure & Module Organization
- Single Gradle module: `:app` (Kotlin + Jetpack Compose).
- Source code: `app/src/main/java/com/rpeters/jellyfin/...`.
- UI/resources: `app/src/main/res`; manifest: `app/src/main/AndroidManifest.xml`.
- Unit tests (JVM): `app/src/test/java`; instrumentation: `app/src/androidTest/java`.
- Build config: `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`; versions in `gradle/libs.versions.toml`.

## Build, Test, and Development Commands
- Build debug APK: `./gradlew assembleDebug` → outputs `app/build/outputs/apk/debug`.
- Install on device: `./gradlew installDebug`.
- JVM unit tests: `./gradlew testDebugUnitTest`.
- Instrumentation tests: `./gradlew connectedAndroidTest` (emulator/device; `HiltTestRunner`).
- Lint: `./gradlew lintDebug` → report: `app/build/reports/lint`.
- Coverage: `./gradlew jacocoTestReport` → HTML/XML under `app/build/reports`.

## Coding Style & Naming Conventions
- Kotlin style, 4‑space indent, ~120‑char lines.
- Names: Classes `PascalCase`; functions/vars `camelCase`; constants `UPPER_SNAKE_CASE`.
- Packages under `com.rpeters.jellyfin`.
- UI: Compose + Material 3; unidirectional data flow (MVVM); DI via Hilt.

## Testing Guidelines
- Frameworks: JUnit4, MockK, Turbine, AndroidX Test; Hilt testing configured.
- Focus tests on ViewModel/Repository logic; mock network/I/O boundaries.
- Name tests descriptively (e.g., `loadMovieDetails_updates_state_on_success`).
- Run unit tests with `testDebugUnitTest`; instrumentation with `connectedAndroidTest`.
- Coverage via `jacocoTestReport`; generated/DI classes already filtered.

## Commit & Pull Request Guidelines
- Commits use Conventional Commits (e.g., `feat:`, `fix:`, `docs:`). Keep changes small and focused.
- Branches: `feature/...`, `bugfix/...`, `hotfix/...`, `docs/...`.
- PRs: clear description, linked issues, and screenshots/GIFs for UI changes. List affected areas and note test/coverage impact.
- Ensure `./gradlew testDebugUnitTest` and `./gradlew lintDebug` pass before requesting review.

## Security & Configuration Tips
- Do not commit secrets/keystores; prefer Android Keystore/Encrypted storage.
- Network config: `app/src/main/res/xml/network_security_config.xml`.
- Minimum SDK 26; Java 17 with desugaring enabled.
- Keep versions aligned in `gradle/libs.versions.toml`.

