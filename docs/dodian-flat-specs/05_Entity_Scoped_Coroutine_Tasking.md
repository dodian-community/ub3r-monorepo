# Spec 05: Entity-Scoped Coroutine Tasking
### Dodian-Flat Final Draft — Based on `game-server old` Actual Codebase Audit

---

> ⛔ **READ [00_MASTER_RULES_READ_FIRST.md](./00_MASTER_RULES_READ_FIRST.md) BEFORE THIS SPEC.**
> `GatheringTask`, `ProductionTask`, and `GameTask` are **infrastructure for existing skilling content**. Do not use these to create new skilling mechanics, new resources, or new XP rates. 
> Examples showing a mining or woodcutting loop are illustrating the pattern for migrating existing mining/woodcutting code. The XP values, tick delays, and item IDs in examples must match what the existing code already does.

---

## 1. Executive Summary

The coroutine tasking system in `game-server old` is **already implemented and functional**. It lives in `net.dodian.uber.game.engine.tasking` and is the backbone of the skilling loop system. Understanding the exact API that exists is critical before using it — this spec documents the real implementation and defines the developer-facing wrapper layer.

### 1.1 What Already Exists (Do Not Reimplement)

| Class | Package | Purpose |
|:---|:---|:---|
| `GameTask` | `engine.tasking` | Core coroutine task with `wait()`, `waitUntil()`, `waitUntilCycle()`, `repeatWhile()`, `terminate()` |
| `GameTaskRuntime` | `engine.tasking` | Factory for creating and enqueueing tasks via `queuePlayer()` and `queueWorld()` |
| `GameTaskSet` | `engine.tasking` | Per-entity set of active tasks |
| `TaskHandle` | `engine.tasking` | Reference to a running task, supports `cancel()` |
| `TaskPriority` | `engine.tasking` | Enum — `STANDARD`, `LOW`, `HIGH`, etc. |
| `TaskRequestKey<T>` | `engine.tasking` | Typed key for `waitReturnValue()` returns |
| `TaskCoroutineFacade` | `engine.tasking.coroutine` | Adapter between the coroutine and the engine task lifecycle |
| `PawnTaskSet` | `engine.tasking.set` | Entity-level task set (player/NPC) |
| `WorldTaskSet` | `engine.tasking.set` | World-level task set |
| `WaitCondition` | `engine.tasking.suspension` | Tick countdown condition |
| `PredicateCondition` | `engine.tasking.suspension` | Predicate-based suspend condition |
| `TaskStep` | `engine.tasking.suspension` | Pairs a condition with a continuation |
| `StopControlScope` | `engine.tasking` | Internal scope for `repeatWhile` cancellation |

---

## 2. The `GameTask` API (Real Signatures)

Every coroutine-based task is a `GameTask`. Content developers create tasks via `GameTaskRuntime`, not by constructing `GameTask` directly.

### 2.1 Suspension Methods

```kotlin
// Suspends for exactly `ticks` game ticks (1 tick = 600ms)
suspend fun GameTask.wait(ticks: Int)

// Suspends for a random number of ticks in the range [minTicks, maxTicks)
suspend fun GameTask.wait(minTicks: Int, maxTicks: Int)

// Suspends until the game cycle counter reaches or exceeds `targetCycle`
suspend fun GameTask.waitUntilCycle(targetCycle: Long)

// Suspends until `predicate` returns true (checked each tick)
suspend fun GameTask.waitUntil(predicate: () -> Boolean)

// Suspends until a return value is submitted for the given key
// Used for dialogue choices, input requests, etc.
suspend fun <T> GameTask.waitReturnValue(key: TaskRequestKey<T>): T

// Loop: repeatedly waits `delayTicks` between executions.
// Stops when `canRepeat` returns false or `scope.stop()` is called.
suspend fun GameTask.repeatWhile(
    delayTicks: Int,
    immediate: Boolean = false,
    canRepeat: suspend () -> Boolean,
    logic: suspend StopControlScope.() -> Unit,
    onFinished: (suspend GameTask.() -> Unit)? = null,
)
```

### 2.2 Utility Methods

