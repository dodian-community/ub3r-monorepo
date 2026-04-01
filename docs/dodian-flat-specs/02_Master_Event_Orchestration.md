# Spec 02: Master Event Orchestration (The Bus and All Signals)
### Dodian-Flat Final Draft — Based on `game-server old` Actual Codebase Audit

---

> ⛔ **READ [00_MASTER_RULES_READ_FIRST.md](./00_MASTER_RULES_READ_FIRST.md) BEFORE THIS SPEC.**
> The event signals and DSL patterns shown here are **infrastructure plumbing** for existing content, not new content itself.
> Creating a new event signal class (`ItemClickEvent` etc.) is allowed. Registering a new handler for an NPC that does something new is NOT allowed.
> Every `GameEventBus.on<>` handler you write must reproduce existing behavior, not invent new behavior.

---

## 1. Executive Summary

The event system in `game-server old` is **already functional and thread-safe**. The `GameEventBus` at `net.dodian.uber.game.event.GameEventBus` uses `ConcurrentHashMap<Class<out GameEvent>, CopyOnWriteArrayList<EventListener<out GameEvent>>>` for its listener registry and `CopyOnWriteArrayList` for filter chains. The bus supports:
- Standard fire-and-forget dispatch via `post(event)`
- Boolean-handled queries via `postWithResult(event)`
- Return-value collectors via `postAndReturn<T>(event)`
- Typed filters via `addFilter<E> { predicate }`
- Inline reified registration via `on<E> { ... }`

This spec defines:
1. The precise relocation plan for the bus machinery (Spec 01 references this)
2. The full catalog of signals (event data classes) that must exist and where they go
3. The `on<T>` DSL pattern content developers will use
4. How the bus integrates with the existing Netty packet listeners in Java
5. Thread safety guarantees — all game events are dispatched on the game thread
6. What must NOT be changed (pathfinding-related signals)

---

## 2. The Existing Bus: What It Does and Does Not Have

### 2.1 What Exists Today

```kotlin
// net.dodian.uber.game.event.GameEventBus (actual current implementation)
object GameEventBus {
    private val listeners = ConcurrentHashMap<Class<out GameEvent>, CopyOnWriteArrayList<EventListener<out GameEvent>>>()
    private val returnableListeners = ConcurrentHashMap<..., CopyOnWriteArrayList<ReturnableEventListener<...>>>()
    private val filters = ConcurrentHashMap<Class<out GameEvent>, CopyOnWriteArrayList<EventFilter<out GameEvent>>>()

    fun bootstrap()         // initializes CoreEventBusBootstrap
    fun <E> post(event: E)  // fire-and-forget dispatch
    fun <E> postWithResult(event: E): Boolean  // returns true if any handler returned true
    fun <E, T> postAndReturn(event: E): List<T> // collects return values from ReturnableEventListeners
    inline fun <reified E> on(...) // reified DSL registration
    fun <E> on(clazz, listener)   // Java-compatible registration
    inline fun <reified E, T> onReturnable(...) // return-value DSL
    inline fun <reified E> addFilter(...)        // filter DSL
    fun clear()             // used in tests
}
```

### 2.2 What Is Missing

The following signals **do not currently exist** and must be created as data-only `data class` files in `net.dodian.uber.game.events`:

| Missing Signal | Trigger Point | Required For |
|:---|:---|:---|
| `ItemClickEvent` | Opcodes 122 (opt1), 41 (opt2), 16 (opt3), 75 (opt4) | Item interaction routing |
| `ItemOnItemEvent` | Opcode 53 | Combination system routing |
| `ItemOnNpcEvent` | Opcode 57 | NPC interaction routing |
| `ItemOnObjectEvent` | Opcode 192 | Object interaction routing |
| `MagicOnNpcEvent` | Opcode 131 | Magic routing |
| `MagicOnPlayerEvent` | Opcode 249 | PvP routing |
| `MagicOnObjectEvent` | Opcode 35 | Object magic routing |
| `PlayerLoginEvent` | Login flow — after player state is loaded | Login hook for existing systems |
| `PlayerLogoutEvent` | Logout — before final player save | Logout hook for existing systems |
| `LevelUpEvent` | XP gain → level threshold crossed | Level-up interface (existing) |
| `DeathEvent` | `Client.onDeath()` equivalent | Death handler (existing) |
| `DamageEvent` | Combat damage application | Combat metrics (existing) |

### 2.3 What Already Exists (Do Not Recreate)

