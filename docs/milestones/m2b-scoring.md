# M2b — Scoring, Line Clears & Advanced Detection

**Status:** Completed
**Depends on:** M2a

## Scope

- Line-clear detection + cascade (remove completed rows, shift above rows down)
- T-spin detection (3-corner rule on T-piece locked via rotation; ≥3 diagonals = T-spin, 2 back-facing = mini T-spin)
- All-clear detection (board fully empty after line clear; score replaces base line-clear value, does not stack)
- Full scoring engine per the plan's points table:
  - Base line-clear values (100/300/500/800 × level)
  - T-spin / mini T-spin bonuses
  - Back-to-back ×1.5 multiplier (Tetris or T-spin clears; chain breaks on normal clears)
  - Combo bonus (50 × combo count × level; resets on zero-clear placement)
  - All-clear replacement values (800/1200/1800/2000/3200 × level)
  - Soft/hard drop flat rewards (already wired in M2a, now integrated into total score)
- Level progression: every 10 lines advances one level, cap at 100
- Level-up updates gravity tick interval and next-queue display depth per tier table
- Combo counter lifecycle (increment on consecutive clears, reset on zero-clear lock)

## Exit criteria

Every scoring path verified with unit tests — all rows in the points table, B2B chain continuation/break rules, all-clear replacements, combo accumulation and reset, level-up triggers correct gravity and queue-depth changes.

## Verified coverage

- `BoardState`: line clearing, row shifting, all-clear detection, T-spin (Full/Mini) 3-corner detection.
- `GameLoop`: full scoring table integration, level advancement logic, combo tracking, back-to-back multiplier, gravity speed scaling.
- `BoardStateTest`: unit tests for line clearing, T-spin detection, and all-clear detection.
