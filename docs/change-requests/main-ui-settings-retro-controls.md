# Change Request: Settings, Retro Controls, and Music Selection

## Status

- Status: Implemented
- Completed: 2026-04-08
- Milestones affected: M6, M7

## Request

Update the main gameplay UI and settings flow to support:

- on-screen control buttons for left, rotate, right, and hard drop
- a new settings screen opened from a cog icon in the top bar
- toggles for showing buttons, enabling gestures, and enabling music playback
- a selectable main tune in the music playlist, marked by a checkbox
- a more retro visual treatment for controls and score UI
- keyboard-safe layout behavior on the game-over screen

This request is focused on UI, settings, and music behavior. Core gameplay logic should remain intact unless it is needed to wire these options through the UI.

---

## 1. Settings Rules

### New persisted options

Add the following settings:

- `buttonsEnabled`
- `gesturesEnabled`
- `musicEnabled`
- `mainTrackPathOrUri`

Keep the existing `isMuted` setting unchanged.

### Required behavior

- At least one of `buttonsEnabled` or `gesturesEnabled` must remain `true` at all times.
- `buttonsEnabled` and `gesturesEnabled` are independent settings, but the UI must prevent disabling the last active input method.
- `musicEnabled = false` disables MOD music playback everywhere in the app.
- `musicEnabled` must not affect sound effects or vibration.
- `isMuted` remains the separate global mute control for all audio and vibration.
- Effective MOD playback is enabled only when `musicEnabled = true` and `isMuted = false`.

### UI implications

- The settings screen must make the “at least one input method enabled” rule clear.
- If one input method is already off, the other toggle should be disabled or blocked from turning off.

---

## 2. Settings Screen

### Entry point

- Add a cog icon in the top bar near the audio button.
- This opens a dedicated settings screen.
- Keep the existing long-press on the mute icon as a shortcut to the music library.

### Navigation behavior

- The settings screen should slide in from the right.
- It should behave like a separate panel over the main game screen.
- The transition should feel lightweight and arcade-like, not modal-heavy.
- Opening settings while gameplay is running should pause the game.
- Back should close the top-most panel first.
- The app should support stacked panel flow: `Game -> Settings -> Music Library`.
- Long-pressing the mute icon may still open the music library directly as a shortcut: `Game -> Music Library`.

### Settings content

Include controls for:

- show buttons
- enable gestures
- enable music
- main tune selection entry point

### Main tune selection

- The main tune is a single track selected from the playlist.
- Use a checkbox-style selection in the playlist menu.
- Only one track can be the main tune at a time.
- The playlist can be opened from the settings screen as the main tune selection flow.
- When the game starts, the selected tune should play first if music playback is enabled.
- If the selected tune is missing or unavailable, fall back to the first available valid track.

---

## 3. Game Start Music Behavior

### Main tune playback

- At game start, the app should attempt to play the selected main tune.
- This happens only if `musicEnabled = true` and `isMuted = false`.
- If no main tune is selected, use a fallback from the current playlist.
- If `musicEnabled = false`, game start must not auto-start MOD playback.
- If `musicEnabled` is turned off while a MOD track is playing, playback must stop immediately.
- If `musicEnabled = false`, pause/resume and background/foreground flows must not restart MOD playback.

### Separation from mute

- `musicEnabled` controls only MOD music playback.
- Existing mute controls still silence everything, including SFX and vibration.

### Playlist behavior

- The playlist screen should support marking one item as the main tune.
- The checkbox state should be persisted in settings.
- The current main tune should be visible in the playlist UI.
- If `musicEnabled = false`, the playlist remains visible for browsing and main-tune selection, but it must not allow MOD playback preview or manual play.
- If the playlist was opened from settings, Back returns to settings.
- If the playlist was opened directly from the long-press mute shortcut, Back returns to the main game screen.

---

## 4. Gameplay Controls

### New visible controls

Add on-screen buttons for:

- move left
- rotate
- move right
- hard drop

### Layout direction

- Add large vertical move buttons on the left and right edges of the screen.
- Add the rotate and hard-drop controls along the bottom.
- Keep small margins between the board area and screen edges.
- The layout should feel like an arcade cabinet rather than a material-style mobile app.

### Layout structure

- Restructure the gameplay stage into distinct zones: center board area, left control gutter, right control gutter, and bottom control row.
- The board interaction surface must be scoped to the board area only.
- Side and bottom controls must not overlap the board gesture region.
- Hold and next panels should remain visually tied to the board area while fitting within the new stage layout.

### Control visibility

- Show buttons only when `buttonsEnabled = true`.
- If buttons are disabled, gesture control may remain enabled.

### Existing gestures

- Keep gesture handling available when enabled.
- If gestures are disabled, touches on the board must not trigger movement, rotation, or drop actions.
- If buttons are disabled, the side and bottom control zones should collapse or become inert without destabilizing the board layout.

---

## 5. Top Bar Redesign

### Top icons

