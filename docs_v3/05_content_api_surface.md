# Phase 5: Content API Surface

## Goal
Build the **complete, stable API** that content developers use for all gameplay operations. After this phase, content code NEVER reaches into `runtime/`, `persistence/`, or internal `systems/` packages.

## Prerequisites
- Phase 2 (unified plugin system) complete
- Phase 3 (skill system) in progress or complete

---

## 5.1 API Design Philosophy

### The "Two-Import Rule"
A content developer adding a simple feature should need at most **two imports**:
1. The plugin interface they're implementing (e.g., `ObjectPlugin`)
2. The content API namespace (e.g., `net.dodian.uber.game.api.*`)

Everything a content developer needs — scheduling, timing, inventory, dialogue, XP, events, messaging — lives under `net.dodian.uber.game.api`.

### The "No Engine Access" Rule
Content code should **never** import from:
- `net.dodian.uber.game.runtime.*`
- `net.dodian.uber.game.persistence.*`
- `net.dodian.uber.game.model.chunk.*` (internal spatial)

This is enforced by architecture tests.

---

## 5.2 API Package Layout

```
net.dodian.uber.game.api/
├── plugin/                     # Plugin interfaces (Phase 2)
│   ├── ContentPlugin.kt
│   ├── ObjectPlugin.kt
│   ├── NpcPlugin.kt
│   ├── ItemPlugin.kt
│   ├── ButtonPlugin.kt
│   ├── CommandPlugin.kt
│   ├── SkillContentPlugin.kt
│   ├── ShopContentPlugin.kt
│   ├── BootstrapPlugin.kt
│   ├── PluginRegistry.kt
│   └── PluginDispatcher.kt
│
├── schedule/                   # Task scheduling
│   ├── GameSchedule.kt         # world/player/npc task launchers
│   └── ScheduleScope.kt        # delay, repeatEvery
│
├── timing/                     # Tick timing
│   └── GameTiming.kt           # currentCycle(), ticksForMs(), msForTicks()
│
├── action/                     # Player action management
│   ├── GameActions.kt          # cancel, reset, isActive
│   └── PolicyPreset.kt         # GATHERING, PRODUCTION, COMBAT, etc.
│
├── skill/                      # Skill API
│   ├── SkillXp.kt              # award(), level(), hasLevel()
│   ├── SkillStateContainer.kt
│   └── template/               # Gathering/Production/Action templates
│
├── inventory/                  # Inventory manipulation
│   └── GameInventory.kt        # add, remove, has, count, freeSlots
│
├── equipment/                  # Equipment queries
│   └── GameEquipment.kt        # equipped(), hasWeapon(), slot()
│
├── dialogue/                   # Dialogue DSL
│   └── GameDialogue.kt         # player.dialogue { ... }
│
├── message/                    # Player messaging
│   └── GameMessage.kt          # send, sendFormatted, yell
│
├── animation/                  # Animation/GFX
│   └── GameAnimation.kt        # animate(), gfx(), resetAnimation()
│
├── movement/                   # Movement control
│   └── GameMovement.kt         # teleport(), walkTo(), freeze(), isMoving()
│
├── event/                      # Event publishing
│   └── GameEvents.kt           # post(), on(), onReturnable()
│
├── world/                      # World queries
│   └── GameWorld.kt            # nearbyPlayers(), nearbyNpcs(), spawnObject()
│
├── safety/                     # Safety checks
│   └── GameSafety.kt           # canTrade(), isBusy(), isDead()
│
└── combat/                     # Combat API
    └── GameCombat.kt           # startAttack(), isInCombat(), maxHit()
```

---

## 5.3 Core API Objects (Detailed Design)

### 5.3.1 `GameSchedule` — Task Scheduling

