## Implementation task for agent

**Goal:**
Refresh the Android gameplay UI of **ModFall** using the existing **Jetpack Compose** setup, while preserving the current game logic. The implementation should follow the **visual vibe and mood of the attached concept image**: dark premium neon, subtle glow, clean layout, embedded previews, and light floating controls.

**Important:**
Please use the attached image as the primary visual direction and keep its overall **feel, atmosphere, spacing, and premium dark-neon vibe**. The result does not need to match the image pixel-for-pixel, but it should clearly preserve the same aesthetic direction.

---

## Context

This repo contains the game previously called BlockDrop before the rename to ModFall.

The desired gameplay screen style is:

* dark navy / deep blue background
* subtle glow, not excessive bloom
* premium mobile game feel
* minimal and clean
* no heavy floating side cards
* no large bottom control bar
* hold and next previews integrated into the playfield background
* floating ghost-style controls
* top HUD with help / settings / sound
* support for split controls:

  * move left
  * move right
  * rotate left
  * rotate right
  * soft drop

If hard drop already exists, keep it working, but it does not need to become a prominent visible button.

---

## Main request

Find the existing Compose gameplay screen and rework the gameplay UI layer so it matches the attached visual concept as closely as practical within the current architecture.

Do **not** rewrite the core gameplay logic.
Do **not** change scoring, collision, spawning, or line-clear rules.
Focus on the rendering/UI/input layer.

---

## Scope

### Please do

1. Find the existing Compose entry point for the gameplay screen.
2. Identify where the following are currently sourced from:

   * board matrix / settled blocks
   * active piece
   * ghost piece
   * hold piece
   * next queue
   * score / level / lines
   * input actions
3. Implement a refreshed Compose-based gameplay UI with these parts:

   * `GameScreen`
   * `TopHud`
   * `Playfield`
   * embedded `HoldPreview`
   * embedded `NextPreview`
   * `ControlPadOverlay`
4. Keep all existing gameplay logic working.
5. Minimize architectural disruption. If the current rendering path is unusual, make the smallest safe changes needed to support the new UI.

### Please do not

* do not rewrite core game logic
* do not introduce gameplay rule changes
* do not perform a large unrelated refactor
* do not replace working systems unless necessary for the UI refresh

---

## Visual direction

Follow the **vibe of the attached image**:

* dark premium background
* subtle blue glow
* clean and elegant composition
* embedded UI instead of heavy cards
* board remains the visual hero
* controls are present but visually quiet
* overall look should feel polished, atmospheric, and slightly futuristic while still arcade-inspired

The design should feel **close in mood and style to the attached concept image**.

---

## Design requirements

### 1. Overall style

The gameplay screen should be:

* dark
* clean
* minimal
* premium
* slightly futuristic / arcade-inspired
* visually calm, not noisy

Use:

* deep blue / navy gradients
* soft glow accents
* subtle borders
* restrained translucency
* low visual clutter

---

### 2. Playfield

The playfield is the main focal point.

Requirements:

* centered tall board
* subtle glowing frame
* very light grid
* no heavy side panels attached to screen edges
* hold and next must be integrated into the playfield/background rather than looking like separate floating cards

### Embedded hold / next

* `DRŽET` should appear in the upper-left area inside the playfield region
* `DALŠÍ` should appear in the upper-right area inside the playfield region
* both should be low-contrast and feel embedded into the background layer
* hold preview should use a subtle outlined preview area
* next preview should be a slim, subtle vertical stack inside the board region
* previews should not visually overpower the board itself

These elements should preserve the mood shown in the attached image, but improved to match these layout requirements.

---

### 3. Tetromino styling

Each block should have:

* a clean body
* a light top highlight
* a subtle glow
* only mildly rounded corners

Requirements:

* active piece should have stronger glow
* settled blocks should have weaker glow
* ghost piece should be outline-only or nearly outline-only
* ghost piece should remain readable but unobtrusive

