# Spec 09: Interaction Geometry and Decoupling
### Dodian-Flat Final Draft — Based on `game-server old` Actual Codebase Audit

---

> ⛔ **READ [00_MASTER_RULES_READ_FIRST.md](./00_MASTER_RULES_READ_FIRST.md) BEFORE THIS SPEC.**
> New `InteractionIntent` subclasses and new `process*()` methods in `InteractionProcessor` are **routing plumbing** for existing interaction types that currently have no bus dispatch. They do not add new game mechanics.
> Adding `ItemOnNpcIntent` so that an existing item-on-NPC interaction can be wired to the event bus is allowed. Adding a new item effect to that interaction is NOT allowed.

---

## 1. Executive Summary

This spec defines the boundary between the interaction engine (range validation, movement intent, object detection) and content scripts. The key insight is that **content scripts must never perform distance checks or pathfinding calls directly** — that is the systems layer's job. Content code should receive an event only after the engine has confirmed the player is in range and the target is valid.

In `game-server old`, this boundary is enforced by `InteractionProcessor`. This processor is well-implemented and handles most interaction types. This spec documents how it works, what content developers need to know, and what must change as new interaction patterns are added.

---

## 2. The `InteractionProcessor` — How It Actually Works

`InteractionProcessor` at `net.dodian.uber.game.systems.interaction.InteractionProcessor` is a game-thread singleton that processes each player's `pendingInteraction` once per tick. The actual code has been reviewed — here is the precise flow:

### 2.1 The Processing Loop (Per Player, Per Tick)

```
InteractionProcessor.process(player)
    │
    ├─ Guard checks:
    │   • player.pendingInteraction == null → return CANCELLED
    │   • player.didTeleport() → clear, return CANCELLED
    │   • player.disconnected || !isActive || !validClient → clear, return CANCELLED
    │   • PlayerRegistry.cycle < player.interactionEarliestCycle → return WAITING
    │
    ├─ Dispatch by intent type:
    │   • NpcInteractionIntent → processNpcInteraction()
    │   • ObjectClickIntent → processObjectClick()
    │   • ItemOnObjectIntent → processItemOnObject()
    │   • MagicOnObjectIntent → processMagicOnObject()
    │   • else → clear, return CANCELLED
    │
    └─ Each sub-processor:
        1. State guards (disconnected, randomed, UsingAgility)
        2. Target validation (NPC still alive, object still present)
        3. Range check (goodDistanceEntity or ObjectInteractionDistance)
        4. Policy lookup (ObjectContentRegistry.resolvePolicy)
        5. Settle gate (requireMovementSettled, settleTicks)
        6. Dispatch to content handler
        7. Timing metrics (slowLogIfNeeded)
        8. clear(player) — always called after handling
```

### 2.2 Return Values

`InteractionExecutionResult` is an enum with 3 values:
- `WAITING` — player is not yet in range; keep the intent for the next tick
- `COMPLETE` — interaction was handled successfully
- `CANCELLED` — interaction was invalidated; intent is cleared

Content scripts never see `WAITING` or `CANCELLED`. They only execute when the result is `COMPLETE`.

---

## 3. The `InteractionIntent` Interface

```kotlin
// net.dodian.uber.game.systems.interaction.InteractionIntent (actual)
interface InteractionIntent {
    val opcode: Int
    val createdCycle: Long
}
```

Concrete implementations (these are real classes in the codebase):

| Class | Fields | Trigger |
|:---|:---|:---|
| `NpcInteractionIntent` | `npcIndex`, `option`, `opcode`, `createdCycle` | NPC click packets |
| `ObjectClickIntent` | `objectId`, `option`, `objectPosition`, `objectData?`, `objectDef?`, `opcode`, `createdCycle` | Object click packets |
| `ItemOnObjectIntent` | `objectId`, `objectPosition`, `objectData?`, `objectDef?`, `itemId`, `itemSlot`, `interfaceId`, `opcode`, `createdCycle` | Item-on-object packet |
| `MagicOnObjectIntent` | `objectId`, `objectPosition`, `objectData?`, `objectDef?`, `spellId`, `opcode`, `createdCycle` | Magic-on-object packet |

**Missing intents that must be added to expand coverage:**

| Class | Fields | Trigger |
|:---|:---|:---|
| `ItemOnNpcIntent` | `npcIndex`, `itemId`, `itemSlot`, `interfaceId`, `opcode`, `createdCycle` | Item-on-NPC packet |
| `MagicOnNpcIntent` | `npcIndex`, `spellId`, `opcode`, `createdCycle` | Magic-on-NPC packet |
| `MagicOnPlayerIntent` | `targetPlayerIndex`, `spellId`, `opcode`, `createdCycle` | Magic-on-player packet |
| `PlayerAttackIntent` | `targetPlayerIndex`, `opcode`, `createdCycle` | Player attack packet |

