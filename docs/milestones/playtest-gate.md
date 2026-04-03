# Playtest Gate

**Status:** Not started
**Depends on:** M3
**Type:** Developer go/no-go checkpoint

## Purpose

The developer plays the game on a physical device and decides whether the core gameplay feel is good enough to proceed. No further milestones start until this gate passes.

## Evaluate and tune

- DAS / ARR feel (responsive but not twitchy)
- Hard-drop velocity threshold (intentional flicks vs accidental drags)
- Grab mode `GRAB_CELL_SLOP` (hit-test tolerance — not too tight, not too loose)
- Drop delay duration curve (useful at low levels, not overpowered at high levels)
- Soft drop vs hard drop gesture boundary (no misclassifications in fast play)
- Gravity curve feel across levels (fair ramp, no sudden walls)
- Overall control responsiveness and visual clarity

## Exit criteria

Developer is satisfied with the core gameplay feel. All tuning changes applied to `GameConstants`. Explicit go decision recorded.
