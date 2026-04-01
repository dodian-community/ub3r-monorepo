# Spec 04: Content DSL — Interactions, Dialogue, and UI
### Dodian-Flat Final Draft — Based on `game-server old` Actual Codebase Audit

---

> ⛔ **READ [00_MASTER_RULES_READ_FIRST.md](./00_MASTER_RULES_READ_FIRST.md) BEFORE THIS SPEC.**
> The DSL patterns in this spec are **tools for migrating existing content**, not blueprints for creating new content.
> The `Hans.kt` example shows how to re-express Hans's existing dialogue in the new pattern. **Do not add new lines of dialogue.** The `BankInterface.kt` example shows how to migrate the existing deposit button. **Do not add new bank features.**
> Every extension function and DSL helper you implement here must serve existing functionality, not enable new functionality.

---

## 1. Executive Summary

Kotlin DSLs are the primary tool that makes the Dodian-Flat architecture actually developer-friendly. A DSL (`Domain-Specific Language`) in Kotlin is just a set of extension functions, lambda receivers, and type-safe builders that let a content developer write code that reads like English without boilerplate.

In `game-server old`, much of the DSL infrastructure already exists in partial form:
- `DialogueService` at `systems.ui.dialogue.DialogueService` provides a `start(client) { ... }` builder with a `DialogueFactory` receiver
- `InterfaceButtonService` at `systems.ui.buttons.InterfaceButtonService` provides button binding
- `GameEventBus.on<T>` provides event subscription
- The `systems.api.content` package (`ContentActions`, `ContentInteraction`) provides cancellation and state checks

What is missing is a cohesive, documented, and discoverable set of extension functions and DSL helpers that content developers can use without reading engine internals. This spec defines those APIs.

---

## 2. The Dialogue DSL (Real Implementation)

### 2.1 What Already Exists

The `DialogueService.start(client) { ... }` call uses a `DialogueFactory` builder. The `DialogueFactory` is defined in `systems.ui.dialogue.DialogueFactory.kt`. The `DialogueStep` sealed interface defines the step types: `NpcChat`, `PlayerChat`, `Options`, `Option`, `Action`, `Restart`, `Finish`, `FinishThen`.

The actual existing builder functions must be documented accurately:

```kotlin
// Actual builder functions in DialogueFactory (check actual file for current signatures)
// These are what content developers already use:
DialogueService.start(client) {
    npcChat(npcId = 591, text = "Welcome to my shop!")
    playerChat(text = "Thanks!")
    options("What shall I do?",
        "Option 1" to { npcChat(npcId = 591, text = "Option 1!") },
        "Option 2" to { npcChat(npcId = 591, text = "Option 2!") }
    )
    action { player ->
        // arbitrary logic executed inline
    }
    finish { player ->
        // cleanup on end
    }
}
```

### 2.2 What Must Be Added to DialogueFactory

The existing factory is missing several convenience functions that remove friction:

```kotlin
// net.dodian.uber.game.systems.ui.dialogue.DialogueFactory extensions

// Multi-line NPC chat — auto-paginates across multiple dialogue boxes
fun DialogueFactory.npcChatMulti(npcId: Int, vararg lines: String) {
    lines.forEach { line -> npcChat(npcId = npcId, text = line) }
}

// Statement without a face — reuses a "text only" dialogue type
fun DialogueFactory.statement(text: String) {
    action { player -> player.sendMessage(text) }
}

// Conditional branch — only executes steps if condition is true
fun DialogueFactory.onlyIf(condition: (Client) -> Boolean, block: DialogueFactory.() -> Unit) {
    action { player ->
        if (condition(player)) {
            val sub = DialogueFactory().apply(block)
            // prepend steps (advanced implementation detail)
        }
    }
}

// Give item during dialogue
fun DialogueFactory.giveItem(itemId: Int, amount: Int = 1) {
    action { player ->
        player.inventory.addItem(itemId, amount)
        // send quantity message
    }
}

// Require item or halt dialogue
fun DialogueFactory.requireItem(itemId: Int, failMessage: String = "You don't have the required item.") {
    action { player ->
        if (!player.playerHasItem(itemId)) {
            player.sendMessage(failMessage)
            // halt dialogue
        }
    }
}
```