Please avoid a cheap over-blurred look. The glow should feel layered and controlled.

---

### 4. Controls

Replace any heavy bottom bar approach with floating overlay controls.

### Preferred layout

Use split controls like this:

```text
   [rotate left]       [rotate right]

[left]        [down]        [right]
```

### Control style

* semi-transparent
* subtle border
* light glow
* no large bottom dock
* rounded rectangle or similarly soft shape
* on press:

  * slight scale down
  * slightly stronger opacity/glow

Controls should be visible enough to use, but quiet enough that the board remains dominant.

---

### 5. Top HUD

Top section should include:

* help on the left
* settings in the center
* sound on the right

Also include score info in Czech:

* `Skóre`
* `Úroveň`
* `Řádky`

Preferred behavior:

* score should be more legible than secondary info
* overall HUD should feel cohesive and polished, not like a debug overlay

---

## Technical requirements

### 1. Compose-first

Use Jetpack Compose for the UI.

Preferred approach:

* playfield frame/background via Compose drawing and/or Canvas
* grid via Canvas
* block rendering via Compose or Canvas, whichever best fits the existing code
* controls as Compose composables
* icons as vector assets / ImageVector, not bitmaps

---

### 2. Design tokens

Introduce or centralize gameplay UI tokens, such as:

* background colors
* frame color
* grid color/alpha
* primary and secondary text colors
* button fill/border/icon colors
* corner radii
* glow strengths
* spacing

Example file:

* `GameUiTokens.kt`
* or equivalent theme/helper file

Goal:

* reduce scattered magic numbers
* make future tuning easy

---

### 3. Performance

Use glow and blur carefully.

Requirements:

* do not apply expensive blur indiscriminately
* prefer efficient layered drawing where possible
* active piece may have the strongest glow
* settled blocks should use lighter treatment
* grid should be drawn as a single canvas layer rather than many tiny composables

If tradeoffs are needed, prefer stable performance over excessive effects.

---

### 4. Input wiring

Wire the control overlay into the existing input system.

Required actions:

* move left
* move right
* rotate left
* rotate right
* soft drop

If rotate-left does not currently exist:

* inspect how rotation is handled now
* add the smallest safe support needed
* do not break keyboard, gesture, or gamepad input if they already exist

---

## Suggested implementation steps

### Step 1

Find the current gameplay Compose screen / entry point.

### Step 2

Map where these come from:

* board state
* active piece
* hold piece
* next pieces
* score / level / line count
* input handlers

### Step 3

Introduce a refreshed composable structure with minimal disruption:

* `GameScreen`
* `TopHud`
* `Playfield`
* `HoldPreview`
* `NextPreview`
* `ControlPadOverlay`

### Step 4

Implement the new playfield look:

* background
* frame
* subtle grid
* embedded hold/next
* active / settled / ghost rendering treatment

### Step 5

Implement floating split controls.

### Step 6

Implement top HUD and score hierarchy.

### Step 7

Polish pressed states, opacity, spacing, glow balance, and visual hierarchy so the final screen stays close to the attached image’s vibe.

---

## Acceptance criteria

1. Existing gameplay still works.
2. The new gameplay UI clearly looks cleaner and more modern than before.
3. The final result preserves the **visual vibe of the attached concept image**.
4. `DRŽET` and `DALŠÍ` are embedded into the playfield/background rather than appearing as heavy separate cards.
5. Controls do not use a large bottom bar.
6. There are distinct controls for:

   * move left
   * move right
   * rotate left
   * rotate right
   * soft drop
7. Active piece glow is stronger than settled piece glow.
8. Ghost piece remains clear and readable.
9. Top HUD feels cohesive and visually quiet.
10. Styling values are reasonably centralized.
11. If architectural constraints require compromise, document them clearly.

---

## Deliverables from the agent

Please provide:

1. a list of the relevant files you found
2. a short explanation of how gameplay rendering and input are currently wired
3. a list of modified or added files
4. a short summary of the UI decisions you made
5. any performance or architecture compromises you had to make

