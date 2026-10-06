# AGENTS.md — C8Calculations

These instructions apply to the entire repository. They are written for Codex/AI coding agents working on С8 Расчеты.

## 1. Read project context before changing code
Before implementation, read:
1. AGENTS.md
2. PROJECT_STATE.md
3. ARCHITECTURE.md
4. AUTOMATED_BUILD.md when the task touches versioning, CI, signing, APKs or releases.

Treat PROJECT_STATE.md as the current implementation snapshot and ARCHITECTURE.md as the architecture/business-invariant contract.

If code and documentation disagree, inspect the code/tests and resolve the discrepancy explicitly; do not silently guess.

## 2. Preserve working functionality
- Change only what the user asked for or what is strictly necessary to make that change work.
- Do not remove or redesign existing working behavior without explicit approval.
- Avoid unrelated refactors, dependency upgrades and broad formatting churn.
- Preserve package/applicationId com.pavel.c8calculations.
- Keep the Android UI in Russian unless the user explicitly requests localization changes.

## 3. Git workflow
- Do not push feature/fix/documentation work directly to main.
- Create a focused branch from the current main.
- Use a clear branch name such as feature/..., fix/..., docs/... or ci/....
- Open a pull request to main.
- Require the repository CI to pass before merging.
- Merge only the tested PR head; do not merge if the head moved unexpectedly.
- Do not rewrite published history.
- Do not delete branches, tags, releases or artifacts unless the task explicitly authorizes deletion/cleanup.

## 4. Android architecture rules
- Keep the current single app module unless there is a concrete, user-approved reason to modularize.
- Compose screens own presentation/input state.
- Financial/domain calculations belong in focused components under calculation/, not in Composables.
- Recognition/PDF rules belong under recognition/, not in UI.
- Domain models belong under model/.
- Do not create empty layers/interfaces/repositories just to imitate a generic architecture template.
- Reuse existing engines instead of adding parallel implementations.

## 5. Financial business invariants
Unless the user explicitly changes the business rules:
- Money calculations use BigDecimal.
- Levels are C1-C6.
- Deposits: C1=300, C2=700, C3=1500, C4=3000, C5=6000, C6=10000 USDT.
- Income per signal: C1=2.4, C2=5.6, C3=12, C4=24, C5=48, C6=80 USDT.
- X is leader-signal count.
- VIP bonus and L1>=10 rules are implemented centrally by SignalRuleEngine.
- Friday/Saturday L1 bonus uses Z=1 only when the approved L1>=10 condition is true.
- Daily income = signals × income per signal.
- Full daily income is added to balance.
- 30% commission is calculated from the approved profit basis and does not reduce simulation balance.
- Net profit is 70% of the same basis.
- Auto-upgrade is checked after daily accrual and applies from the next day.
- Auto-upgrade does not subtract deposit from balance.
- A multi-threshold jump may select the maximum reachable level.
- Forecast modes must reuse the same daily rules.

Formula/rule changes require tests.

## 6. Team recognition invariants
- Normalize Latin/Cyrillic C/С variants, case and spacing.
- Never identify the leader by a fixed personal name.
- Column immediately right of the leader = L1, then L2, then L3.
- Official team includes only L1-L3.
- Leader, C0 and C1 are excluded from official team count even when positioned in L1-L3.
- C1 may still be displayed where required by UI.
- Recognition counts remain manually correctable.

PDF-specific participant list:
- exists only for PDF recognition;
- participant data includes level, name, UID and date;
- card display remains compact at two lines;
- sorting is by line, level, name or date;
- UID sorting is intentionally absent.

Do not change image-recognition behavior when a request is explicitly PDF-only, and vice versa.

## 7. Dividend invariants
C1 does not participate in dividends.

Keep the centralized C2-C6 formulas:
- C2: 11.2 × 2% × days × count
- C3: 24 × 2% × days × count
- C4: 48 × 2% × days × count
- C5: 96 × 2% × days × count
- C6: 160 × 2% × days × count

Changes require unit tests.

## 8. Tests and verification
For code changes:
- run relevant unit tests;
- run the full Android unit-test suite when practical;
- verify debug assembly before proposing merge;
- add a regression test for a logic bug when feasible.

Current CI equivalents:
- gradle --no-daemon :app:testDebugUnitTest
- gradle --no-daemon :app:assembleDebug

A Gradle Wrapper is not currently committed. Do not pretend ./gradlew exists until it is added.

For UI-only changes that are not covered by unit tests, still run compilation/build checks and clearly state the manual/visual verification that remains.

## 9. Versioning, signing and releases
- Do not create a new app version for a cosmetic/isolated change unless the user requests a release or the established task flow requires one.
- For a new installable release, increase versionCode and set the approved versionName.
- Never change applicationId or signing identity casually; Android update-over-existing depends on them.
- Never commit keystores, passwords, tokens or GitHub secrets.
- Release APK signing is handled by protected GitHub Actions secrets.
- GitHub Release v<versionName> is the canonical permanent APK location.
- Actions artifacts are temporary.
- Do not replace an existing permanent Release asset with code from a different commit while keeping the same version/tag. Bump the version instead.

## 10. Dependencies and security
- Do not upgrade dependencies without a task-related reason.
- Prefer existing dependencies/platform APIs.
- Never print or expose secrets.
- Do not add credentials or generated signing material to the repository.

## 11. Documentation
Update documentation together with reality:
- PROJECT_STATE.md: every released version and material state/feature change.
- ARCHITECTURE.md: architecture or invariant changes.
- AUTOMATED_BUILD.md: CI/signing/release/storage changes.

Do not leave old PHASE/version claims that contradict current code.

## 12. iOS boundary
The iOS implementation currently lives in ios-development and is not synchronized with Android main.

- Do not modify iOS during Android-only work.
- Do not claim feature parity without checking the iOS branch.
- Cross-platform synchronization must be an explicit task.

## 13. Completion report
When finishing a task, report succinctly:
- what changed;
- what tests/builds ran and their result;
- version/release created, if any;
- any remaining limitation or user verification needed.

Do not report an action as successful until its actual result is verified.