### 2.3 NPC Emote Constants

The `DialogueService.showNpcChat` accepts an `emote: Int` parameter. Content developers should use named constants rather than raw integers:

```kotlin
// net.dodian.uber.game.systems.ui.dialogue.DialogueEmote (already exists in codebase)
// Verify actual values against the client and add any missing:
object DialogueEmote {
    const val CALM = 9760
    const val CALM_TALK = 9762
    const val DEFAULT = 9800        // generic NPC talking face
    const val HAPPY = 9765
    const val SAD = 9775
    const val ANGRY = 9770
    const val SCARED = 9782
    const val LAUGH = 9768
    const val EVIL_LAUGH = 9771
    const val TALKING = 9822
}
```

---

## 3. The NPC Script Pattern (Canonical Form)

Every NPC script in `content.npcs` must follow this pattern. It registers event listeners on the bus. No class hierarchy, no dispatcher calls, no raw opcode references.

### 3.1 Simple Dialogue NPC

```kotlin
// net.dodian.uber.game.content.npcs.Hans.kt
package net.dodian.uber.game.content.npcs

import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.NpcClickEvent
import net.dodian.uber.game.systems.ui.dialogue.DialogueEmote
import net.dodian.uber.game.systems.ui.dialogue.DialogueService

object Hans {
    fun register() {
        GameEventBus.on<NpcClickEvent>(
            condition = { it.npc.id == NpcIds.HANS && it.option == 1 }
        ) { event ->
            DialogueService.start(event.client) {
                npcChat(npcId = NpcIds.HANS, emote = DialogueEmote.CALM_TALK, text = "Hello there. I've been here longer than you.")
                playerChat(text = "How long?")
                npcChat(npcId = NpcIds.HANS, emote = DialogueEmote.CALM_TALK, text = "Ages. You'll find out eventually.")
            }
            true
        }
    }
}
```

### 3.2 NPC with Multiple Options

```kotlin
// net.dodian.uber.game.content.npcs.BankTeller.kt
object BankTeller {
    fun register() {
        GameEventBus.on<NpcClickEvent>(
            condition = { it.npc.id == NpcIds.BANK_TELLER && it.option == 1 }
        ) { event ->
            DialogueService.start(event.client) {
                npcChat(npcId = NpcIds.BANK_TELLER, text = "Good day! How can I help you?")
                options("Choose an option:",
                    "Bank please." to {
                        action { player -> player.openBank() }
                    },
                    "Collect." to {
                        action { player -> player.openBank() } // same for now
                    },
                    "Goodbye." to {
                        npcChat(npcId = NpcIds.BANK_TELLER, text = "Come back soon!")
                    }
                )
            }
            true
        }
    }
}
```

---

## 4. The Object Script Pattern

Object scripts are registered via `GameEventBus.on<ObjectClickEvent>`. The object content system (currently using `ObjectContentRegistry` and `ObjectInteractionService`) provides the routing. Scripts must not directly call `ObjectInteractionService` — they register on the event bus.

```kotlin
// net.dodian.uber.game.content.objects.BankBooth.kt
package net.dodian.uber.game.content.objects

import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.ObjectClickEvent

object BankBooth {
    private val BANK_BOOTH_IDS = setOf(2213, 11402, 45324)

    fun register() {
        GameEventBus.on<ObjectClickEvent>(
            condition = { it.objectId in BANK_BOOTH_IDS && it.option == 1 }
        ) { event ->
            event.client.openBank()
            true
        }

        GameEventBus.on<ObjectClickEvent>(
            condition = { it.objectId in BANK_BOOTH_IDS && it.option == 2 }
        ) { event ->
            // collect from loot interface
            event.client.openBank()
            true
        }
    }
}
```

