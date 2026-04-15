# Technical Specification: UI & Interaction Audit

This document outlines the strategy for resolving broken UI elements and inconsistent interaction logic caused by the updated client and rapid system migrations.

---

## 1. Global Button ID Audit
The updated client uses a different component layout for many interfaces. We must ensure every clickable button has a corresponding server-side binding.

### 1.1 Source: `button_ids_export.txt`
*   **Task:** Cross-reference every "Unknown" button in the export with the client's interface debug mode (or dump).
*   **Target:** Migrate all hardcoded `switch(actionButtonId)` logic from `Client.java` to `InterfaceButtonContent` implementations.
*   **Focus Areas:**
    *   **Sidebar Tabs:** Music, Emotes, Settings, and Prayer.
    *   **Skill Guides:** Ensure all level-up and guide buttons correctly paginate.
    *   **Teleport Menus:** Centralize teleport buttons into a single `TravelService`.

---

## 2. Dialogue Factory Migration
We are moving away from manual `sendFrame126` and `NpcChat` calls. The new `DialogueFactory` handles the complexity of the packet-level UI management.

### 2.1 The Standard Pattern
*   **Legacy:** `c.sendNPCChat("Hello", 591, "Man");`
*   **Modern (DSL):**
    ```kotlin
    player.dialogue {
        npc(Man, "Hello there!")
        options("What's up?", "Nothing.") {
            onOption1 { npc(Man, "Not much.") }
            onOption2 { stop() }
        }
    }
    ```
### 2.2 Migration Steps
1.  Search for all `showNPCChat` and `showPlayerChat` calls in `Client.java`.
2.  Replace with `player.dialogue { ... }` blocks.
3.  Ensure `DialogueOptionService` is correctly routing the button IDs (e.g., 9157, 9158) to the active factory instance.

---

## 3. World Interaction & Reachability
Fixing the "feel" of interacting with objects and NPCs.

### 3.1 Porting Luna's Reachability
*   **Wall Objects (Type 0-3):** Implement logic to allow diagonal interaction if the face is reachable.
*   **Large Objects (e.g., 2x2, 3x3):** Use the `direction` bitmask (Opcode 69) to block specific faces (e.g., you cannot "reach" a bank booth from the side).
*   **Water Overlay (ID 6):** Update `DecodedMapTile.isBlocked` to treat Overlay 6 as impassable terrain.

### 3.2 Object Interaction Policies
*   **Standardize Policies:** Ensure all interactive objects use a `PolicyPreset` (e.g., `GATHERING` or `BANKING`).
*   **Distance Checks:** Replace manual `if(distanceTo < 2)` with the `InteractionService` suite.

---

**Goal:** Zero "You can't reach that" errors when standing in a valid spot, and 100% button click coverage across all tabs.
