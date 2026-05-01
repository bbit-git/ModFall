# 19 — main-ui-settings-retro-controls review follow-up

## Reported symptoms

The implementation for `docs/change-requests/main-ui-settings-retro-controls.md` does not yet match the documented UI direction.

Observed issues:

- gameplay buttons are not positioned correctly
- rounded corners are still used widely across the gameplay UI
- the overall look still reads as Material/modern rather than retro arcade
- hold and next components do not match the requested HUD treatment
- the held piece appears transparent and should not look transparent in normal use
- the top score HUD should be very compact, centered under the icon row, with no bordering and very small margin/padding

## Scope

This fix request is limited to the UI/layout/styling work already described in:

- `docs/change-requests/main-ui-settings-retro-controls.md`

Do not expand scope into gameplay logic changes unless needed for UI wiring.

## Required fixes

### 1. Correct gameplay control placement

The current stage layout does not match the documented arcade layout.

Requirements:

- use distinct gameplay zones:
  - left control gutter
  - center board area
  - right control gutter
  - bottom control row
- place `move left` in the left gutter
- place `move right` in the right gutter
- place `rotate` and `hard drop` in the bottom control row
- keep the board gesture region scoped to the board area only
- keep control zones from overlapping the board interaction surface
- if buttons are disabled, the board area should remain stable and the control zones should collapse cleanly

Implementation note:

- the current implementation stacks rotate and hard drop in the right gutter; this is incorrect for this task

### 2. Rework gameplay controls into a retro arcade look

The current controls still look too close to Material 3 defaults.

Requirements:

- remove the Material button feel from in-game controls
- use a retro grey control surface language
- use slight or minimal corner rounding only
- add a subtle inset/bevel/shadow treatment
- keep controls large and readable
- apply the same treatment consistently across:
  - left/right movement buttons
  - bottom rotate/hard-drop controls
  - related in-game control surfaces where appropriate

Implementation note:

- `FilledTonalButton` plus rounded card styling is not sufficient for this task

### 3. Tighten the top HUD layout

The score header should be quieter and more compact.

Requirements:

- top row should be icon-only: help, settings, mute
- move the score HUD to a separate compact row directly under the icon row
- center the score HUD horizontally
- remove border/chrome from the score HUD
- use very small padding and margins
- keep the presentation compact and readable
- preserve the centered `SCORE | LEVEL | ROWS` treatment in compact form

### 4. Restyle hold and next panels to match the requested HUD direction

The hold/next presentation still reads as bordered rounded cards.

Requirements:

- restyle hold and next in the same retro pass as the controls and score HUD
- remove the current rounded bordered card look
- keep the containers visually tied to the board area
- ensure the next queue container reads as a distinct opaque panel
- ensure the hold panel also uses the intended in-game HUD treatment

Clarification:

- “opaque next queue container” means it must not look like a translucent overlay floating over gameplay

### 5. Fix held piece appearance

The held piece should not look transparent during normal use.

Requirements:

- when hold is available, the held piece should render fully solid/normal
- if a disabled/unavailable state is still desired, keep it clearly readable and avoid making it look unintentionally washed out
- verify the final appearance against the retro HUD pass, not in isolation

## Acceptance criteria

This task is complete when all of the following are true:

1. Control placement matches the documented zone layout.
2. `move left` and `move right` are in side gutters.
3. `rotate` and `hard drop` are in a bottom control row.
4. The in-game controls no longer read as default Material buttons.
5. Rounded corners are reduced to a slight/minimal use and no longer dominate the gameplay UI.
6. The top icon row is compact and icon-only.
7. The score HUD is centered below the icon row, compact, and borderless.
8. Hold and next match the same retro HUD direction.
9. The next queue container is clearly opaque.
10. The held piece no longer appears unintentionally transparent in normal use.
11. No board/control overlap or spacing regressions are introduced on common portrait aspect ratios.

## Validation expectations

Verify at minimum:

- gameplay running with buttons enabled
- gameplay running with buttons disabled
- gestures enabled/disabled combinations
- compact header readability on narrow portrait screens
- hold available vs unavailable appearance
- next queue readability over active gameplay
- pause/game-over overlays still behaving correctly after the layout pass

## Deliverable

Provide:

- the implementation changes
- a short summary of what changed
- screenshots or a concise reviewer-ready description of the final layout structure
- any remaining visual compromises or follow-ups kept out of scope