---

## 5. The Button Click Pattern

Button registrations use `ButtonClickEvent`. The `InterfaceButtonService` already exists at `systems.ui.buttons.InterfaceButtonService`. The pattern for content code is the bus listener, not direct registry manipulation:

```kotlin
// net.dodian.uber.game.content.ui.BankInterface.kt
package net.dodian.uber.game.content.ui

import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.ButtonClickEvent

object BankInterface {

    // Interface and component ID constants — named not magic numbered
    const val INTERFACE_ID = 5292
    const val TITLE_COMPONENT = 5383
    const val CAPACITY_COMPONENT = 5384
    const val INVENTORY_PANE = 5064

    // Button IDs (from the 317 client — verify against actual button opcodes)
    const val BUTTON_DEPOSIT_INVENTORY = 47
    const val BUTTON_DEPOSIT_EQUIPMENT = 48
    const val BUTTON_DEPOSIT_ALL = 49
    const val BUTTON_CLOSE = 45

    fun register() {
        GameEventBus.on<ButtonClickEvent>(
            condition = { it.buttonId == BUTTON_DEPOSIT_INVENTORY }
        ) { event ->
            val player = event.client
            // deposit all inventory items
            for (slot in 0 until player.playerItems.size) {
                if (player.playerItems[slot] > 0) {
                    player.bank.addItem(player.playerItems[slot] - 1, player.playerItemsN[slot])
                    player.playerItems[slot] = 0
                    player.playerItemsN[slot] = 0
                }
            }
            player.refreshInventory()
            player.refreshBank()
            true
        }
    }
}
```

> **Important Note:** The actual button ID values must be verified against the Java packet handler. The values above are illustrative. Check `UpstreamHandler.java` or the relevant input handler for the actual button opcode mapping.

---

## 6. The Item Script Pattern

Item scripts respond to `ItemClickEvent`. Item interactions follow the same bus pattern:

```kotlin
// net.dodian.uber.game.content.items.Teleportation.kt
package net.dodian.uber.game.content.items

import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.ItemClickEvent
import net.dodian.uber.game.model.Position

object Teleportation {
    private val AMULET_OF_GLORY = setOf(1712, 1706, 1704, 1702, 1700)

    fun register() {
        GameEventBus.on<ItemClickEvent>(
            condition = { it.itemId in AMULET_OF_GLORY && it.option == 2 }
        ) { event ->
            val player = event.client
            // Edgeville teleport
            player.teleportTo(Position(3096, 3494, 0))
            true
        }
    }
}
```

---

## 7. Player Extension API

Content developers need a rich set of extension functions on `Client` to avoid direct field access. These extensions live in `net.dodian.uber.game.systems.api.content.Extensions.kt` (or a clearly named file in `systems.api`).

### 7.1 Messaging Extensions

```kotlin
// These already exist in some form — standardize and document:
fun Client.sendMessage(text: String) {
    send(SendMessage(text))
}

fun Client.sendColorMessage(text: String, color: Int) {
    sendMessage("<col=${color.toString(16)}>$text<col>")
}
```

### 7.2 Inventory Extensions

The `Client` object holds inventory in `playerItems` (item IDs, 1-based, 0=empty) and `playerItemsN` (quantities). The existing `playerHasItem(id)` and `playerHasItem(id, amount)` methods exist in Java. Add Kotlin wrappers:

```kotlin
fun Client.hasItem(itemId: Int, amount: Int = 1): Boolean = playerHasItem(itemId, amount)

fun Client.addItem(itemId: Int, amount: Int = 1) {
    addItem(itemId + 1, amount) // legacy 1-based item ID encoding
    refreshInventory()
}

fun Client.removeItem(itemId: Int, amount: Int = 1): Boolean {
    if (!playerHasItem(itemId, amount)) return false
    deleteItem(itemId + 1, amount)
    refreshInventory()
    return true
}

fun Client.inventoryCount(itemId: Int): Int {
    return (0 until playerItems.size).sumOf { slot ->
        if (playerItems[slot] == itemId + 1) playerItemsN[slot] else 0
    }
}
```