---

## Suggested PR title

**Refresh gameplay UI with embedded previews and floating Compose controls**

## Suggested PR summary

* redesign gameplay screen in Compose
* preserve the visual vibe of the attached concept image
* embed hold/next into the playfield background
* add floating split controls for left/right/rotate left/rotate right/down
* improve playfield styling, grid, ghost piece, and block glow
* centralize gameplay UI tokens for easier tuning

---

## Current state

The requested gameplay UI refresh has been implemented and then iterated beyond the original request. The current shipped state is:

* gameplay still runs through the existing Compose gameplay screen and existing game logic remains intact
* top HUD uses help left, settings center, sound right, with score visually prioritized over level and lines
* the playfield is the main hero element with a darker blue premium treatment, lighter grid, softer frame, and stronger block glow
* `DRŽET` and `DALŠÍ` are aligned above the playfield border and their preview regions are integrated into the board rendering
* button controls are floating overlays and no longer reserve layout space below the board
* help and scoreboard now open as standalone full-screen panels instead of dialog overlays
* tutorial screen has a fixed title and a fixed bottom button
* scoreboard is also a full-screen panel with a fixed bottom close button
* panel buttons use the same visual family as gameplay controls, but with higher opacity
* optional particles were added in settings, with a density control and hard-drop burst support
* a simplified centered `4 ROW` grow-and-fade celebration text replaced the old boxed Tetris celebration

Known limitation:

* particle reactions are implemented, but they still need another visibility pass if the goal is for every move/rotate/soft-drop reaction to be immediately obvious during live gameplay

---

## Implementation checklist

- [x] Confirm the gameplay entry point remains the single Compose screen for gameplay.
- [x] Keep current game logic unchanged: scoring, collision, spawning, line clears, hold rules, and ghost placement.
- [x] Preserve the dark navy / deep blue premium background from the concept image.
- [x] Reduce visual noise so the board stays the focal point.
- [x] Keep glow subtle and controlled, not blurry or overbloomed.
- [x] Rework the HUD to match the screenshot mood.
- [x] Place help on the left, settings in the center, and sound on the right.
- [x] Make score text more prominent than level and lines.
- [x] Use Czech labels `Skóre`, `Úroveň`, and `Řádky`.
- [x] Make the playfield a centered tall hero element.
- [x] Keep the frame glow, but make it softer and more premium.
- [x] Use a very light grid with low contrast.
- [x] Embed hold and next into the playfield/background instead of separate heavy cards.
- [x] Place `DRŽET` in the upper-left of the playfield region.
- [x] Place `DALŠÍ` in the upper-right of the playfield region.
- [x] Make the hold preview subtle, outlined, and low contrast.
- [x] Make the next preview a slim vertical stack that feels embedded.
- [x] Keep the ghost piece clearly readable but nearly outline-only.
- [x] Ensure active blocks glow more strongly than settled blocks.
- [x] Keep settled block glow weaker and more restrained.
- [x] Use mildly rounded block corners with a clean body and top highlight.
- [x] Replace the bottom control bar feel with floating overlay controls.
- [x] Support split controls for move left, move right, rotate left, rotate right, and soft drop.
- [x] Keep hard drop working, but visually de-emphasized.
- [x] Add pressed-state feedback for controls with slight scale down and stronger opacity or glow.
- [x] Centralize styling values in `GameUiTokens.kt` or an equivalent helper.
- [x] Avoid scattered magic numbers in the gameplay UI.
- [x] Keep performance reasonable by limiting blur use to a few key layers.
- [x] Verify the final screen still works on mobile portrait layouts.
- [x] Verify overlays still open correctly for tutorial, settings, music library, scoreboard, pause, and game over states.
- [x] Validate that the final composition matches the screenshot’s mood: dark premium neon, subtle glow, clean layout, embedded previews, and quiet floating controls.
