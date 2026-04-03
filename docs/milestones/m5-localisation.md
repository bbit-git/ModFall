# M5 — Localisation, Testing & Launch Prep

**Status:** Completed
**Depends on:** M2b (for test targets); can run **in parallel** with M3 / M4
**Parallelisable:** Yes — localisation and test authoring are independent of UI/effects work

## Scope

### Localisation
- Translate all 17 non-English `strings.xml` files
- Arabic RTL verification: layout mirroring, `?` button top-right, score panel mirrored, no clipped text
- German / Russian text overflow checks (longest average word length); adjust font size or wrapping as needed

### Unit tests
- `BoardState` (collision, line-clear, top-out)
- `Scoring` (all clear types, T-spin, B2B, combo)
- `PieceGenerator` (7-bag distribution)
- `GameLoop` (lock-delay reset cap, drop-delay timer, one-use flag) using `kotlinx-coroutines-test` with `TestScope` / `advanceTimeBy`

### Compose previews
- Board (with ghost piece), tutorial, scoreboard, pause menu — in English, Arabic (RTL), Chinese Simplified

### Launch prep
- Cold-start profiling: target < 1 s to first frame on mid-range device
- Replace placeholder icon and splash screen with final assets (developer-created)

## Exit criteria

All locales render correctly. Unit tests pass. Cold start meets target. APK ready for distribution.