```kotlin
// Current game cycle — used for scheduling relative to a fixed point
fun GameTask.currentCycle(): Long

// Called when the task should be terminated early (e.g., player logs out)
fun GameTask.terminate()

// Register a callback to run when the task terminates
fun GameTask.onTerminate(block: GameTask.() -> Unit)
```

---

## 3. `GameTaskRuntime`: Creating Tasks

Tasks are enqueued via `GameTaskRuntime`. Two entry points exist:

```kotlin
// Queue a task scoped to a specific player (Client entity)
// The task is automatically cancelled if the player logs out
GameTaskRuntime.queuePlayer(
    client: Client,
    priority: TaskPriority = TaskPriority.STANDARD,
    block: suspend GameTask.() -> Unit
): TaskHandle

// Queue a world-scoped task (not tied to a player)
GameTaskRuntime.queueWorld(
    priority: TaskPriority = TaskPriority.STANDARD,
    block: suspend GameTask.() -> Unit
): TaskHandle
```

### 3.1 How the Game Thread Integration Works

`GameTask` implements `Continuation<Unit>`. When `wait(ticks)` is called, it stores a `TaskStep(WaitCondition(ticks), continuation)` and suspends. On each game tick, the `GameLoopService` calls `task.cycle()` which checks if `WaitCondition.resume()` is true (the tick counter hit zero), and if so resumes the continuation.

This means **all task resumptions happen on the game thread** — there is no cross-thread dispatch. Content developers writing `suspend fun` code inside a `GameTask` block are always on the game thread.

---

## 4. The GatheringTask Pattern (What Already Exists in `skills.core`)

The `GatheringTask` abstract class at `net.dodian.uber.game.content.skills.core.runtime.GatheringTask` is the backbone of all gathering skills (mining, woodcutting, fishing). Its actual implementation:

```kotlin
// net.dodian.uber.game.content.skills.core.runtime.GatheringTask (current actual)
abstract class GatheringTask(
    private val actionName: String,
    private val client: Client,
    private val delayCalculator: (Client) -> Int,
    private val requirements: List<Requirement>,
    private val priority: TaskPriority = TaskPriority.STANDARD,
) {
    fun start(beforeStart: () -> Unit = {}): Boolean
    fun cancel(reason: ActionStopReason = ActionStopReason.USER_INTERRUPT)
    protected fun stop(reason: ActionStopReason)
    protected fun succeedCycle()
    protected abstract fun onStart()
    protected abstract fun onTick(): Boolean  // return false to stop
    protected abstract fun onStop(reason: ActionStopReason)
}
```

This class posts `SkillingActionStartedEvent`, `SkillingActionCycleEvent`, `SkillingActionSucceededEvent`, and `SkillingActionStoppedEvent` to the `GameEventBus` at the appropriate lifecycle points.

**Current Location:** `content.skills.core.runtime` — **must move to `systems.skills`** (it is engine infrastructure, not domain content).

---

## 5. Target Package After Migration

After Spec 01 migrations, the tasking infrastructure resolves as:

```
systems.skills/
    GatheringTask.kt         — Abstract base for mining/woodcutting/fishing (moved from content.skills.core.runtime)
    ProductionTask.kt        — Abstract base for smithing/cooking/crafting (NEW — see Section 6)
    ProgressionService.kt    — XP math and level-up handling (NEW — see Spec 07)
    ActionStopReason.kt      — Enum (moved from content.skills.core.runtime)
    requirements/
        Requirement.kt       — Requirement interface (moved from content.skills.core.requirements)
        ValidationResult.kt  — Result sealed class (moved)
```

---

## 6. The Production Task Pattern (NEW)

Not all skills use the gathering loop. Smithing, cooking, fletching, herblore, and crafting follow a "production" pattern: player selects an item to make, a quantity, and clicks a button. The server then loops until quantity is reached or requirements fail.

