# Technical Specification: Performance & Infrastructure

This document outlines the plan to stabilize and verify the high-performance asynchronous systems introduced during the recent engine migration.

---

## 1. Async SQL & Non-Blocking Database Audit
The goal is to ensure the main game thread **never** waits for the database.

### 1.1 The "Forbidden" Operations
*   **Search Pattern:** Identify any usage of `java.sql.ResultSet`, `Statement.executeQuery`, or `PreparedStatement.executeUpdate` that is not wrapped in an async wrapper.
*   **Action:** Migrate all blocking legacy SQL in `Client.java` and `NPC` loading to `AsyncSqlService.execute { ... }`.
*   **Target:** `AccountPersistenceService`, `CharacterLoading`, and `LogEntry` subclasses.

---

## 2. Coroutine Lifecycle & Memory Safety
With the introduction of Coroutines, we must ensure background tasks are tied to the appropriate lifecycle.

### 2.1 Player-Scoped Coroutines
*   **Standard:** Coroutines launched for a specific player (e.g., a timed skilling task or a delayed message) must use a `CoroutineScope` that is cancelled upon `PlayerLogoutEvent`.
*   **Audit:** Check for any usage of `GlobalScope` or custom scopes that lack a cancellation hook.
*   **Benefit:** Prevents "Memory Leaks" where the server continues processing logic for players who are no longer online.

---

## 3. Farming WIP API (The Farming Hook System)
To support our other developer without causing conflicts in `Farming.kt`.

### 3.1 `FarmingPatchRegistry`
*   **Action:** Create a registry that allows content plugins to define a "Patch" without modifying the core growth engine.
*   **Pattern:**
    ```kotlin
    FarmingPatchRegistry.register(
        objectId = 8151,
        type = PatchType.ALLOTMENT,
        position = Position(2666, 3374, 0)
    )
    ```
*   **Benefit:** Decouples the *logic* of farming (how things grow) from the *data* of farming (where the patches are).

---

## 4. Operational Telemetry & Stress Testing
Verify the 300-player stability claim in a real-world scenario.

*   **Task:** Run the `stress-client` with a simulated load of 300 characters performing diverse actions (Combat, Mining, Trading).
*   **Monitoring:** Use `OperationalTelemetry.snapshot()` to monitor the `tick.total` phase. 
*   **Alarm:** If `tick.total` exceeds 100ms on average under load, identify the bottleneck using the phase samples.

---

**Goal:** Zero main-thread blocking and linear scalability up to 1,000 players.
