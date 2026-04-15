# Skills Refactor & Production Roadmap

This document outlines the required work to standardize all skills, fix architectural debt, and ensure a high-quality experience for live players.

---

## 🏗️ 1. Core Standardization (Highest ROI)
Every skill must follow the same lifecycle and registration patterns.

*   [ ] **Standardize Plugin Registration:** Ensure ALL skills use the `SkillPlugin` interface and are registered in `SkillPluginRegistry`. (Currently: Some are standalone objects, some are class-based).
*   [ ] **Event-Driven Rewards:** Replace direct `player.addItem` or `addXp` calls with `GameEventBus` signals where appropriate (e.g., `SkillingSuccessEvent`).
*   [ ] **Unified Requirement System:** Migrate all skill-level checks to the `net.dodian.uber.game.skill.runtime.requirements` package for consistent "Missing Requirement" messaging.

---

## 🏃 2. Agility: Traversal suite Hardening
Agility is currently brittle and allows for "ghosting" or "noclip" errors during complex animations.

*   [ ] **Path Persistence:** The `AgilityTraversalService` must lock the player's movement until the entire animation sequence is verified as complete.
*   [ ] **Edge-Case Safety:** Ensure player state (e.g., `UsingAgility`) is reset if a player disconnects mid-traversal.
*   [ ] **Course State Audit:** Migrate `agilityCourseStage` to a more robust `AgilitySession` state object rather than a raw integer in `Player.java`.

---

## 🧤 3. Thieving: Interaction Refactor
The current thieving system is heavily dependent on hardcoded coordinates.

*   [ ] **Dynamic Stall Orientation:** Remove hardcoded face calculations in `Thieving.attempt`. Use `GameObjectData` rotation to determine the correct interaction tile dynamically.
*   [ ] **NPC Pickpocketing Migration:** Move pickpocketing logic for Farmers and Master Farmers out of legacy NPC files and into the `Thieving` skill plugin.
*   [ ] **Loot Table Standardization:** Move thieving loot to a structured `LootTable` system rather than raw `intArrayOf` in `ThievingData.kt`.

---

## ⚔️ 4. Slayer: Legacy Migration
Slayer is still heavily intertwined with `Client.java` and legacy NPC death logic.

*   [ ] **Plugin Conversion:** Move all Slayer task assignment and tracking logic to a `SlayerSkillPlugin`.
*   [ ] **Event-Driven Kill Tracking:** Implement a `NpcDeathEvent` listener in the Slayer plugin to update tasks, removing the need for `Client.java` to "know" about Slayer during combat.
*   [ ] **Slayer Equipment Logic:** Centralize the effects of Slayer Helmets, Leaf-bladed weapons, and Salt shakers into the `Slayer` system.

---

## ⚒️ 5. Smithing & Crafting: Interface Stability
The production skills (Smithing/Crafting) often suffer from interface desync.

*   [ ] **Production Task Suite:** Ensure both skills use the `ProductionTask` system for consistent "Make-X" behavior and interrupt handling (e.g., walking away cancels the task).
*   [ ] **Smithing Interface Audit:** Fix the `SmithingInterface` button IDs. Ensure that clicking a bar type correctly updates the displayed products based on the user's level.

---

## 🌿 6. Farming: API & Hooks
Provide support for the ongoing Farming work without bloating the core engine.

*   [ ] **Farming Lifecycle Hooks:** Add `onTick`, `onLogin`, and `onLogout` signals to the `Farming` system.
*   [ ] **Object Binding Audit:** Ensure all farming patches are registered via `bindObjectContent` rather than manual `ObjectInteractionListener` cases.

---

## 📜 7. General Cleanup
*   [ ] **Remove Skill Logic from `Client.java`:**
    *   `Prayer` (Burying and Altar offering)
    *   `Slayer` (Task management)
    *   `Thieving` (Pickpocketing)
*   [ ] **Button Audit:** Remap all "Unknown" buttons in `button_ids_export.txt` that belong to skill guides or skill menus.
*   [ ] **Operational Telemetry:** Add skilling-specific counters (e.g., `skill.mining.success`) to track economy health in real-time.

**Goal:** All skills should be 100% decoupled from the core engine by the end of this refactor.