| Signal Class | Current Package |
|:---|:---|
| `NpcClickEvent` | `event.events` — move to `events` flat package |
| `ObjectClickEvent` | `event.events` — move to `events` flat package |
| `ButtonClickEvent` | `event.events` — move to `events` flat package |
| `CommandEvent` | `event.events` — move to `events` flat package |
| `DialogueContinueEvent` | `event.events` — move to `events` flat package |
| `DialogueOptionEvent` | `event.events` — move to `events` flat package |
| `PlayerTickEvent` | `event.events` — move to `events` flat package |
| `WorldTickEvent` | `event.events` — move to `events` flat package |
| `SkillingActionStartedEvent` | `event.events.skilling` — move to `events.skilling` |
| `SkillingActionCycleEvent` | `event.events.skilling` — move to `events.skilling` |
| `SkillingActionSucceededEvent` | `event.events.skilling` — move to `events.skilling` |
| `SkillingActionStoppedEvent` | `event.events.skilling` — move to `events.skilling` |

---

## 3. The Complete Signal Catalog

### 3.1 Player Interaction Signals

Each of these must be `data class` in `net.dodian.uber.game.events`. They carry only data — no methods, no business logic.

```kotlin
// net.dodian.uber.game.events.NpcClickEvent (already exists, move only)
data class NpcClickEvent(
    val client: Client,
    val option: Int,
    val npc: Npc,
) : GameEvent

// net.dodian.uber.game.events.ObjectClickEvent (already exists, move only)
data class ObjectClickEvent(
    val client: Client,
    val option: Int,
    val objectId: Int,
    val position: Position,
) : GameEvent

// net.dodian.uber.game.events.ButtonClickEvent (already exists, move only)
data class ButtonClickEvent(
    val client: Client,
    val buttonId: Int,
) : GameEvent

// net.dodian.uber.game.events.ItemClickEvent (NEW — must create)
data class ItemClickEvent(
    val client: Client,
    val option: Int,
    val itemId: Int,
    val itemSlot: Int,
    val interfaceId: Int,
) : GameEvent

// net.dodian.uber.game.events.ItemOnItemEvent (NEW — must create)
data class ItemOnItemEvent(
    val client: Client,
    val usedItemId: Int,
    val usedItemSlot: Int,
    val targetItemId: Int,
    val targetItemSlot: Int,
    val usedInterfaceId: Int,
    val targetInterfaceId: Int,
) : GameEvent

// net.dodian.uber.game.events.ItemOnObjectEvent (NEW — must create)
data class ItemOnObjectEvent(
    val client: Client,
    val itemId: Int,
    val itemSlot: Int,
    val interfaceId: Int,
    val objectId: Int,
    val position: Position,
) : GameEvent

// net.dodian.uber.game.events.ItemOnNpcEvent (NEW — must create)
data class ItemOnNpcEvent(
    val client: Client,
    val itemId: Int,
    val itemSlot: Int,
    val interfaceId: Int,
    val npc: Npc,
) : GameEvent

// net.dodian.uber.game.events.MagicOnNpcEvent (NEW — must create)
data class MagicOnNpcEvent(
    val client: Client,
    val spellId: Int,
    val npc: Npc,
) : GameEvent

// net.dodian.uber.game.events.MagicOnPlayerEvent (NEW — must create)
data class MagicOnPlayerEvent(
    val client: Client,
    val spellId: Int,
    val targetIndex: Int,
) : GameEvent

// net.dodian.uber.game.events.CommandEvent (already exists, move only)
data class CommandEvent(
    val client: Client,
    val command: String,
    val args: Array<String>,
) : GameEvent
```

### 3.2 Lifecycle Signals

```kotlin
// net.dodian.uber.game.events.PlayerLoginEvent (NEW — must create)
data class PlayerLoginEvent(
    val client: Client,
) : GameEvent

// net.dodian.uber.game.events.PlayerLogoutEvent (NEW — must create)
data class PlayerLogoutEvent(
    val client: Client,
    val isForced: Boolean,
) : GameEvent

// net.dodian.uber.game.events.PlayerTickEvent (already exists, move)
data class PlayerTickEvent(
    val client: Client,
) : GameEvent

// net.dodian.uber.game.events.WorldTickEvent (already exists, move)
object WorldTickEvent : GameEvent

// net.dodian.uber.game.events.LevelUpEvent (NEW — must create)
data class LevelUpEvent(
    val client: Client,
    val skillId: Int,
    val newLevel: Int,
) : GameEvent

// net.dodian.uber.game.events.DeathEvent (NEW — must create)
data class DeathEvent(
    val client: Client,
) : GameEvent
```