The top bar should contain only icons, with no button background or border:

- help
- settings
- audio / mute

### Visual treatment

- Icons should sit cleanly on the background.
- Do not use filled button chrome.
- Keep the top row compact and visually quiet.

---

## 6. Retro Button Styling

### Control buttons

Rework the control buttons to look more retro:

- grey border
- grey background
- icon color should match or contrast with the main background
- subtle inset or shadow effect
- slight rounding only

### Scope

Apply the same visual language to:

- bottom control buttons
- large side movement buttons
- related in-game control panels where appropriate

### Intent

The result should feel like a classic arcade control panel, not a Material 3 default.

---

## 7. Score and HUD Styling

### Header treatment

The top score header should be revised so:

- `SCORE | LEVEL | ROWS` is centered
- labels are more compact
- the header uses less vertical and horizontal space

### In-game HUD scope

Update the in-game HUD panels to match the retro UI direction:

- remove borders
- make the table larger overall
- keep the content readable and less cramped
- use smaller cell labels so the values fit cleanly
- apply the same retro treatment to related gameplay panels such as hold and next

### Visual consistency

- The in-game HUD should be restyled in the same pass as the buttons.
- It should visually align with the new arcade-like presentation.
- This change does not require redesigning the separate scoreboard screen.

---

## 8. Next Queue Presentation

### Requirement

- Make the next-block queue container opaque.

### Purpose

- Prevent the queue background from interfering visually with the active board and current piece.
- Preserve clarity in the top-right preview area.

### Related note

- The container should read as a distinct UI panel rather than a translucent overlay.

---

## 9. Keyboard-Safe Layout

### Desired behavior

- When the keyboard opens during the game-over flow, the game-over overlay should reposition within the remaining visible area above the IME.
- The main game layout behind the overlay does not need to shift as a whole.
- The nickname text field should stay centered in the remaining visible space.

### Game-over flow

- The game-over screen should reflow around the keyboard.
- The text field should remain usable and visually centered in the available area.
- Avoid compressing the overlay into the bottom portion of the screen.

### Implementation intent

- Use IME/insets-aware layout handling.
- Preserve the overall composition of the screen while lifting only the game-over overlay above the keyboard.

---

## 10. UI Flow Summary

### Main screen

- Top bar: help, settings, mute icons
- Board centered
- Opaque next queue panel
- Retro side controls and bottom controls when enabled
- Gesture input optional, as long as at least one input method remains enabled

### Settings screen

- Slides in from the right
- Contains toggles for buttons, gestures, and music
- Exposes main tune selection
- Pauses gameplay while open

### Playlist screen

- One track can be selected as the main tune via checkbox
- Main tune is used automatically at game start

---

## 11. Suggested Implementation Order

1. Extend `SettingsRepository` with the new preference keys and dependency rule.
2. Add settings fields to `GameUiModel` and wire them through `GameViewModel`.
3. Add the new settings screen and slide-in transition.
4. Update the music library UI to support selecting a main tune.
5. Wire game-start playback to the selected main tune.
6. Add the new on-screen controls and gesture gating.
7. Restyle top icons, control buttons, and the in-game HUD.
8. Make the next queue container opaque.
9. Fix keyboard behavior on the game-over screen.
10. Update strings and accessibility content descriptions.

---

## 12. Validation Checklist

- Buttons toggle on and off correctly.
- The settings UI prevents disabling the last active input method.
- Gestures can remain enabled when buttons are disabled.
- Music playback can be disabled without muting SFX or vibration.
- When `musicEnabled = false`, no MOD playback starts from game start, resume, background restore, or manual playlist actions.
- The selected main tune is used on game start.
- The playlist checkbox reflects the persisted main tune.
- The settings screen slides in from the right.
- Opening settings pauses gameplay when a run is active.
- Back closes panels in stack order.
- The top bar shows icon-only controls with no background or border.
- The score header is centered and compact.
- The in-game HUD panels no longer use borders and remain readable.
- The next queue container is opaque.
- The game-over overlay lifts above the keyboard instead of compressing into the bottom of the screen.
- The nickname field stays centered in the remaining visible space.

---

## Outcome

This change request updates the gameplay UI into a more arcade-like configuration while adding a dedicated settings flow, explicit control over gestures and music playback, and a selectable main tune for the playlist. The design also tightens the score presentation, queue readability, and keyboard handling on the game-over flow.

## Delivered status

- Implemented settings persistence for buttons, gestures, music enablement, and preferred main tune.
- Added stacked `Game -> Settings -> Music Library` panel flow with right-slide transitions.
- Added retro-styled on-screen controls and gesture gating tied to settings.
- Updated the top bar and HUD styling toward the requested arcade presentation.
- Added main-tune checkbox selection in the music library and game-start playback preference.
- Blocked MOD playback when music is disabled while preserving browsing and tune selection.
- Lifted the game-over overlay with IME-aware padding so it remains usable with the keyboard open.
