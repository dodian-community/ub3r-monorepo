# Technical Specification: Easy Wins & Quick Fixes

This document details low-effort, high-impact tasks that will immediately improve the developer and player experience.

---

## 1. Timer Standardization (`TaskSystem`)
Many legacy systems still rely on manual timestamp comparisons which are difficult to pause or cancel.

*   **Action:** Audit `Client.java` for `System.currentTimeMillis()`.
*   **Refactor:** Replace manual cooldowns with the Kotlin `TaskSystem`.
*   **Benefit:** Timers are now tied to the game tick and are automatically cleared if a player disconnects, preventing "phantom" actions.

---

## 2. Appearance Persistence Fix
Players have reported their character appearance resetting to default (or turning invisible) during certain transitions.

*   **Cause:** The `PacketAppearanceService` may not be correctly handling the bitmask for equipment slots during the Netty update block generation.
*   **Action:** Debug the `NpcUpdateBlockSet` and `PlayerUpdateBlockSet` to ensure the "Appearance" block is correctly formatted and flushed.
*   **Benefit:** Resolves the most frequent visual bug reported during the Netty transition.

---



**Goal:** Polish the visual experience and secure the world for the first wave of beta testers.