### 3.3 Skilling Signals (Existing — Move to Flat Package)

The 4 existing skilling event classes in `event.events.skilling.SkillingEvents.kt` are:
- `SkillingActionStartedEvent(client, actionName)`
- `SkillingActionCycleEvent(client, actionName)`
- `SkillingActionSucceededEvent(client, actionName)`
- `SkillingActionStoppedEvent(client, actionName, reason)`

These must be moved to `net.dodian.uber.game.events.skilling.SkillingEvents.kt`. The `actionName: String` field is how content distinguishes which skill fired (e.g., `"Mining"`, `"Woodcutting"`). This approach works and must not be changed.

---

## 4. The `on<T>` DSL for Content Developers

Content scripts register listeners using the bus DSL. The existing `GameEventBus.on<E>` inline function supports this. Here is the pattern content developers use:

### 4.1 Standard NPC Script Registration

```kotlin
// net.dodian.uber.game.content.npcs.Hans.kt
package net.dodian.uber.game.content.npcs

import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.NpcClickEvent
import net.dodian.uber.game.systems.ui.dialogue.DialogueService
import net.dodian.uber.game.systems.ui.dialogue.DialogueFactory

// Registration happens in the NpcContentDispatcher.bootstrap() call
// which is invoked from CoreEventBusBootstrap
fun register() {
    GameEventBus.on<NpcClickEvent>(
        condition = { it.npc.id == NpcIds.HANS && it.option == 1 }
    ) { event ->
        DialogueService.start(event.client) {
            npcChat(npcId = NpcIds.HANS, text = "Hello there. I've been here longer than you.")
        }
        true
    }
}
```

### 4.2 Button Click Registration

```kotlin
GameEventBus.on<ButtonClickEvent>(
    condition = { it.buttonId == InterfaceIds.BANK_DEPOSIT_ALL }
) { event ->
    // deposit logic via systems API
    true
}
```

### 4.3 Skilling Event Observation

```kotlin
GameEventBus.on<SkillingActionSucceededEvent>(
    condition = { it.actionName == "Mining" }
) { event ->
    // awards bonus XP on a per-event basis
    false // return false to allow other handlers to run
}
```

---

## 5. Thread Safety Invariants

The `GameEventBus` is designed to be called from the **main game thread only**. The following rules are non-negotiable:

1. **All `GameEventBus.post()` calls must occur on the game thread.** The game thread is the thread executing the `GameLoopService` tick cycle.
2. Netty worker threads (which decode inbound packets) do NOT call `post()` directly. They push `InboundPacket` objects to a queue. The queue is drained at the start of each game tick on the game thread, where `post()` is then called.
3. `ConcurrentHashMap` and `CopyOnWriteArrayList` are used in the bus internals only as a safety net for the listener registration operations (which can happen on startup from multiple threads). The dispatch itself (`processListeners`) does not require concurrency protection because it is single-threaded.
4. Database operations (player saves) are always dispatched to `DbDispatchers.IO` (a coroutine dispatcher backed by an IO thread pool) and never run on the game thread.

---

## 6. The Packet-to-Signal Pipeline (Actual Flow)

```
[Netty Worker Thread]
    Client sends opcode 72 (NPC Click Option 1)
    → UpstreamHandler.channelRead()
    → PacketHandler.handlePacket() [Java]
    → Sets player.pendingInteraction = NpcInteractionIntent(...)

[Game Thread — InteractionProcessor, called from processing phase]
    → InteractionProcessor.process(player)
    → Validates range (goodDistanceEntity)
    → Calls NpcContentDispatcher.tryHandleClickTimed(player, option, npc)
    → NpcContentDispatcher posts NpcClickEvent to GameEventBus
    → EventBus dispatches to all registered handlers with matching condition
    → Handler (e.g., Hans.kt) executes dialogue
```

This pipeline means the `NpcClickEvent` is fired only once the player is confirmed in range and the interaction is validated. Content scripts do not need to check distance or player state — the systems layer has already done it.

### 6.1 Opcodes That Must Trigger Signals

The following opcodes are received by the Java `UpstreamHandler` / packet decoder and must eventually be translated into the game event signals. The column `Signal` shows the target event class.

