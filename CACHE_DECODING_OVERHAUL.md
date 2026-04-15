# Technical Specification: Cache Decoding & Pathfinding Overhaul

This document outlines the required updates to the server's cache decoding and collision systems to achieve perfect alignment with the visual world and fix long-standing interaction bugs.

---

## 1. Object Definition Overhaul (`loc.dat`)
We are currently skipping critical metadata during object decoding. This data is essential for "smart" interactions and correct collision state.

### 1.1 Restore Missing Opcodes
*   **Opcode 69 (Surroundings):**
    *   **Action:** Stop skipping and store as `interactionFaceMask`.
    *   **Usage:** Used to restrict which cardinal faces of an object allow interaction (e.g., Bank Booths only from the front).
*   **Opcodes 77 & 92 (Morphs):**
    *   **Action:** Decode `varbitId`, `varpId`, and `childIds[]`.
    *   **Usage:** Allows the pathfinder to "see" through doors or gates that are currently closed but can be opened.
*   **Opcode 74 (Hollow):**
    *   **Action:** Set `solid = false` and `impenetrable = false` if this opcode is present.
    *   **Usage:** Corrects collision for decorative arches or overhead features that players should be able to walk under.

### 1.2 Interactive Inference Logic
*   **Current:** Hardcoded rules in `ObjectDefinitionDecoder.kt`.
*   **Refactor:** Align with Mystic Client's logic. If an object has a `name` and `models`, it should be clickable unless explicitly marked otherwise. This ensures decorative but interactable world features (like certain levers or drawers) remain functional.

---

## 2. Terrain & Collision Mapping
Fixing the "Jesus Walking" and "Noclip" issues.

### 2.1 Water Tile Detection
*   **Problem:** Many water tiles lack the `BLOCKED` flag because the client renders them via overlay.
*   **Action:** Update `MapDecoder.decodeTile` to recognize **Overlay ID 6**.
*   **Rule:** If `overlay == 6`, the tile must be flagged as blocked in the `CollisionMatrix`.

### 2.2 Bridge Attribute (Flag 2)
*   **Action:** Ensure tiles with `attributes == 2` (Bridge) are handled correctly.
*   **Logic:** These tiles often allow movement on `plane + 1` while blocking `plane 0`. Our `CollisionBuildService` must account for this verticality.

---

## 3. The "Reachability" Suite (Port from Luna)
This is the highest ROI task for improving the "feel" of the game.

### 3.1 `reachedObject` Logic
Port the following specialized reach checks from the Luna project:
*   **Wall Interaction:** Allows interacting with a wall (Types 0-3) from a diagonal tile if the specific face is not blocked.
*   **Face-Aware Interaction:** Uses the `interactionFaceMask` (Opcode 69) to determine if the player is standing on a "valid" side of the object.
*   **Sized Object Adjacency:** Correctly handles 2x2, 3x3, and larger objects so the player doesn't try to stand "inside" them to interact.

---

## 4. Implementation Plan
1.  **Phase 1:** Update `GameObjectData.kt` and `ObjectDefinitionDecoder.kt` to store and decode the new opcodes.
2.  **Phase 2:** Update `MapDecoder.kt` and `CollisionBuildService.kt` to handle Water overlays and Bridge attributes.
3.  **Phase 3:** Implement the `InteractionReachService` (Kotlin) by porting the logic from Luna's `reachedObject` and `reachedWall` methods.
4.  **Phase 4 (Validation):** Test with Bank Booths (cannot bank from the side) and large gates (can click from diagonal).

---

**Goal:** Achieve 1:1 interaction parity with the original RS2 client behavior.
