# ARCHITECTURE — С8 Расчеты

Source of truth for the approved architecture and business rules. PHASE 1 intentionally contains only the application shell; business rules are implemented in later controlled phases.

## Architecture
Single `app` module. Compose UI -> ViewModel (only when state/business logic exists) -> focused business components. OCR, financial calculations, dividends, settings and persistence stay separated. No financial calculations in Composables.

## Planned areas
- `model`: domain models.
- `calculation`: SignalRuleEngine, DailyProfitCalculator, LevelUpgradeEngine, ProfitForecastEngine, TargetBalanceCalculator, ProfitCalculator.
- `ocr`: image/OCR/normalization/parsing/layout analysis. Leader rule: if the leftmost visual column contains exactly one rectangle, that rectangle is the leader and is excluded; if it contains two or more rectangles, no leader is present in that image.
- `dividend`: dividend calculation independent from OCR.
- `data/settings`: centralized defaults and persisted settings.
- `ui`: screens and presentation state.

## Core invariants
One daily calculation engine is reused by both forecast modes. Level/deposit/balance are separate concepts. Money uses BigDecimal. OCR output is manually confirmable before it becomes TeamStructure. Business constants have one source of truth. No duplicate implementations of the same formula.

## Development rules
Package/applicationId stay `com.pavel.c8calculations`. Do not upgrade dependencies without a reason. Do not remove working functionality without explicit approval. Avoid empty architectural layers. Every stage must leave a buildable project; list changed files and update PROJECT_STATE.md. Formula changes require tests. Architecture changes must be documented before implementation.
