# Technical Specification: Client.java Decompression

This document details the plan to extract legacy gameplay systems from `Client.java` and migrate them to modern Kotlin Services. This is the highest priority task for stabilizing the core engine.

---

## 1. Interaction Service Migration (Trade & Duel)
Currently, `Client.java` manages complex state for trading and dueling using primitive flags and arrays. This logic is brittle and hard to debug.

### 1.1 Target: `net.dodian.uber.game.engine.systems.interaction.InteractionService`
*   **State Extraction:** Move the following fields to a `PlayerInteractionState` Kotlin object:
    *   Trade: `inTrade`, `tradeRequested`, `tradeConfirmed`, `tradeConfirmed2`, `trade_reqId`.
    *   Duel: `inDuel`, `duelRequested`, `duelConfirmed`, `duelConfirmed2`, `duelFight`, `duel_with`, `duelRule[]`, `duelBodyRules[]`.
*   **Logic Migration:**
    *   `openTrade()`, `declineTrade()`, `confirmTrade()` -> `TradeService.kt`
    *   `openDuel()`, `declineDuel()`, `confirmDuel()`, `startDuel()` -> `DuelService.kt`
*   **Benefit:** Centralizes interaction rules (like distance checks and "busy" states) and prevents duplication exploits by using a unified state machine.

---

## 2. Skill Logic Extraction
Several skills still have "hooks" or full logic blocks inside `Client.java` that should be moved to the `SkillPlugin` system.

### 2.1 Prayer
*   **Audit:** `PrayerManager` is already a separate class, but `Client.java` still handles some bone burying logic.
*   **Action:** Ensure all `Item on Object` (Altars) and `Item Click` (Bones) are registered via `SkillPlugin`.

### 2.2 Slayer
*   **Audit:** `Client.java` has hardcoded checks for Slayer equipment (Slayer Helmet, Spiny Helmet) in the defense/combat logic.
*   **Action:** Create a `SlayerService` that provides extension functions for `Player` to check for active task bonuses, moving the logic out of the main combat loop.

### 2.3 Thieving
*   **Audit:** Legacy `ObjectInteractionListener` cases still call `Thieving.attempt`.
*   **Action:** Fully migrate these to `bindObjectContent` in `ThievingSkillPlugin`.

---

## 3. Packet Delegation Audit
The move to Netty is 90% complete, but we need to ensure the "last mile" of packet handling is clean.

*   **Task:** Audit all classes in `net.dodian.uber.game.netty.listener.in`.
*   **Standard:** A Listener should **ONLY** perform:
    1.  ByteBuf Decoding.
    2.  Basic range/sanity checks.
    3.  Call a Kotlin Service.
*   **Anti-Pattern:** If a Listener contains a `switch` statement or `if/else` logic that changes player stats or moves items, it must be refactored.

---

## 4. Field & Memory Cleanup
`Client.java` contains many fields that are no longer used or have been superseded by Kotlin state objects.

*   **Social Fields:** (DONE) Removed `friends` and `ignores` lists.
*   **Quest/Minigame Flags:** Many `public int` flags for old quests should be moved to a `Json`-backed `QuestState` map to reduce the base memory footprint of every player object.
*   **Action Timers:** Consolidate `lastAction`, `lastMagic`, `lastPickAction`, etc., into a single `ActionTimerManager` or use the `TaskSystem`.

---

**Goal:** Reduce `Client.java` from 4,800+ lines to under 2,000 lines while maintaining 100% feature parity.