```kotlin
package net.dodian.uber.game.api.schedule

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.task.TaskHandle
import net.dodian.uber.game.runtime.task.TaskPriority

/**
 * Content-facing API for scheduling delayed and repeating tasks.
 *
 * All delays are in **game ticks** (1 tick = 600ms).
 * Tasks execute on the game thread — no synchronization needed.
 *
 * Example:
 * ```kotlin
 * GameSchedule.player(player) {
 *     delayTicks(3)
 *     player.sendMessage("3 ticks later!")
 * }
 * ```
 */
object GameSchedule {

    /** Schedule a one-shot or multi-step task scoped to the world (no owner). */
    fun world(
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend ScheduleScope.() -> Unit,
    ): TaskHandle

    /** Schedule a task scoped to a player. Cancelled automatically on logout. */
    fun player(
        player: Client,
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend ScheduleScope.() -> Unit,
    ): TaskHandle

    /** Schedule a task scoped to an NPC. Cancelled automatically on death/despawn. */
    fun npc(
        npc: Npc,
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend ScheduleScope.() -> Unit,
    ): TaskHandle

    /** Schedule a repeating world task. Return false from block to stop. */
    fun worldRepeating(
        intervalTicks: Int,
        initialDelayTicks: Int = 0,
        block: suspend () -> Boolean,
    ): TaskHandle
}

class ScheduleScope {
    /** Suspend for the given number of game ticks. */
    suspend fun delayTicks(ticks: Int)

    /** Suspend for a random number of ticks in [min, max]. */
    suspend fun delayTicks(min: Int, max: Int)

    /** Execute block every intervalTicks. Return false to stop. */
    suspend fun repeatEvery(
        intervalTicks: Int,
        initialDelayTicks: Int = 0,
        block: suspend () -> Boolean,
    )
}
```

### 5.3.2 `GameTiming` — Tick Timing

```kotlin
package net.dodian.uber.game.api.timing

object GameTiming {
    /** Current game cycle (tick count since server start). */
    fun currentCycle(): Long

    /** Convert milliseconds to the nearest number of game ticks. */
    fun ticksForMs(milliseconds: Long): Int

    /** Convert game ticks to milliseconds. */
    fun msForTicks(ticks: Int): Long

    /** Current system time in milliseconds. */
    fun nowMs(): Long = System.currentTimeMillis()

    /** The duration of one game tick in milliseconds (600). */
    const val TICK_MS = 600L
}
```

### 5.3.3 `GameInventory` — Inventory Manipulation

```kotlin
package net.dodian.uber.game.api.inventory

import net.dodian.uber.game.model.entity.player.Client

/**
 * Safe inventory manipulation API.
 * All methods include overflow protection and audit logging.
 */
object GameInventory {

    /** Check if the player has at least [amount] of [itemId]. */
    fun has(player: Client, itemId: Int, amount: Int = 1): Boolean

    /** Count how many of [itemId] the player has. */
    fun count(player: Client, itemId: Int): Int

    /** Add items to inventory. Returns true if successful. */
    fun add(player: Client, itemId: Int, amount: Int = 1): Boolean

    /** Remove items from inventory. Returns true if successful. */
    fun remove(player: Client, itemId: Int, amount: Int = 1): Boolean

    /** Number of free inventory slots. */
    fun freeSlots(player: Client): Int

    /** Check if inventory has at least [needed] free slots. */
    fun hasSpace(player: Client, needed: Int = 1): Boolean

    /** Check if player has any of the given item IDs. */
    fun hasAny(player: Client, vararg itemIds: Int): Boolean

    /** Get the item ID in the given slot, or -1 if empty. */
    fun itemAt(player: Client, slot: Int): Int

    /** Check if inventory contains the item, checking both inventory and equipment. */
    fun hasAnywhere(player: Client, itemId: Int): Boolean
}
```

### 5.3.4 `GameDialogue` — Dialogue DSL

```kotlin
package net.dodian.uber.game.api.dialogue

import net.dodian.uber.game.model.entity.player.Client

/**
 * Start a branching dialogue with a player.
 *
 * Example:
 * ```kotlin
 * player.dialogue {
 *     npc(558, "Welcome to my shop!") {
 *         options("What would you like?",
 *             "Buy" to { openShop(player) },
 *             "Nevermind" to { player("Goodbye!") }
 *         )
 *     }
 * }
 * ```
 */
fun Client.dialogue(block: DialogueBuilder.() -> Unit)

class DialogueBuilder {
    fun npc(npcId: Int, vararg lines: String, next: (DialogueBuilder.() -> Unit)? = null)
    fun player(vararg lines: String, next: (DialogueBuilder.() -> Unit)? = null)
    fun options(title: String, vararg options: Pair<String, DialogueBuilder.() -> Unit>)
    fun statement(vararg lines: String, next: (DialogueBuilder.() -> Unit)? = null)
    fun item(itemId: Int, vararg lines: String, next: (DialogueBuilder.() -> Unit)? = null)
}
```

