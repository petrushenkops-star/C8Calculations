# PROJECT_STATE — С8 Расчеты

- Application Name: С8 Расчеты
- Package Name: com.pavel.c8calculations
- Application ID: com.pavel.c8calculations
- Version Name: 0.1.0
- Version Code: 1
- Current Phase: PHASE 1 COMPLETE

## Toolchain pinned
- Android Gradle Plugin: 9.4.0
- Gradle: 9.6.0 (CI)
- JDK: 17 (CI)
- compileSdk: 37
- minSdk: 26
- targetSdk: 37
- Kotlin/Compose compiler plugin: 2.4.10
- Compose BOM: 2026.09.00
- Navigation Compose: 2.10.2
- Activity Compose: 1.13.0

## Implemented
- Single app module and fixed package/applicationId.
- Material 3 Compose theme with light/dark system selection.
- Home screen.
- Profit placeholder with “До даты” and “До баланса”.
- Team recognition placeholder.
- Dividend placeholder.
- Settings placeholder.
- Centralized Navigation Compose routes and back navigation.
- GitHub Actions workflow for reproducible debug APK build.

## Not implemented by design
Financial/business calculations, levels, deposits, X/Y/Z, automatic upgrades, OCR, leader recognition runtime logic, dividends, DataStore, Room, ML Kit, CameraX.

## Tested
- GitHub Actions run 36545915238.
- :app:assembleDebug: SUCCESS.
- APK existence verification: SUCCESS.
- Artifact upload: SUCCESS.
- Artifact: C8Calculations-v0.1.0-debug.

## Known Problems
- No known PHASE 1 build errors.
- APK still needs user device smoke test.
- Current artifact is a debug build; permanent release signing is a later setup task.

## Dependencies
Only Activity Compose 1.13.0, Compose Material 3 via BOM 2026.09.00, and Navigation Compose 2.10.2. No ML Kit, CameraX, Room, DataStore, Retrofit, DI, JSON or image-loading libraries.

## Last Completed Task
PHASE 1 project uploaded to GitHub and successfully built in CI.

## Next Task
After user confirms the APK launches and navigation works: PHASE 2 — data models and level configuration.