| Opcode | Direction | Signal | Status |
|:---|:---|:---|:---|
| 72 | Client→Server | `NpcClickEvent(option=1)` | Existing partial — via `NpcInteractionIntent` |
| 17 | Client→Server | `NpcClickEvent(option=2)` | Existing partial |
| 21 | Client→Server | `NpcClickEvent(option=3)` | Existing partial |
| 18 | Client→Server | `NpcClickEvent(option=4)` | Existing partial |
| 155 | Client→Server | `NpcClickEvent(option=1)` alternate | Existing partial |
| 132 | Client→Server | `ObjectClickEvent(option=1)` | Existing partial |
| 252 | Client→Server | `ObjectClickEvent(option=2)` | Existing partial |
| 70 | Client→Server | `ObjectClickEvent(option=3)` | Existing partial |
| 234 | Client→Server | `ObjectClickEvent(option=4)` | Existing partial |
| 228 | Client→Server | `ObjectClickEvent(option=5)` | Existing partial |
| 185 | Client→Server | `ButtonClickEvent` | Existing partial — via `InterfaceButtonService` |
| 122 | Client→Server | `ItemClickEvent(option=1)` | **Missing — must create** |
| 41 | Client→Server | `ItemClickEvent(option=2)` | **Missing — must create** |
| 16 | Client→Server | `ItemClickEvent(option=3)` | **Missing — must create** |
| 75 | Client→Server | `ItemClickEvent(option=4)` | **Missing — must create** |
| 103 | Client→Server | `CommandEvent` | Existing partial |
| 40 | Client→Server | `DialogueContinueEvent` | Existing |
| 53 | Client→Server | `ItemOnItemEvent` | **Missing — must create** |
| 57 | Client→Server | `ItemOnNpcEvent` | **Missing — must create** |
| 192 | Client→Server | `ItemOnObjectEvent` | **Missing — must create** |
| 131 | Client→Server | `MagicOnNpcEvent` | **Missing — must create** |
| 249 | Client→Server | `MagicOnPlayerEvent` | **Missing — must create** |
| 35 | Client→Server | `MagicOnObjectEvent` | **Missing — must create** |
| 208 | Client→Server | `StringInputEvent` | Via `ContentActions` / input handler |
| 60 | Client→Server | `IntegerInputEvent` | Via `ContentActions` / input handler |

---

## 7. The `CoreEventBusBootstrap`: How Registration Works

`CoreEventBusBootstrap.bootstrap()` is called from `GameEventBus.bootstrap()`, which is invoked once on server startup. This is where all event listener registrations occur. Each content area registers its listeners here.

### 7.1 Current Bootstrap Pattern

```kotlin
// net.dodian.uber.game.event.bootstrap.CoreEventBusBootstrap (actual)
object CoreEventBusBootstrap {
    fun bootstrap() {
        // Currently wires up various service listeners
        // e.g., ButtonClickLoggingService, command dispatcher, dialogue integration
    }
}
```

### 7.2 Target Bootstrap Pattern (Auto-Registration via KSP)

When KSP annotation processing is available, NPC scripts tagged with `@NpcScript` would be wired automatically. Until that is implemented, bootstrap wiring is done explicitly in the bootstrap file.

For content developers, the expected pattern is that each major content area (NPCs, Objects, Items, Commands) has its own `register()` method called from a centralized bootstrap file. Example:

```kotlin
// Target: CoreEventBusBootstrap.kt
object CoreEventBusBootstrap {
    fun bootstrap() {
        NpcContentRegistry.registerAll()
        ObjectContentRegistry.registerAll()
        ButtonContentRegistry.registerAll()
        CommandContentRegistry.registerAll()
        ItemContentRegistry.registerAll()
    }
}
```

Each `registerAll()` method calls `GameEventBus.on<>()` for every script in that category.

---

## 8. Event Filtering — Correct Usage Pattern

The `addFilter<E>` function registers a predicate that runs before any listener for event class `E`. If the predicate returns `false`, the entire listener chain is skipped.

### 8.1 Anti-Cheat Filter Example (Correct)

```kotlin
// In SystemsBootstrap or SecurityBootstrap
GameEventBus.addFilter<NpcClickEvent> { event ->
    // Block interactions if player is in a restricted state
    !event.client.randomed && !event.client.disconnected && event.client.isActive
}
```

### 8.2 Content-Level Filtering (Use Condition Lambda, Not addFilter)

Content listeners should use the `condition` parameter, not `addFilter`. `addFilter` applies globally to all listeners for the event class and should only be used by security/anti-cheat code in the systems layer.

```kotlin
// CORRECT: content-level conditional registration
GameEventBus.on<NpcClickEvent>(
    condition = { it.npc.id == 591 && it.option == 1 }
) { event ->
    // handler body
    true
}

// WRONG: using addFilter in content code
GameEventBus.addFilter<NpcClickEvent> { it.npc.id == 591 } // This filters ALL NPC click handlers!
```

---