### 7.3 Position and World Extensions

```kotlin
val Client.position: Position
    get() = Position(absX, absY, heightLevel)

fun Client.teleportTo(position: Position) {
    teleportToX = position.x
    teleportToY = position.y
    heightLevel = position.z
    mapRegionDidChange = true
}

fun Client.distanceTo(other: Client): Int =
    Misc.distance(absX, absY, other.absX, other.absY)

fun Client.distanceTo(position: Position): Int =
    Misc.distance(absX, absY, position.x, position.y)

fun Client.isWithinRange(position: Position, range: Int): Boolean =
    distanceTo(position) <= range
```

### 7.4 Animation and Visual Extensions

These already exist as direct Java calls in Client (`startAnimation`, `gfx`, etc.). Add Kotlin wrappers:

```kotlin
fun Client.animate(animationId: Int) {
    startAnimation(animationId)
}

fun Client.graphic(graphicId: Int) {
    gfx(graphicId)
}

fun Client.face(target: Client) {
    facePlayer(target.slot)
}

fun Client.face(npc: Npc) {
    faceNpc(npc.slot)
}
```

### 7.5 Skill Level Check API

```kotlin
fun Client.getLevel(skillId: Int): Int = levelForXp[skillId]

fun Client.getXp(skillId: Int): Double = playerXP[skillId].toDouble()

fun Client.hasLevel(skillId: Int, requiredLevel: Int): Boolean = getLevel(skillId) >= requiredLevel
```

---

## 8. The Validation Block DSL

Content scripts frequently need to validate requirements before executing logic. Rather than scattered `if (!player.hasItem(...)) return` checks, a unified validation block:

```kotlin
// net.dodian.uber.game.systems.api.content.Validation.kt

class ValidationBuilder(private val client: Client) {
    private val failures = mutableListOf<String>()

    fun hasLevel(skillId: Int, level: Int, failMessage: String = "You need a ${Skills.name(skillId)} level of at least $level.") {
        if (!client.hasLevel(skillId, level)) failures += failMessage
    }

    fun hasItem(itemId: Int, amount: Int = 1, failMessage: String = "You need the required item.") {
        if (!client.hasItem(itemId, amount)) failures += failMessage
    }

    fun hasFreeSlots(count: Int = 1, failMessage: String = "You need $count free inventory space.") {
        if (client.freeSlots < count) failures += failMessage
    }

    fun custom(failMessage: String, condition: () -> Boolean) {
        if (!condition()) failures += failMessage
    }

    internal fun validate(): Boolean {
        if (failures.isEmpty()) return true
        failures.forEach { client.sendMessage(it) }
        return false
    }
}

fun Client.validate(block: ValidationBuilder.() -> Unit): Boolean {
    return ValidationBuilder(this).apply(block).validate()
}
```

Usage:
```kotlin
if (!player.validate {
    hasLevel(Skills.SMITHING, 60)
    hasItem(2359, 5) // 5 mithril bars
    hasFreeSlots(1)
}) return true // validation failed, messages already sent
```

---

## 9. The `NpcIds` and `ItemIds` Constants Files

All numeric game IDs should be named constants. These files live in `net.dodian.uber.game.content`:

```kotlin
// net.dodian.uber.game.content.NpcIds.kt
package net.dodian.uber.game.content

object NpcIds {
    const val HANS = 0
    const val BANK_TELLER = 166
    const val GENERAL_STORE_KEEPER = 520
    const val AUBURY = 591
    // ... complete list populated as content is migrated
}

// net.dodian.uber.game.content.ItemIds.kt
object ItemIds {
    const val COINS = 995
    const val DRAGON_BONES = 536
    const val BIG_BONES = 532
    const val RUNE_AXLE = 1349
    const val BRONZE_AXE = 1351
    // ... complete list
}

// net.dodian.uber.game.content.ObjectIds.kt
object ObjectIds {
    const val BANK_BOOTH_VARROCK = 2213
    const val BANK_BOOTH_FALADOR = 11402
    const val ALTAR_LUMBRIDGE = 409
    // ... complete list
}
```