---

## 4. Range Validation — How It Works Today

### 4.1 NPC Range Check

```kotlin
// From actual InteractionProcessor code:
val range = if (intent.option == NPC_ATTACK_OPTION && player.getAttackStyle() != 0) {
    5  // ranged/magic: 5 tile range
} else {
    1  // melee / dialogue: 1 tile range
}
if (!player.goodDistanceEntity(npc, range)) {
    return InteractionExecutionResult.WAITING
}
```

`player.goodDistanceEntity(entity, range)` is a Java method on `Client`. This internally uses the legacy pathfinder to walk the player toward the target if out of range.

### 4.2 Object Range Check

For objects, `ObjectInteractionDistance.resolveDistancePosition()` returns `null` if the player is out of range. Unlike NPC checks, object distance uses the object's boundary geometry (its width/height from the cache data) rather than simple tile distance.

### 4.3 Policy-Based Object Distance Modes

`ObjectInteractionPolicy` (Java class, used by `InteractionProcessor`) defines a `distanceRule`:

| Distance Rule | Meaning |
|:---|:---|
| `LEGACY_OBJECT_DISTANCE` | Standard 317 object interaction distance — varies by interaction type (click vs item vs magic) |
| `NEAREST_BOUNDARY_CARDINAL` | Must reach a cardinal boundary tile of the object (North/South/East/West) |
| `NEAREST_BOUNDARY_ANY` | Reach any adjacent tile of the object boundary |

Content scripts that register via `ObjectContentRegistry` can specify their `distanceRule` policy. Most content should use `LEGACY_OBJECT_DISTANCE` (the default).

---

## 5. The Settlement Gate

Some object interactions require the player to be fully settled (not walking) before they trigger. This is the "settle gate":

```kotlin
// From InteractionProcessor.isSettleGateSatisfied():
// If policy.requireMovementSettled == false → no wait needed
// If player moving → not settled → WAITING
// If settleTicks > 0 → must wait additional settleTicks after settling
```

The settlement check uses raw Client fields:
```kotlin
private fun isMovementSettled(player: Client): Boolean {
    return player.primaryDirection == -1 &&
        player.secondaryDirection == -1 &&
        player.wQueueReadPtr == player.wQueueWritePtr
}
```

This is one of the few places where the legacy walk queue is referenced in Kotlin. Under the new contract:
- Content scripts must not replicate or extend this check
- The settle gate is always the `InteractionProcessor`'s responsibility
- If a new interaction type needs settlement, add it to `InteractionProcessor`, not to content

---

## 6. Content Script Contracts

Content scripts receive a fully resolved interaction via the event bus. They have the following guarantees when their handler is called:

### 6.1 Guaranteed When a Handler Fires
- Player is alive and connected (`!client.disconnected && client.isActive`)
- Player is in valid range of the target (for NPC: ≤1 tile; for object: within policy range)
- Target exists in the world (NPC is alive; object is present at the given position)
- Player is not in a blocked state (`!player.randomed`, `!player.UsingAgility` for most types)
- The interaction intent has not been superseded by a newer one

### 6.2 What Content Code Must NOT Assume
- The player's exact pixel/sub-tile position within the tile
- That the player is standing exactly adjacent to the target (range may be 1 or 5)
- That no other handler has already fired for this same event (multiple handlers can match)
- That the player has items, levels, or quest flags — these must be checked in the handler

### 6.3 What Content Code Must NOT Call
- `player.goodDistanceEntity(...)` — not needed; range already validated
- `player.pendingInteraction = ...` — never overwrite pending interaction from content
- `player.interactionEarliestCycle = ...` — never touch this field from content
- `Misc.getObject(...)` — use `ObjectInteractionContext` data provided in the event

---

## 7. Object Content Registration — The `ObjectContentRegistry`

`ObjectContentRegistry` at `net.dodian.uber.game.content.objects.ObjectContentRegistry` provides:
- Policy resolution: `resolvePolicy(objectId, position, interactionType, option, obj, ...)`
- Distance rule overrides per object/interaction combination

Content scripts that need non-default distance rules must register their object policy here. Most content scripts do NOT need to interact with this registry — they simply register `GameEventBus.on<ObjectClickEvent>` handlers.

Only scripts with unique geometry needs (e.g., multi-tile objects where the player must approach from a specific side) register custom policies.

---

## 8. The Pathfinding System — Freeze and Contract

The current pathfinding is implemented entirely in Java in `Client.java` via the walk queue:
- `walkingQueue[]` — array of tile positions to walk
- `wQueueReadPtr` / `wQueueWritePtr` — read/write pointers
- `primaryDirection` / `secondaryDirection` — current tick's movement directions

