# Technical Debt: Collection & Memory Optimization

This document outlines specific areas where legacy Java collections (`ArrayList`, `HashMap`, `CopyOnWriteArrayList`) should be replaced with primitive arrays or zero-allocation structures to improve server performance and reduce GC pressure.

---

## 🏗️ 1. High ROI: Interaction Structures
These collections are modified frequently during player-to-player interactions and currently cause unnecessary object allocations.

*   [ ] **Trade & Duel Offered Items:** 
    *   **Current:** `CopyOnWriteArrayList<GameItem> offeredItems` in `Client.java`.
    *   **Optimization:** Replace with `GameItem[28]` (fixed-size array).
    *   **Why:** `CopyOnWriteArrayList` is extremely expensive for frequent writes (every item offered causes a full array copy). Since trade/duel windows have a fixed size, a standard array is significantly faster.
*   [ ] **Bank Style Views:**
    *   **Current:** `ArrayList<Integer> bankStyleViewIds` and `bankStyleViewAmounts`.
    *   **Optimization:** Replace with `int[]`.
    *   **Why:** Removes `Integer` object wrapping for every single item ID and amount displayed in specialized bank views (like Loot interfaces).

---

## ⚡ 2. Engine & Registry Optimizations
These collections are accessed every server tick (600ms) and directly impact the "Tick Budget."

*   [ ] **Player Registry Scans:**
    *   **Current:** `val locals = ArrayList<Player>(256)` in `PlayerRegistry.kt`.
    *   **Optimization:** Use a reusable `InternalPlayerBuffer` array or a `Sequence` that doesn't allocate a new list every time `forEach` is called.
    *   **Why:** Allocating a new `ArrayList` for every local scan across all players adds significant garbage collection pressure during high player counts.
*   [ ] **Primitive Map Migration:**
    *   **Current:** `ConcurrentHashMap<Long, Client> playersOnline`.
    *   **Optimization:** Use a primitive-optimized map (e.g., `Long2ObjectOpenHashMap` from fastutil or similar).
    *   **Why:** Standard `HashMap` wraps every `Long` key into an object. A primitive map stores the `long` keys directly, reducing memory overhead and improving lookup speed.

---

## 📦 3. State & Persistence Cleanup
Lower priority but important for long-term memory health.

*   [ ] **Slayer Data & Travel Data:**
    *   **Current:** `ArrayList<Integer> slayerData` and `ArrayList<Boolean> travelData` in `PlayerProgressState.java`.
    *   **Optimization:** Replace with `int[]` and a `BitSet` (for booleans).
    *   **Why:** `slayerData` is a fixed set of integers; `travelData` (unlocked locations) is much more memory-efficient as a bitmask/BitSet than a list of `Boolean` objects.
*   [ ] **NPC Drop Lists:**
    *   **Current:** `ArrayList<NpcDrop> drops` in `NpcData.java`.
    *   **Optimization:** Replace with `NpcDrop[]`.
    *   **Why:** NPC drops are static data loaded from the database/cache. They never change after server startup, so an immutable array is the most efficient structure.

---

## 📜 4. Implementation Rules for Developers
1.  **Prefer Arrays for Fixed Sizes:** If a container has a maximum size (Inventory = 28, Equipment = 14), **always** use a primitive array.
2.  **Avoid Object Wrappers:** Never use `ArrayList<Integer>`, `ArrayList<Long>`, or `ArrayList<Boolean>`. Use `int[]`, `long[]`, or `BitSet`.
3.  **Use `internal` for Reusable Buffers:** If a service needs a temporary list during a tick, define it as a private/internal reusable array to avoid `new` allocations inside loops.

**Goal:** Reduce "Young Gen" garbage collection frequency by 30% and improve tick stability.
