# M3 — Board Rendering & Controls

**Status:** Completed
**Depends on:** M2b

## Scope

### Board rendering
- `BoardCanvas` via Compose `Canvas`:
  - Candy block rendering per Visual style spec (base fill → inner gradient → top-left shine → edge bevel → outer glow via `BlurMaskFilter`)
  - Ghost piece (outline only, ~30% opacity, no fill, no glow)
  - Board background (`#1A1A2E`, subtle grid lines at 4% white, thin neon border)
- App background (deep purple-black radial gradient, `#0D0D1A` → `#000000`)

### Controls
- `ControlSurface` gesture recognizer:
  - Tap right half → rotate CW; tap left half → rotate CCW
  - Swipe left/right with DAS (150 ms) / ARR (30 ms) → move
  - Slow drag down (< 800 dp/s) → soft drop; fast flick (≥ 800 dp/s) → hard drop
  - Swipe up → drop delay
  - Grab mode: `ACTION_DOWN` hit-test on active piece cells (with `GRAB_CELL_SLOP`), horizontal drag moves column-by-column, lift without drag = rotate, downward drag without prior horizontal = soft/hard drop

### Panels & HUD
- Next-piece preview panel (level-tier queue depth, animated slot fade on tier transition)
- Held piece panel (tappable, dimmed when `canHold` is false)
- `ScorePanel` (animated score / level / lines display)
- `?` button (top-start) — opens the full-screen tutorial/help screen
- Mute button (top-end) — toggles `isMuted` StateFlow, icon switches, preference persisted in DataStore
- Back button: running → pause dialog; paused → exit confirmation

## Exit criteria

Fully playable on a device/emulator with touch controls. Visual style matches candy block spec. All UI elements render and respond correctly.

## Verified coverage

- `BoardCanvas`: Custom `Canvas` implementation with candy-style blocks (glow, gradient, shine, bevel).
- `ScorePanel`: Responsive HUD with held piece and next pieces preview.
- `ControlSurface`: Advanced gesture classification with grab mode and DAS/ARR.
- Tutorial/help screen: functional full-screen help screen.
- `BlockDropApp`: Integrated layout with overlays for Start, Pause, and Game Over.