**This system is frozen.** It will be replaced in a future spec that has not yet been written. Until then:

### 8.1 Do Not Expand the Walk Queue API
- Do not add new methods to `Client.java` that manipulate the walk queue
- Do not write Kotlin extension functions that set walk queue fields directly
- Do not create a `Router` or `Pathfinder` class that calls into the walk queue

### 8.2 Do Not Wrap It Either
- Do not create a `PathfindingService` that delegates to the current `Client.java` walk queue
- The current pathfinding is already called by `UpstreamHandler` (walk packets) and `goodDistanceEntity` — adding another call site risks double-walking

### 8.3 The Only Acceptable Movement in Content

```kotlin
// ALLOWED: Teleportation (bypasses pathfinding entirely)
player.teleportTo(Position(3096, 3494, 0))

// ALLOWED: Check if player is in range of a position
if (player.distanceTo(targetPosition) <= 1) { /* do thing */ }

// ALLOWED: Wait until player arrives near a target (inside a GameTask)
suspend fun GameTask.waitForArrival(client: Client, position: Position, range: Int = 1) {
    waitUntil { client.distanceTo(position) <= range }
}

// FORBIDDEN: Manipulating walk queue from content
player.walkingQueue[...] = ...  // Never do this
player.addWaypoint(x, y)         // If this method exists, do not call it from content
```

---

## 9. Adding New Interaction Types

When a new interaction type needs to be supported (e.g., `ItemOnNpc`):

1. **Create the `Intent` data class** (e.g., `ItemOnNpcIntent.kt`) in `systems.interaction`
2. **Wire it from the packet handler** — modify `UpstreamHandler.java` to set `player.pendingInteraction = ItemOnNpcIntent(...)` when opcode 57 arrives
3. **Add a `when` branch to `InteractionProcessor.process()`** that calls a new `processItemOnNpc()` method
4. **Implement `processItemOnNpc()`** following the same guard/range/dispatch pattern as existing methods
5. **Create the event class** `ItemOnNpcEvent` (see Spec 02) and post it from the new processor method
6. **Register content handlers** via `GameEventBus.on<ItemOnNpcEvent>` in content scripts

This pattern ensures all new interaction types have proper range validation before content code fires.

---

## 10. The `ActiveInteraction` Record

When an interaction is dispatched, `InteractionProcessor` sets:
```kotlin
player.activeInteraction = ActiveInteraction(intent, player.lastProcessedCycle)
```

This record can be read by content code to know what interaction is currently active. After the handler returns, `activeInteraction` is cleared by `InteractionProcessor.clear(player)`. Content code should not manually set or clear `activeInteraction`.

---

## 11. Interaction Timing Metrics

`InteractionProcessor` already logs slow interactions using `runtimePhaseWarnMs` from `config`:
```kotlin
if (elapsedMs >= runtimePhaseWarnMs) {
    logger.warn("Slow interaction: type={} player={} target={} option={} total={}ms ...", ...)
}
```

Content developers can review these logs to identify scripts that are taking too long to execute. Any handler that takes > `runtimePhaseWarnMs` (default: check `config.kt` for actual value, typically 5-10ms) will be logged. Content scripts should aim for < 1ms execution time. Database calls from content are forbidden for this reason.

---

## 12. Definition of Done for Spec 09

- [ ] `InteractionProcessor` documented accurately for content developers — they understand what is guaranteed when their handler fires
- [ ] `ItemOnNpcIntent`, `MagicOnNpcIntent`, `MagicOnPlayerIntent`, `PlayerAttackIntent` classes created in `systems.interaction`
- [ ] `processItemOnNpc()` and `processMagicOnNpc()` methods added to `InteractionProcessor`
- [ ] `ItemOnNpcEvent` and `MagicOnNpcEvent` are fired from these new processor methods (see Spec 02)
- [ ] `PacketOpcodes.ITEM_ON_NPC` and `PacketOpcodes.MAGIC_ON_NPC` wired in `UpstreamHandler.java` to set `pendingInteraction`
- [ ] Content script contracts (Section 6) documented and shared with the team
- [ ] Pathfinding freeze policy (Section 8) communicated clearly — specific code patterns prohibited
- [ ] `ObjectInteractionDistance` and settle-gate logic are engine-internal only, not referenced from any `content.*` file
- [ ] `runtimePhaseWarnMs` threshold is configured in `engine.config` (after Spec 01 config relocation)
- [ ] `InteractionExecutionResult.WAITING` behavior documented — content devs understand why their handler might not fire immediately
- [ ] No walk-queue field access from any `content.*` Kotlin file
