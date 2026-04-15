# Technical Specification: Architectural Cleanup & Lean-up

This document outlines the plan to organize the Kotlin source tree, harden API boundaries, and standardize event-driven communication.

---

## 1. Directory Rationalization
While sub-packages exist, the sheer number of classes in `sync` and `systems` can be overwhelming for content developers.

### 1.1 Goal: The "Internal" Engine
*   **Action:** Move low-level engine components (Networking, Synchronization, Metrics) into an `engine.internal` or `engine.core` package.
*   **Benefit:** Keeps the high-level `systems` package focused on things content developers actually touch (Combat, Trading, World events).
*   **Standardize Naming:** Ensure all service-level objects end in `Service` or `Manager` for consistent discovery.

---

## 2. API Hardening (Visibility Control)
We currently expose too many engine internals to the Plugin layer.

### 2.1 The `internal` Keyword
*   **Standard:** All classes in `net.dodian.uber.game.engine.sync` and `net.dodian.uber.game.netty` should be marked as `internal` where possible.
*   **Plugin Gateway:** Content should only interact with the engine via:
    1.  The `GameEventBus`.
    2.  The `ContentRuntimeApi`.
    3.  Extension functions on `Player` provided by high-level services.
*   **Audit:** Check for accidental leakage of `ByteBuf` or `Channel` objects into the `skill` or `npc` packages.

---

## 3. Event Bus Standardization
Reduce direct coupling between unrelated systems.

### 3.1 Migration to Signals
*   **Legacy:** `player.getSlayer().incrementKill()` called directly from `NPC.die()`.
*   **Modern:** 
    1.  `NPC.die()` posts an `NpcDeathEvent`.
    2.  `SlayerSkillPlugin` listens for `NpcDeathEvent` and updates the task.
*   **Task:** Audit `Client.java` and legacy `Npc` logic for direct class-to-class calls that should be signals.

### 3.2 Signal Documentation
*   **Action:** Create a `Signals.md` or update `package-info.java` in the `events` package listing all available events and their payloads.

---

## 4. Coroutine & Threading Safety
Prevent race conditions and memory leaks in the new async systems.

*   **Context Scoping:** Ensure all `GlobalScope.launch` calls are replaced with `player.scope.launch` or a managed `ServiceScope`.
*   **Blocking Audit:** Use a tool (or manual grep) to find `Thread.sleep` or `CountDownLatch.await` in the Kotlin tree. These must be removed or moved to a background dispatcher.

---

**Goal:** A "Black Box" engine where content developers only see what they need to build the game.
