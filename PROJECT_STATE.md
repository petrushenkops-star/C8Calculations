# PROJECT_STATE — С8 Расчеты

- Application Name: С8 Расчеты
- Package Name: com.pavel.c8calculations
- Application ID: com.pavel.c8calculations
- Version Name: 0.1.0
- Version Code: 1
- Current Phase: PHASE 4 COMPLETE

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
- PHASE 2 ParticipantLevel C1-C6 and centralized LevelConfiguration.
- Monetary calculations use BigDecimal.
- Approved deposits: 300 / 700 / 1500 / 3000 / 6000 / 10000 USDT.
- Approved income per signal: 2.4 / 5.6 / 12 / 24 / 48 / 80 USDT.
- Base signal counts: 2 / 2 / 3 / 3 / 4 / 4.
- PHASE 3 SignalRuleEngine.
- Monday-Thursday and Sunday: base signals + X + VIP bonus.
- Friday-Saturday: 1 + Z; Z=1 when L1 count >= 10.
- DailyCalculationInput and DailyCalculationResult.
- DailyCalculationEngine calculates signal count, daily income and balance after the day.
- Full signal income is added in PHASE 3; no 30% withholding is applied.
- Validation rejects negative X and L1 count.
- Unit tests cover level configuration and PHASE 3 calculation rules.
- PHASE 4 multi-day ProfitSimulationEngine and LevelUpgradeEngine.
- Auto-upgrade is checked after each day and applies from the next calendar day.
- Auto-upgrade selects the maximum level whose deposit threshold is reached; balance is not reduced.
- Auto-upgrade can be disabled.
- Gross profit = expected balance - deposit of the resulting level; 30% withholding is calculated separately and does not reduce simulation balance; net profit = gross profit × 70%.
- CI runs unit tests before assembling APK.

## Not implemented by design
OCR, leader runtime analysis, dividends, DataStore, Room, ML Kit and CameraX.

## Tested
- GitHub Actions run 36550773008.
- :app:testDebugUnitTest: SUCCESS.
- :app:assembleDebug: SUCCESS.
- APK existence verification: SUCCESS.
- Artifact upload: SUCCESS.
- Artifact: C8Calculations-v0.1.0-debug.

## Known Problems
- No known PHASE 4 build/test errors.
- Current build remains debug-signed; permanent release signing remains a separate setup task.

## Last Completed Task
PHASE 4 — multi-day profit forecast and automatic level-upgrade logic.

## Next Task
PHASE 5 — connect calculation core to the Profit screen UI.
