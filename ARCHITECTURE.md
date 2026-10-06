# ARCHITECTURE — С8 Расчеты

This document defines the approved architecture and the invariants that must remain stable while the project evolves. The current implemented state is described in `PROJECT_STATE.md`; build/release behavior is described in `AUTOMATED_BUILD.md`.

## Scope
Android application `С8 Расчеты` with four user-facing areas:
- Profit forecast: «До даты», «До баланса», «До чистой прибыли».
- Team recognition from image and PDF.
- Dividend calculation.
- Settings.

Android package/applicationId remains `com.pavel.c8calculations`.

## Current Android structure
The project intentionally uses one Android `app` module. Do not add architectural layers or modules without a concrete need.

```text
app
└── src/main/java/com/pavel/c8calculations
    ├── calculation/   financial/domain engines and report formatting
    ├── model/         domain models
    ├── recognition/   image OCR, PDF import/parsing and team scan persistence
    └── ui/            Compose screens, navigation and theme
```

### calculation
Current focused components include:
- `SignalRuleEngine`
- `DailyCalculationEngine`
- `ProfitSimulationEngine`
- `LevelUpgradeEngine`
- `TargetBalanceCalculator`
- `TargetNetProfitCalculator`
- `DividendCalculator`
- `TeamReportFormatter`
- centralized `LevelConfiguration`

Financial formulas belong here, not in Compose UI.

### recognition
Current components include:
- `TeamLevelRecognizer` for image/OCR recognition.
- `ParticipantTextExtractor` for participant text extraction.
- `PdfTeamImporter` and `PdfTeamParser` for PDF recognition.
- `TeamScanStorage` for the latest recognized team data used by reports.

ML Kit Text Recognition is used for image text recognition. PDFBox Android is used for PDF parsing/import.

### ui
Compose screens are grouped by feature:
- `ui/home`
- `ui/profit`
- `ui/team`
- `ui/dividend`
- `ui/settings`
- `ui/navigation`
- `ui/theme`

UI owns presentation and user input state. Calculation and recognition rules must stay in focused domain components and must not be duplicated in Composables.

## Core financial invariants
- Money uses `BigDecimal`; do not introduce Float/Double for business money calculations.
- Level, deposit and balance are separate concepts.
- Levels are C1-C6.
- Deposits are centralized: 300 / 700 / 1500 / 3000 / 6000 / 10000 USDT.
- Income per signal is centralized: 2.4 / 5.6 / 12 / 24 / 48 / 80 USDT.
- Signal count rules have one implementation in `SignalRuleEngine`.
- X = leader signals; VIP adds its approved bonus; L1>=10 controls the Friday/Saturday Z bonus.
- Daily income = signal count × income per signal.
- Balance is increased by full daily income.
- Commission is calculated once as 30% of profit basis and does not reduce the simulated balance.
- Net profit is 70% of the same profit basis.
- Auto-upgrade is evaluated after a day's accrual. The selected new level applies from the next calendar day.
- Auto-upgrade never subtracts the new deposit from balance.
- When several thresholds are crossed, the maximum reachable level may be selected.
- The same daily calculation rules must be reused by all forecast/target modes.

Any formula or level-rule change requires corresponding unit tests.

## Team recognition invariants
### Text normalization
Recognition must handle Latin/Cyrillic C/С, upper/lowercase variants and spacing such as `C4`, `С4`, `c4`, `с4`, `C 4`, `С 4`.

### Leader and lines
- The leader is determined by layout, never by a hard-coded person name.
- For the established visual layout, the column immediately to the right of the leader is L1, then L2, then L3.
- Official team composition includes only L1-L3.
- Leader, C0 and C1 are excluded from official team composition even if their positions fall within L1-L3.
- C1 may still be displayed where the UI requires it; exclusion concerns official team count/dividends.

### PDF-specific participant list
- The participant list is shown for PDF recognition only.
- Participant data contains level, name, UID and date.
- Cards are compact (two display lines).
- Supported sorting: line, level, name and date.
- UID sorting is intentionally not present.

Recognition output remains manually correctable before final counts are used.

## Dividend invariants
Dividend calculation is independent from OCR parsing and consumes confirmed team counts.

C1 is excluded. Current C2-C6 formulas:
- C2: 11.2 × 2% × days × count(C2)
- C3: 24 × 2% × days × count(C3)
- C4: 48 × 2% × days × count(C4)
- C5: 96 × 2% × days × count(C5)
- C6: 160 × 2% × days × count(C6)

Dividend constants/formulas must have one implementation and require tests when changed.

## Persistence
Only persistence required by implemented features should exist. The current Android app persists the latest recognized team scan for reporting through `TeamScanStorage`.

Do not introduce Room/DataStore or other persistence layers solely for architectural symmetry. Add them only when an approved feature needs them.

## Dependency policy
- Do not upgrade AGP, Kotlin/Compose, AndroidX, ML Kit or PDFBox without a concrete reason.
- Prefer platform/framework capabilities before adding a dependency.
- Keep dependency changes scoped and documented.

## Testing policy
At minimum, preserve tests for:
- level configuration and signal rules;
- daily/multi-day calculations and upgrades;
- target balance and target net profit behavior when changed;
- 30% commission / 70% net-profit invariants when changed;
- dividends;
- PDF parsing and L1-L3 team rules;
- report formatting.

Bug fixes should add a regression test whenever the affected logic is testable outside UI/OCR platform APIs.

## Build and release architecture
- Pull request to `main`: unit tests + debug build.
- Push/merge to `main`: tests followed by signed release build.
- Permanent signing key is stored only in GitHub Actions secrets.
- CI verifies the APK signature.
- Stable APK is published permanently to GitHub Release `v<versionName>`.
- Actions artifacts are temporary; GitHub Releases are the canonical distribution location.
- Version changes require a higher `versionCode`; release filename/tag derive from `versionName`.

See `AUTOMATED_BUILD.md` for exact workflow behavior.

## iOS boundary
iOS currently lives in the separate `ios-development` branch. It contains an `ios/` Swift/SwiftUI codebase and iOS workflows but is not yet synchronized with Android `main`.

Until the repository layout is normalized:
- do not assume an Android feature exists on iOS;
- do not modify iOS as a side effect of an Android-only task;
- iOS synchronization must be an explicit task.

## Development rules
- Preserve working behavior unless the user explicitly requests a change.
- Keep changes narrowly scoped.
- Do not duplicate business formulas.
- Avoid speculative abstraction and empty layers.
- Every code change must leave the project buildable.
- Run relevant tests before merge.
- Update `PROJECT_STATE.md` for each release or material architecture/state change.
- Update this file before or together with material architecture changes.
- Follow the repository-level `AGENTS.md` for the operational workflow used by Codex.