## 9. Pathfinding Signal Policy

Movement-related signals are explicitly excluded from expansion in this phase. The existing `MovementMessage` / walk queue handling in `Client.java` feeds `player.pendingInteraction`. No `MovementRequestEvent` should be created or wired until the new pathfinding system spec is published.

The `InteractionProcessor` already handles all movement-to-interaction bridging. Content scripts receive events only after the engine confirms the player is in range.

---

## 10. Signal Traceability Matrix (Complete Reference)

### 10.1 Interaction Signals

| Signal Class | Source Opcode | Dispatch Point | Content Recipient |
|:---|:---|:---|:---|
| `NpcClickEvent` | 72, 17, 21, 18, 155 | `InteractionProcessor → NpcContentDispatcher` | `content.npcs.*` |
| `ObjectClickEvent` | 132, 252, 70, 234, 228 | `InteractionProcessor → ObjectInteractionService` | `content.objects.*` |
| `ButtonClickEvent` | 185 | `InterfaceButtonService` | `content.ui.*` |
| `ItemClickEvent` | 122, 41, 16, 75 | **Must be added to packet handler** | `content.items.*` |
| `CommandEvent` | 103 | Command dispatcher | `content.commands.*` |
| `DialogueContinueEvent` | 40 | `DialogueService.onContinue()` | `systems.ui.dialogue` |
| `ItemOnItemEvent` | 53 | **Must be added** | `content.items.*` |
| `ItemOnObjectEvent` | 192 | `InteractionProcessor → ObjectInteractionService` | `content.objects.*` |
| `ItemOnNpcEvent` | 57 | **Must be added to InteractionProcessor** | `content.npcs.*` |
| `MagicOnNpcEvent` | 131 | **Must be added** | `content.npcs.*` |
| `MagicOnPlayerEvent` | 249 | **Must be added** | `content.minigames.*` |
| `MagicOnObjectEvent` | 35 | **Must be added** | `content.objects.*` |

### 10.2 Lifecycle Signals

| Signal Class | Trigger Point | Currently Posted? |
|:---|:---|:---|
| `PlayerLoginEvent` | After player data loaded and world entry complete | **No — must add to login flow** |
| `PlayerLogoutEvent` | Before final player save on disconnect | **No — must add to logout flow** |
| `PlayerTickEvent` | Per-player processing phase in game loop | Exists, move package only |
| `WorldTickEvent` | Each 600ms game tick start | Exists, move package only |
| `LevelUpEvent` | When skills gain crosses a level threshold | **No — must add to XP system** |
| `DeathEvent` | When player health reaches 0 | **No — must add to death handler** |

### 10.3 Skilling Lifecycle Signals (Existing)

| Signal Class | When Posted | `actionName` Value |
|:---|:---|:---|
| `SkillingActionStartedEvent` | `GatheringTask.start()` called | e.g., `"Mining"`, `"Woodcutting"` |
| `SkillingActionCycleEvent` | Each successful gather cycle | Same |
| `SkillingActionSucceededEvent` | On successful item gain | Same |
| `SkillingActionStoppedEvent` | On task stop (any reason) | Same + `ActionStopReason` |

---

## 11. Definition of Done for Spec 02

- [ ] `GameEventBus` and all machinery moved to `net.dodian.uber.game.engine.event`
- [ ] All data-only event classes moved to `net.dodian.uber.game.events` (flat)
- [ ] `SkillingEvents.kt` moved to `net.dodian.uber.game.events.skilling`
- [ ] `ItemClickEvent`, `ItemOnItemEvent`, `ItemOnObjectEvent`, `ItemOnNpcEvent` created and wired to correct inbound opcodes
- [ ] `MagicOnNpcEvent`, `MagicOnPlayerEvent`, `MagicOnObjectEvent` created and wired
- [ ] `PlayerLoginEvent` fired from the login flow (after player state loaded)
- [ ] `PlayerLogoutEvent` fired before player save on disconnect
- [ ] `LevelUpEvent` fired from the XP system when a level-up occurs
- [ ] `DeathEvent` fired when player HP reaches 0
- [ ] `CoreEventBusBootstrap` updated to wire all new signal registrations
- [ ] All `GameEventBus.post()` calls happen on the game thread only
- [ ] No event class contains business logic — data only
- [ ] `addFilter` is only used by systems/engine layer code
- [ ] Content scripts use `on<T>(condition = { ... }) { ... }` DSL pattern
- [ ] Server boots and all existing event handlers still function correctly
- [ ] Dialogue continue, command handling, NPC clicks, button clicks all still work
