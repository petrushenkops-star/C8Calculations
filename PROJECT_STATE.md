# PROJECT_STATE — С8 Расчеты

This file is the current state summary for ChatGPT/Codex and maintainers. Keep it synchronized with every released version and material architecture change.

## Current release
- Application Name: С8 Расчеты
- Package / Application ID: `com.pavel.c8calculations`
- Version Name: **1.1.11**
- Version Code: **113**
- Primary Android branch: `main`
- Stable APK distribution: GitHub Releases, tag `v<versionName>`
- Current stable release: `v1.1.11`

## Toolchain
- Android Gradle Plugin: 9.4.0
- Gradle: 9.6.0 via committed Gradle Wrapper
- JDK: 17 in GitHub Actions
- compileSdk: 37
- minSdk: 26
- targetSdk: 37
- Kotlin/Compose compiler plugin: 2.4.10
- Compose BOM: 2026.09.00
- Navigation Compose: 2.10.2
- Activity Compose: 1.13.0
- ML Kit Text Recognition: 16.0.1
- PDFBox Android: 2.0.27.0
- JUnit: 4.13.2

## Architecture
- Single Android `app` module.
- Compose UI is separated from calculation and recognition logic.
- `calculation/`: signal rules, daily calculations, profit simulations, targets, level upgrades, dividends and report formatting.
- `recognition/`: image OCR, PDF team import/parsing, participant extraction and persisted latest team scan.
- `model/`: domain models.
- `ui/`: Home, Profit, Team Recognition, Dividends, Settings and navigation.
- Money calculations use `BigDecimal`.
- Business calculations must not be duplicated in Composables.

## Implemented — Profit
- Levels C1-C6 with centralized level configuration.
- Deposits: 300 / 700 / 1500 / 3000 / 6000 / 10000 USDT.
- Income per signal: 2.4 / 5.6 / 12 / 24 / 48 / 80 USDT.
- SignalRuleEngine supports leader signals X, VIP and L1>=10 bonus rules by weekday.
- DailyCalculationEngine calculates daily signals, income and ending balance.
- ProfitSimulationEngine supports the «До даты» mode.
- TargetBalanceCalculator supports the «До баланса» mode.
- TargetNetProfitCalculator supports the «До чистой прибыли» mode.
- Net profit: 70% of (result balance - current/resulting level deposit); 30% commission is reported separately and does not reduce simulation balance.
- Optional automatic C1→C6 level upgrades; new level applies from the next calendar day and balance is not reduced.
- Detailed day-by-day calculation report is available.
- Net profit can be evaluated in USDT and converted to RUB using a user-entered USDT/RUB rate.

## Implemented — Team recognition
- Image recognition uses ML Kit Text Recognition.
- PDF import and parsing uses PDFBox Android.
- OCR normalization handles Latin/Cyrillic C/С variants, case and spacing.
- PDF participant cards contain level, name, UID and date and are displayed compactly.
- Team lines are determined relative to the leader: first column to the right is L1, then L2 and L3.
- Official team composition includes only participants from L1-L3.
- Leader, C0 and C1 are excluded from official team composition even when positioned in L1-L3.
- PDF participant list supports sorting by line, level, name and date; UID sorting is not used.
- Recognition counts can be manually corrected before use.
- Latest recognized team structure is persisted for the report.

## Implemented — Dividends and report
- Dividend calculation is centralized for C2-C6; C1 does not participate.
- Rates/formulas:
  - C2: 11.2 × 2% × days × count
  - C3: 24 × 2% × days × count
  - C4: 48 × 2% × days × count
  - C5: 96 × 2% × days × count
  - C6: 160 × 2% × days × count
- Team recognition results feed the dividend screen.
- Report formatting includes stored team scan data and dividend calculation data.
- UI uses compact global line spacing introduced in 1.1.10.

## Automated tests
Current unit-test suites include:
- `LevelConfigurationTest`
- `Phase3CalculationTest`
- `Phase4SimulationTest`
- `Phase6TargetBalanceTest`
- `DividendCalculatorTest`
- `PdfTeamParserTest`
- `TeamReportFormatterTest`
- PDF parser fixture: `app/src/test/resources/team-pdf-labels.tsv`

CI runs `:app:testDebugUnitTest` before building APKs.

## Build, signing and distribution
- Pull requests to `main`: unit tests + debug APK build.
- `main` is branch-protected: changes require a pull request and the `test-debug` status check from GitHub Actions; the branch must be up to date before merge and administrators cannot bypass the rule.
- Repository visibility is public so GitHub Free can enforce branch protection.
- Push to `main`: tests first, then signed release APK build.
- Release APK uses the permanent C8 signing key stored in GitHub Actions secrets; the key is not committed to the repository.
- CI verifies the APK signature/certificate before publication.
- APK filename is derived automatically from `versionName`.
- Stable APKs are published permanently as GitHub Releases: `v<versionName>`.
- Old Android Actions artifacts from `main` are cleaned automatically.
- A temporary Actions artifact is attempted with `retention-days: 3`; GitHub Releases are the canonical download location.
- Updating an installed app requires increasing `versionCode` and signing with the same permanent key.

See `AUTOMATED_BUILD.md` for the current CI/release workflow.

## iOS
- iOS work currently lives on the separate long-lived branch `ios-development`.
- The `ios/` tree and iOS workflows are not part of Android `main` yet.
- Do not assume Android 1.1.11 features are already synchronized to iOS.

## Known issues / technical debt
- iOS and Android histories have diverged and should be normalized before continued parallel development.
- Test coverage exists for core calculations and PDF parsing but is not yet comprehensive for all OCR/UI scenarios.
- GitHub Actions artifact quota was exhausted by historical APKs on 2026-10-06. Old Android artifacts were cleaned; permanent APK storage now uses GitHub Releases. GitHub may take 6-12 hours to recalculate artifact quota.

## Last completed work
- Version 1.1.11: persisted team scan + team/dividend data in report.
- CI migration: stable signed APKs now publish to GitHub Releases; old Android Actions artifacts were cleaned and temporary retention reduced to 3 days.

## Next work
No numbered PHASE is currently authoritative. Continue from the latest user-approved requirement, preserve working functionality, and update this file whenever a release or material architecture change is completed.
