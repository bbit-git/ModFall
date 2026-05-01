## Task: Refactor in-game UI for fullscreen, safe-area compliance, simplified controls, and stronger overlays

### Context

This is the Android/Kotlin version of Mod Fall. The current in-game layout wastes space, does not feel properly fullscreen, has too many bottom controls, and overlay menus are too transparent. The game should feel board-first, immersive, and safe on devices with cutouts, rounded corners, and gesture areas.

### Objective

Update the gameplay UI so it:

* runs in fullscreen
* respects safe areas and display cutouts
* removes all bottom controls except Hard Drop
* uses almost opaque in-game menus and overlays

### Required changes

#### 1. Enable fullscreen gameplay

Update the gameplay screen to run in fullscreen during active play.

Requirements:

* hide or minimize system UI impact during gameplay
* do not let status/navigation bars permanently consume game space
* avoid visual jumps when entering or leaving fullscreen
* keep the gameplay layout stable across resume/pause transitions

#### 2. Respect safe area and display cutouts

All gameplay-critical UI must remain within safe bounds.

Requirements:

* no important UI under notches, hole-punches, rounded corners, or gesture-danger zones
* apply safe-area handling to board-adjacent UI and overlays
* keep score/stats, hold, next, and important actions fully visible
* support devices with and without cutouts

Implementation guidance:

* use Android window insets / cutout-safe handling
* treat insets as required layout constraints, not cosmetic spacing

#### 3. Remove bottom controls except Hard Drop

Simplify the bottom control area.

Requirements:

* remove Left, Right, Rotate, and Soft Drop bottom buttons
* keep only a visible Hard Drop button
* ensure the remaining layout still looks intentional and balanced
* Hard Drop must remain easy to reach and clearly actionable

Implementation guidance:

* keep a large touch target
* do not leave awkward empty space where removed controls used to be
* reposition or resize the remaining button as needed

#### 4. Make menus almost opaque

Increase overlay legibility and visual separation from gameplay.

Requirements:

* pause/game over/other menu overlays should be almost opaque
* the board can remain faintly visible behind overlays, but only slightly
* text and buttons must be easy to read
* overlays should feel polished and deliberate, not washed out

Suggested target:

* menu panel opacity roughly 90–95%
* background dimming strong enough to clearly separate menu state from gameplay

### Acceptance criteria

The task is complete when all of the following are true:

1. Gameplay screen uses fullscreen presentation during play.
2. No critical UI is obscured by cutouts, rounded corners, or gesture/navigation areas.
3. Bottom controls only show Hard Drop.
4. Hard Drop remains functional, visible, and easy to tap.
5. Pause/game over/menu overlays are almost opaque and clearly readable.
6. Layout feels tighter and more board-focused after control removal.
7. No obvious clipping, overlap, or spacing regressions appear across common device shapes.

### QA expectations

Verify behavior on:

* a device/emulator with display cutout
* gesture navigation
* 3-button navigation
* taller portrait aspect ratios
* pause/game over states
* app resume after backgrounding

### Notes

* Do not redesign unrelated gameplay systems.
* Keep changes scoped to UI/layout/overlay behavior for this task.
* Prefer minimal, maintainable Kotlin/Android-native changes over large architectural rewrites.
* If fullscreen or inset handling exposes existing layout assumptions, fix only what is necessary to satisfy this task.

### Deliverable

Provide:

* the implementation changes
* a short summary of what changed
* any follow-up issues discovered but left out of scope
