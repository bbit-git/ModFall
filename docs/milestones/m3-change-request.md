## Change Request: UI Layout & Gameplay Adjustments

### 1. Layout Refactor (High Priority)

**Goal:** Make the playfield the dominant element and reduce empty space.

#### Changes:

* Center the **game board** and increase its size to occupy ~70% of screen height.
* Reduce outer margins (top, bottom, sides) to a consistent spacing system (8–16px).
* Remove large empty gaps between components.

---

### 2. Top Bar Consolidation

**Current issue:** Stats are scattered and visually heavy.

#### Changes:

* Replace current layout with a single compact horizontal bar:

  * `SCORE | LEVEL | LINES`
* Keep it minimal height (~40–48px).
* Align:

  * Left: Help icon
  * Center: Stats
  * Right: Sound icon

---

### 3. Hold & Next Panels

**Goal:** Keep them visible but not dominant.

#### Changes:

* Position:

  * **Hold** → top-left of board
  * **Next** → top-right of board
* Reduce panel padding and border thickness.
* Scale content appropriately.

#### Special requirement:

* **Next block preview should be ~50% smaller than actual game blocks**

  * Important: preserve shape clarity (don’t scale below readability)
  * Use consistent scaling factor (e.g., `0.5 * tileSize`)
  * Center shapes inside preview box

---

### 4. Game Over Modal

**Current issue:** Too large and intrusive.

#### Changes:

* Reduce modal width to ~70–80% of board width
* Keep it centered over the board
* Maintain background visibility (no full blackout)
* Buttons:

  * `Restart` (primary)
  * `Menu` (secondary)
* Reduce vertical padding

---

### 5. Bottom Controls Redesign

**Current issue:** Feels disconnected and unclear.

#### Changes:

* Place controls in a **single horizontal row directly under the board**
* Use icon-based buttons:

  * ← (move left)
  * → (move right)
  * ↻ (rotate)
  * ↓ (soft drop)
  * ⬇ (hard drop)

#### Hard Drop (critical fix):

* Add **dedicated button** (far right recommended)
* Increase hit area (minimum 48x48px)
* Add visual feedback on press (scale or glow)

---

### 6. Interaction Improvements

#### Hard Drop:

* Ensure:

  * Immediate piece placement
  * No animation delay blocking input
* Add optional:

  * subtle flash or impact effect on landing

#### Touch responsiveness:

* Ensure no UI layer blocks input (especially overlays)
* Verify z-index of modal vs controls vs board

---

### 7. Visual Consistency

#### Spacing system:

* 8px: small gaps
* 16px: section spacing
* 24px max: major separation

#### Typography:

* Avoid wrapping labels like “Rotate CCW”
* Prefer:

  * icons
  * or short labels

---

### 8. Optional (Recommended Next Step)

* Add **ghost piece (shadow projection)** for better placement clarity
* Add subtle grid contrast to improve readability

---

## Summary (What actually changes)

* Bigger board
* Smaller, tighter side panels
* Compact top stats bar
* Clean bottom control row with **working hard drop**
* Next preview blocks at **50% scale**
* Smaller, less intrusive modal