```kotlin
// Target: net.dodian.uber.game.systems.skills.ProductionTask
abstract class ProductionTask(
    private val actionName: String,
    private val client: Client,
    private val quantity: Int,
    private val ticksPerItem: Int,
    private val requirements: List<Requirement>,
    private val priority: TaskPriority = TaskPriority.STANDARD,
) {
    private var produced = 0

    fun start(): Boolean {
        val validation = validateRequirements()
        if (validation is ValidationResult.Failed) {
            client.sendMessage(validation.message)
            return false
        }
        onStart()
        GameEventBus.post(SkillingActionStartedEvent(client, actionName))

        GameTaskRuntime.queuePlayer(client, priority) {
            while (produced < quantity) {
                if (client.disconnected || !client.isActive) return@queuePlayer
                val check = validateRequirements()
                if (check is ValidationResult.Failed) {
                    client.sendMessage(check.message)
                    break
                }
                onBeforeProduce()
                wait(ticksPerItem)
                if (!produce()) break
                produced++
                GameEventBus.post(SkillingActionSucceededEvent(client, actionName))
            }
            onFinish()
            GameEventBus.post(SkillingActionStoppedEvent(client, actionName, ActionStopReason.COMPLETED))
        }
        return true
    }

    protected abstract fun onStart()
    protected abstract fun onBeforeProduce() // play animation
    protected abstract fun produce(): Boolean // perform item exchange, return false to stop
    protected abstract fun onFinish()

    private fun validateRequirements(): ValidationResult { /* same pattern as GatheringTask */ }
}
```

---

## 7. Task Interruption Policy

Tasks must be cancelled when the player:
1. Walks away from the resource (movement detected)
2. Opens a conflicting interface (e.g., opens bank while mining)
3. Enters combat
4. Logs out (automatic via `PawnTaskSet` lifecycle)

### 7.1 Existing Cancellation System

`PlayerActionCancellationService` at `systems.action.PlayerActionCancellationService` (actual class) handles cancellation. It is called from `InteractionProcessor` and other places. It posts a `PlayerActionCancelReason` and cancels the active `GameTask`.

Content code should never call `GameTaskRuntime` cancellation directly. Instead, the content sets up its task to cancel cleanly via conditions:

```kotlin
// Inside a GatheringTask.onTick():
override fun onTick(): Boolean {
    if (!client.isActive || client.disconnected) {
        stop(ActionStopReason.DISCONNECTED)
        return false
    }
    // The task runtime also handles this — belt and suspenders
    if (!client.isStanding()) {
        stop(ActionStopReason.USER_INTERRUPT)
        return false
    }
    // ... normal tick logic
    return true
}
```

### 7.2 `waitUntil` for Multi-Stage Sequences

For sequences that must pause until external state changes (e.g., waiting for a dialogue result in a skill sequence):

```kotlin
GameTaskRuntime.queuePlayer(client) {
    // Present a menu asking how many items
    val inputKey = TaskRequestKey<Int>("smithing_quantity")
    client.openSmithingQuantityInterface()

    // Suspend until the player submits a count via the UI
    val quantity = waitReturnValue(inputKey)

    // Now execute the production
    SmithingTask(client, quantity, ...).start()
}
```

The UI handler (button click listener) calls `task.submitReturnValue(inputKey, chosenQuantity)` to resume the task.

---

## 8. World Tasks — Global Events

Tasks not tied to a player use `GameTaskRuntime.queueWorld()`. These are appropriate for:

- Global timer events (e.g., wilderness drop events every 30 minutes)
- NPC respawn scheduling
- Farming tick processing (currently in `systems.world.farming.FarmingScheduler`)

```kotlin
// Example: broadcast chest drop event every 30 minutes
GameTaskRuntime.queueWorld {
    while (true) {
        wait(3000) // 3000 ticks = 30 minutes at 600ms/tick
        GameEventBus.post(WorldChestDropEvent())
        Server.broadcastMessage("A treasure chest has appeared in Edgeville!")
    }
}
```

---

## 9. NPC AI Tasks

NPCs can have tasks attached. The `PawnTaskSet` supports both `Client` and `Npc` as entity contexts. NPC boss mechanics use this:

```kotlin
// Conceptual NPC AI (not yet implemented — this is the target pattern)
GameTaskRuntime.queueNpc(npc) {
    while (npc.isAlive) {
        wait(5) // special attack every 5 ticks
        if (npc.hp < npc.maxHp / 2) {
            // Phase 2 attack
            npc.animate(NPC_PHASE2_ANIM)
            npc.broadcastNearby("Foolish mortals!")
            wait(2)
            npc.attackNearbyPlayers(range = 3, maxHit = 25)
        } else {
            npc.animate(NPC_STANDARD_ATTACK_ANIM)
            wait(1)
            npc.attackTarget()
        }
    }
}
```

> Implementation Note: `GameTaskRuntime.queueNpc()` may not exist yet in the codebase. Check `GameTaskRuntime.kt` for the actual methods. The `WorldTaskSet.kt` is the current world-level set. The `PawnTaskSet.kt` is the entity-level set.

---

## 10. Coroutine Tasks and Pathfinding

Coroutine tasks must NOT call pathfinding or walk-queue methods directly. If a task needs to wait for a player to arrive somewhere:

```kotlin
// WRONG — do not call walk queue directly from a task
suspend fun GameTask.walkTo(x: Int, y: Int) {
    client.walkingQueue.add(...)  // ❌ Do not do this
}

// CORRECT — set a pending intent, wait for system to resolve it
suspend fun GameTask.waitForArrival(position: Position) {
    waitUntil { client.distanceTo(position) <= 1 }
}
```

The pathfinding system replacement will provide a proper API. Until then, tasks that need movement must either use `teleportTo()` for scripted events or `waitUntil` for proximity checks.

---

## 11. Memory Safety Rules for Tasks

### 11.1 Avoid Capturing Mutable References

The coroutine captures closures at task creation time. Captured `Client` or `Npc` references become stale if the entity id changes. Safe pattern:

```kotlin
// SAFE: use the task's ctx reference
GameTaskRuntime.queuePlayer(client) {
    val snapshot = client.position.copy()  // snapshot at start
    wait(5)
    // client.position may have changed — that is fine and expected
    val current = client.position  // re-read, do not use snapshot unless intentional
}
```

### 11.2 Task Cancellation on Logout

All `PawnTaskSet` tasks are automatically cancelled when the player's `clear()` is called. The `GameTask.terminate()` method is invoked, which runs the `onTerminate` block. Content developers should register cleanup logic here:

```kotlin
val handle = GameTaskRuntime.queuePlayer(client) {
    // ... long task
}
handle.task.onTerminate {
    // cleanup: restore animation, remove overlay, etc.
    client.stopAnimation()
}
```

---

## 12. Tick Rate Reference

| Duration | Ticks | Formula |
|:---|:---|:---|
| 1 game tick | 1 | 600ms per tick |
| 3 seconds | 5 | 3000ms / 600ms |
| 10 seconds | 17 | 10000ms / 600ms, rounded |
| 1 minute | 100 | 60000ms / 600ms |
| 5 minutes | 500 | |
| 30 minutes | 3000 | |
| 1 hour | 6000 | |

---

## 13. Definition of Done for Spec 05

- [ ] `GatheringTask` moved from `content.skills.core.runtime` to `systems.skills`
- [ ] `ActionStopReason` moved from `content.skills.core.runtime` to `systems.skills`
- [ ] `Requirement` and `ValidationResult` moved from `content.skills.core.requirements` to `systems.skills.requirements`
- [ ] `ProductionTask` abstract class created in `systems.skills`
- [ ] `GameTask.wait()`, `waitUntil()`, `waitUntilCycle()`, `repeatWhile()`, `waitReturnValue()` are documented and confirmed working
- [ ] `GameTaskRuntime.queuePlayer()` and `queueWorld()` are the only entry points for task creation in content
- [ ] Content code never calls `GameTaskRuntime.queuePlayer()` directly — it goes through the skill task abstractions (`GatheringTask.start()`, `ProductionTask.start()`)
- [ ] `PawnTaskSet` and `WorldTaskSet` lifecycle (cancel on entity removal) confirmed working
- [ ] No coroutine task captures a direct reference to walk-queue internals or pathfinding state
- [ ] Tick rate reference table published in this spec for developer use
- [ ] `onTerminate` hook is used for cleanup in any task that modifies player state (animations, overlays)