### 5.3.5 `GameMessage` — Player Messaging

```kotlin
package net.dodian.uber.game.api.message

import net.dodian.uber.game.model.entity.player.Client

object GameMessage {
    /** Send a message to the player's chatbox. */
    fun send(player: Client, message: String)

    /** Send a formatted message (with color codes). */
    fun sendFormatted(player: Client, message: String)

    /** Broadcast a message to all online players. */
    fun yell(message: String)

    /** Send a message to all players within [radius] tiles of [player]. */
    fun nearby(player: Client, message: String, radius: Int = 15)
}
```

### 5.3.6 `GameAnimation` — Animation & GFX

```kotlin
package net.dodian.uber.game.api.animation

import net.dodian.uber.game.model.entity.player.Client

object GameAnimation {
    /** Play an animation on the player. */
    fun animate(player: Client, animationId: Int, delay: Int = 0)

    /** Play a graphic (GFX) on the player. */
    fun gfx(player: Client, gfxId: Int, height: Int = 0, delay: Int = 0)

    /** Reset the player's current animation. */
    fun reset(player: Client)

    /** Play an animation on an NPC. */
    fun animateNpc(npc: Npc, animationId: Int, delay: Int = 0)
}
```

### 5.3.7 `GameMovement` — Movement Control

```kotlin
package net.dodian.uber.game.api.movement

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

object GameMovement {
    /** Teleport the player to a position. */
    fun teleport(player: Client, position: Position)

    /** Teleport the player to x, y, z coordinates. */
    fun teleport(player: Client, x: Int, y: Int, z: Int = 0)

    /** Freeze the player for [ticks] game ticks. */
    fun freeze(player: Client, ticks: Int)

    /** Check if the player is currently frozen. */
    fun isFrozen(player: Client): Boolean

    /** Check if the player is currently moving. */
    fun isMoving(player: Client): Boolean

    /** Get the player's current position. */
    fun position(player: Client): Position
}
```

### 5.3.8 `GameEvents` — Event Publishing

```kotlin
package net.dodian.uber.game.api.event

import net.dodian.uber.game.event.GameEvent

object GameEvents {
    /** Publish an event to all listeners. */
    fun <T : GameEvent> post(event: T)

    /** Register a listener for an event type. */
    inline fun <reified T : GameEvent> on(noinline handler: (T) -> Boolean)

    /** Register a listener with a condition filter. */
    inline fun <reified T : GameEvent> on(
        noinline condition: (T) -> Boolean,
        noinline handler: (T) -> Boolean,
    )

    /** Publish an event and collect return values from listeners. */
    fun <T : GameEvent, R> postAndReturn(event: T): List<R>
}
```

---

## 5.4 Extension Functions on Client

For maximum ergonomics, provide extension functions that delegate to the API objects:

```kotlin
package net.dodian.uber.game.api

import net.dodian.uber.game.model.entity.player.Client

// These make content code incredibly concise:

fun Client.hasItem(itemId: Int, amount: Int = 1) = GameInventory.has(this, itemId, amount)
fun Client.addItem(itemId: Int, amount: Int = 1) = GameInventory.add(this, itemId, amount)
fun Client.removeItem(itemId: Int, amount: Int = 1) = GameInventory.remove(this, itemId, amount)
fun Client.freeSlots() = GameInventory.freeSlots(this)

fun Client.message(text: String) = GameMessage.send(this, text)
fun Client.animate(animId: Int, delay: Int = 0) = GameAnimation.animate(this, animId, delay)
fun Client.gfx(gfxId: Int, height: Int = 0) = GameAnimation.gfx(this, gfxId, height)
fun Client.teleport(x: Int, y: Int, z: Int = 0) = GameMovement.teleport(this, x, y, z)
fun Client.teleport(pos: Position) = GameMovement.teleport(this, pos)
fun Client.freeze(ticks: Int) = GameMovement.freeze(this, ticks)

fun Client.skillLevel(skill: Skill) = SkillXp.level(this, skill)
fun Client.hasLevel(skill: Skill, required: Int) = SkillXp.hasLevel(this, skill, required)
fun Client.awardXp(skill: Skill, xp: Double) = SkillXp.award(this, skill, xp)
```

