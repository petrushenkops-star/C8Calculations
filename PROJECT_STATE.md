# PROJECT_STATE — С8 Расчеты

- Application Name: С8 Расчеты
- Package Name: com.pavel.c8calculations
- Application ID: com.pavel.c8calculations
- Version Name: 0.1.0
- Version Code: 1
- Current Phase: PHASE 2 COMPLETE

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
- JUnit: 4.13.2 (unit tests)

## Implemented
- PHASE 1 UI shell, Material 3 theme and navigation.
- ParticipantLevel enum: C1, C2, C3, C4, C5, C6.
- LevelConfig model using BigDecimal for monetary values.
- Central LevelConfiguration as the single source for level deposit, income per signal and base signal count.
- Approved deposits: 300 / 700 / 1500 / 3000 / 6000 / 10000 USDT.
- Approved income per signal: 2.4 / 5.6 / 12 / 24 / 48 / 80 USDT.
- Base signal counts by level: 2 / 2 / 3 / 3 / 4 / 4.
- Unit tests for the centralized level configuration.
- CI runs unit tests before assembling APK.

## Not implemented by design
SignalRuleEngine, daily income calculation, weekday rules, X/Y/Z, VIP/L1 logic, auto-level upgrades, forecasts, profit withholding, OCR, leader runtime analysis, dividends, DataStore, Room, ML Kit and CameraX.

## Tested
- GitHub Actions run 36547850354.
- :app:testDebugUnitTest: SUCCESS.
- :app:assembleDebug: SUCCESS.
- APK existence verification: SUCCESS.
- Artifact upload: SUCCESS.

## Known Problems
- No known PHASE 2 build/test errors.
- Current build remains debug-signed; permanent release signing remains a separate setup task.

## Last Completed Task
PHASE 2 — data models and centralized level configuration.

## Next Task
PHASE 3 — daily calculation engine and signal rules.