---

## 10. Interface ID Constants

All interface and component IDs used in `content.ui` must be named. The following are confirmed from the existing codebase:

| Name | Value | Source / Context |
|:---|:---|:---|
| `SIDEBAR_COMBAT` | 2423 | Combat style sidebar |
| `SIDEBAR_STATS` | 3917 | Skills sidebar |
| `SIDEBAR_QUESTS` | 638 | Quest list |
| `SIDEBAR_INVENTORY` | 3213 | Inventory |
| `SIDEBAR_EQUIPMENT` | 1644 | Worn equipment |
| `SIDEBAR_PRAYER` | 5608 | Prayer toggles |
| `SIDEBAR_MAGIC` | 1151 | Spellbook |
| `SIDEBAR_FRIENDS` | 5065 | Friends list |
| `SIDEBAR_IGNORE` | 5715 | Ignore list |
| `SIDEBAR_LOGOUT` | 2449 | Logout buttons |
| `SIDEBAR_SETTINGS` | 904 | Game options |
| `SIDEBAR_EMOTES` | 147 | Emote list |
| `SIDEBAR_MUSIC` | 4445 | Music tracks |
| `BANK_WINDOW` | 5292 | Main bank window |
| `BANK_TITLE` | 5383 | Bank header text |
| `SHOP_WINDOW` | 3824 | Shop window |
| `TRADE_SCREEN_1` | 3323 | Trade offer screen |
| `TRADE_SCREEN_2` | 3443 | Trade accept screen |
| `DUEL_SCREEN_1` | 6575 | Duel setup |
| `DUEL_SCREEN_2` | 6671 | Duel confirm |
| `MAKEOVER_WINDOW` | 3559 | Makeover mage |
| `SMITHING_WINDOW` | 2311 | Smithing menu |

---

## 11. What Must NOT Change

- The `DialogueService` state machine (`sessions`, `indexedStates`, `Awaiting` enum) must remain intact. The new DSL adds extension functions and convenience builders on top of it, not a replacement.
- The `InterfaceButtonService` binding system must remain intact.
- No changes to Java packet handlers.
- No changes to Netty pipeline.
- No changes to player save flow.

---

## 12. Definition of Done for Spec 04

- [ ] `DialogueFactory` has convenience extensions: `npcChatMulti`, `statement`, `giveItem`, `requireItem`, `onlyIf`
- [ ] `DialogueEmote` constants file is complete and verified against actual client data
- [ ] `NpcIds`, `ItemIds`, `ObjectIds` constant files exist in `content` package
- [ ] Interface ID constants file exists in `content.ui`
- [ ] All content extension functions on `Client` documented and implemented: `sendMessage`, `hasItem`, `addItem`, `removeItem`, `inventoryCount`, `teleportTo`, `distanceTo`, `animate`, `graphic`, `face(npc)`, `face(player)`, `getLevel`, `getXp`, `hasLevel`
- [ ] `ValidationBuilder` DSL implemented with `hasLevel`, `hasItem`, `hasFreeSlots`, `custom`
- [ ] NPC script canonical pattern documented with two complete examples
- [ ] Object script canonical pattern documented with one complete example  
- [ ] Button click pattern documented with one complete example
- [ ] Item click pattern documented with one complete example
- [ ] No raw integer IDs in any content file (must use named constants)
- [ ] No `io.netty.*` imports in any content file
- [ ] No `java.sql.*` imports in any content file
- [ ] `DialogueService` internals are NOT modified
- [ ] `InterfaceButtonService` is NOT replaced