### Content Code Before and After

**Before (reaching into internals):**
```kotlin
import net.dodian.uber.game.systems.skills.ProgressionService
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.systems.api.content.ContentActions
import net.dodian.uber.game.systems.api.content.ContentTiming
import net.dodian.uber.game.systems.action.PlayerActionCancelReason

// ...
client.send(SendMessage("You cut some logs."))
ProgressionService.addXp(client, 25.0, Skill.WOODCUTTING)
ContentActions.cancel(client, PlayerActionCancelReason.NEW_ACTION, false, false)
```

**After (using content API):**
```kotlin
import net.dodian.uber.game.api.*

// ...
player.message("You cut some logs.")
player.awardXp(Skill.WOODCUTTING, 25.0)
GameActions.cancel(player)
```

---

## 5.5 Content API Boundary Architecture Test

```kotlin
@Test
fun `content packages must not import runtime or persistence`() {
    val contentRoots = listOf(
        "net.dodian.uber.game.skill",
        "net.dodian.uber.game.npc",
        "net.dodian.uber.game.object",
        "net.dodian.uber.game.item",
        "net.dodian.uber.game.combat",
        "net.dodian.uber.game.social",
        "net.dodian.uber.game.activity",
        "net.dodian.uber.game.command",
    )

    val forbiddenImports = listOf(
        "net.dodian.uber.game.runtime",
        "net.dodian.uber.game.persistence",
        "net.dodian.uber.game.model.chunk",
    )

    val allowedImports = listOf(
        "net.dodian.uber.game.api",
        "net.dodian.uber.game.model",
        "net.dodian.uber.game.event",
    )

    // Scan all .kt files in content roots
    // For each import statement, verify it matches allowedImports or is standard library
    // Fail if any forbidden import is found
}
```

---

## 5.6 Implementation Steps

### Step 1: Create API package structure
Create all directories and stub files under `api/`.

### Step 2: Implement `GameSchedule`
Delegate to existing `ContentScheduling` / coroutine facade. This is mostly a rename + cleaner API.

### Step 3: Implement `GameTiming`
Delegate to existing `ContentTiming`. Direct mapping.

### Step 4: Implement `GameInventory`
New wrapper around existing `Client` inventory methods. Add audit logging hooks.

### Step 5: Implement `GameEquipment`
New wrapper around `Client.equipment[]` access.

### Step 6: Implement `GameDialogue`
Delegate to existing dialogue DSL. Provide the `Client.dialogue {}` extension.

### Step 7: Implement `GameMessage`
Delegate to existing `SendMessage` packet. Add `yell()` and `nearby()` convenience.

### Step 8: Implement `GameAnimation`
Delegate to existing `Client.performAnimation()`. Cleaner signature.

### Step 9: Implement `GameMovement`
Delegate to existing teleport and freeze logic.

### Step 10: Implement `GameEvents`
Delegate to existing `GameEventBus`. Provide cleaner generic API.

### Step 11: Implement `GameWorld`
New — wrap ChunkManager queries for nearby entity discovery.

### Step 12: Implement `GameSafety`
Move existing `ContentSafety` checks here.

### Step 13: Implement `GameCombat`
Wrap combat state queries and initiation.

### Step 14: Create Client extension functions
Create `api/ClientExtensions.kt` with all the convenience extensions.

### Step 15: Migrate content code
Update all content plugins to use the new API instead of reaching into internals. This can be done gradually — old paths still work.

### Step 16: Add architecture boundary test
Enforce that content packages only import from `api/`, `model/`, and `event/`.

---

## 5.7 Verification Checklist

- [ ] All API objects created and functional
- [ ] Client extension functions provide concise content authoring
- [ ] At least one skill (Woodcutting) fully converted to new API
- [ ] Architecture test enforcing content → API boundary passes
- [ ] No content code directly imports `runtime.*` or `persistence.*`
- [ ] `./gradlew clean build` passes
- [ ] `./gradlew :game-server:test` passes

---

## 5.8 Estimated Effort

| Step | Effort |
|------|--------|
| Create all API stubs | 2 hours |
| Implement delegate wrappers | 4–6 hours |
| Client extension functions | 1 hour |
| Migrate content code | 4–6 hours (incremental) |
| Architecture boundary test | 1 hour |
| **Total** | **~16 hours** |

