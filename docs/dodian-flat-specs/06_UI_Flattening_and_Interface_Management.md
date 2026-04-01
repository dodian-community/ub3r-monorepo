# Spec 06: UI Flattening and Interface Management
### Dodian-Flat Final Draft — Based on `game-server old` Actual Codebase Audit

---

> ⛔ **READ [00_MASTER_RULES_READ_FIRST.md](./00_MASTER_RULES_READ_FIRST.md) BEFORE THIS SPEC.**
> Flattening interfaces means **moving existing interface files** into a flat directory, not adding new interfaces or new buttons.
> The `BankInterface.kt` template shows how to consolidate existing bank code. **Do not add new deposit options, new bank tabs, or any feature the bank doesn't currently have.**
> Button IDs shown in examples must be verified to match buttons that **already exist** in the current Java implementation before being used as constants.

---

## 1. Executive Summary

The UI layer in `game-server old` is fragmented across 19 subdirectories under `content/interfaces/`. Additionally, the Java `net/dodian/uber/game/netty/listener/out/` package contains 60+ individual packet Java classes that handle all outbound UI communication with the client.

This spec defines:
1. The exact flattening of all interface directories into `content/ui/`
2. The `InterfaceService` wrapper that Kotlin content code uses to send UI packets
3. The canonical per-interface file structure (one file = one interface)
4. The `InterfaceIds`, `ComponentIds`, and `SidebarIds` constant files
5. How existing Java packet classes are wrapped into Kotlin extension functions
6. What must NOT change (all existing Java packet classes remain)

---

## 2. Current Interface Directory Inventory (What Exists)

Crawl of `content/interfaces/` subdirectories:

| Sub-Package | What It Contains |
|:---|:---|
| `appearance/` | Makeover mage appearance selection |
| `bank/` | Bank button handlers and component layout |
| `combat/` | Combat style button handling |
| `crafting/` | Crafting menu UI logic |
| `dialogue/` | Legacy dialogue UI helpers (superseded by `DialogueService`) |
| `duel/` | Duel confirm/setup screens |
| `emotes/` | Emote button handlers |
| `fletching/` | Fletching menu UI |
| `magic/` | Spellbook button handlers |
| `partyroom/` | Party room chest and dance floor UI |
| `prayer/` | Prayer toggle handlers |
| `quests/` | Quest tab UI |
| `rewards/` | Vote or donation reward UI |
| `settings/` | Options tab handlers |
| `skillguide/` | Skill guide popup panels |
| `skilling/` | Skilling quantity selection menus |
| `slots/` | Equipment interface |
| `smithing/` | Smithing selection UI |
| `trade/` | Trade screen updates |
| `travel/` | Spirit tree, magic carpet etc. travel interfaces |
| `ui/` | Miscellaneous utility UI helpers |

Each of these becomes a single `.kt` file in `content/ui/`. If a directory had multiple files, they merge into one file named after the interface they represent.

---

## 3. Target Flat Structure — `content/ui/`

After flattening, the `content/ui/` package contains exactly these files:

```
content/ui/
    AppearanceInterface.kt     ← appearance/*
    BankInterface.kt           ← bank/*
    CombatInterface.kt         ← combat/*
    CraftingInterface.kt       ← crafting/*
    DuelInterface.kt           ← duel/*
    EmotesInterface.kt         ← emotes/*
    FletchingInterface.kt      ← fletching/*
    MagicInterface.kt          ← magic/*
    PartyRoomInterface.kt      ← partyroom/*
    PrayerInterface.kt         ← prayer/*
    QuestInterface.kt          ← quests/*
    RewardsInterface.kt        ← rewards/*
    SettingsInterface.kt       ← settings/*
    SkillGuideInterface.kt     ← skillguide/*
    SkillingMenuInterface.kt   ← skilling/*
    EquipmentInterface.kt      ← slots/*
    SmithingInterface.kt       ← smithing/*
    TradeInterface.kt          ← trade/*
    TravelInterface.kt         ← travel/*
    InterfaceIds.kt            ← All interface ID constants
    ComponentIds.kt            ← All component ID constants
    SidebarIds.kt              ← Sidebar tab constants
```

The `dialogue/` sub-package under `content/interfaces/` is deleted entirely because dialogue UI is owned by `systems.ui.dialogue.DialogueService` and `DialogueDisplayService`.

---

## 4. The Java Packet Classes — How They Work Today

Every UI packet that goes to the client is a Java class in `net/dodian/uber/game/netty/listener/out/`. These are sent via `client.send(PacketInstance)`. Key packets:

| Java Class | Function | Opcode |
|:---|:---|:---|
| `SendString` | Sets interface text (component ID → text string) | 126 |
| `ShowInterface` | Opens a main interface window | 97 |
| `SetSidebarInterface` / `SetTabInterface` | Sets sidebar tab content | 71 |
| `RemoveInterfaces` | Closes all open interfaces | 219 |
| `SetInterfaceWalkable` | Makes interface walkable/tappable | 208 |
| `SendInventory` | Sends all 28 inventory slots | varies |
| `SendBankItems` | Sends bank items | varies |
| `ResetItems` | Clears item container display | varies |
| `SendFrame246` | Item model display on interface | 246 |
| `SendFrame200` | Animation on interface | 200 |
| `RefreshSkill` | Updates skill level display | varies |
| `SendRunEnergy` | Updates run energy bar | varies |
| `Sound` | Plays a client-side sound | varies |
| `SetVarbit` | Sets a varbit flag | varies |

### 4.1 What Must NOT Change

All Java packet classes must remain untouched. They are functional and changing them risks desynchronizing the client. Kotlin wrappers provide a clean API on top.

---

## 5. The `InterfaceService` — Kotlin Wrapper API

Content developers never call Java packets directly. They use `InterfaceService` or extension functions on `Client`:

```kotlin
// net.dodian.uber.game.systems.ui.InterfaceService
// (currently PlayerUiDeltaProcessor exists — InterfaceService extends its concept)
package net.dodian.uber.game.systems.ui

import net.dodian.uber.game.netty.listener.out.*
import net.dodian.uber.game.model.entity.player.Client

object InterfaceService {

    /**
     * Opens a main interface window for the player.
     * Equivalent to: client.send(ShowInterface(interfaceId))
     */
    fun open(client: Client, interfaceId: Int) {
        client.send(ShowInterface(interfaceId))
    }

    /**
     * Closes all open interfaces.
     * Equivalent to: client.send(RemoveInterfaces())
     */
    fun closeAll(client: Client) {
        client.send(RemoveInterfaces())
    }

    /**
     * Sets text on a specific interface component.
     * Equivalent to: client.send(SendString(text, componentId))
     */
    fun setText(client: Client, componentId: Int, text: String) {
        client.send(SendString(text, componentId))
    }

    /**
     * Sets a sidebar tab to display a specific interface.
     * Equivalent to: client.send(SetSidebarInterface(sidebarId, interfaceId))
     */
    fun setSidebar(client: Client, sidebarId: Int, interfaceId: Int) {
        client.send(SetSidebarInterface(sidebarId, interfaceId))
    }

    /**
     * Opens an item model on an interface component.
     * Equivalent to: client.send(SendFrame246(componentId, zoom, itemId))
     */
    fun setItemModel(client: Client, componentId: Int, itemId: Int, zoom: Int = 200) {
        client.send(SendFrame246(componentId, zoom, itemId))
    }

    /**
     * Refreshes the player's inventory display.
     */
    fun refreshInventory(client: Client) {
        client.send(SendInventory(client.playerItems, client.playerItemsN))
    }
}
```

### 5.1 Extension Functions on `Client` for UI

Content developers should be able to write `player.ui.open(BankInterface.INTERFACE_ID)` or more directly:

```kotlin
// Extension: direct convenience call
fun Client.openInterface(interfaceId: Int) = InterfaceService.open(this, interfaceId)
fun Client.closeInterfaces() = InterfaceService.closeAll(this)
fun Client.setInterfaceText(componentId: Int, text: String) = InterfaceService.setText(this, componentId, text)
fun Client.refreshInventory() = InterfaceService.refreshInventory(this)
```

---

## 6. The Canonical Interface File Structure

Each interface file in `content/ui/` follows this template:

```kotlin
// net.dodian.uber.game.content.ui.BankInterface.kt
package net.dodian.uber.game.content.ui

import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.ButtonClickEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.ui.InterfaceService

/**
 * Bank interface — interface ID 5292.
 *
 * Handles all button clicks and state management for the bank window.
 * Opening the bank is done by calling BankInterface.open(player).
 * Deposit/withdraw logic is registered as event listeners below.
 */
object BankInterface {

    // ─────────────────────────────────────────────
    // Interface Constants
    // ─────────────────────────────────────────────

    const val INTERFACE_ID = 5292
    const val TITLE_COMPONENT = 5383
    const val CAPACITY_COMPONENT = 5384
    const val INVENTORY_PANE_COMPONENT = 5064

    // ─────────────────────────────────────────────
    // Button IDs (verified against 317 client)
    // ─────────────────────────────────────────────

    const val BTN_DEPOSIT_INVENTORY = 47
    const val BTN_DEPOSIT_EQUIPMENT  = 48
    const val BTN_SWAP_INSERT        = 82 // swap/insert mode toggle
    const val BTN_CLOSE              = 45

    // ─────────────────────────────────────────────
    // Open / Close helpers
    // ─────────────────────────────────────────────

    fun open(client: Client) {
        client.openInterface(INTERFACE_ID)
        InterfaceService.setText(client, TITLE_COMPONENT, "The Bank of Dodian")
        InterfaceService.setText(client, CAPACITY_COMPONENT, "Used: ${client.bankCount}/750")
        client.refreshBank()
    }

    fun close(client: Client) {
        client.closeInterfaces()
    }

    // ─────────────────────────────────────────────
    // Event Registration
    // ─────────────────────────────────────────────

    fun register() {
        GameEventBus.on<ButtonClickEvent>(
            condition = { it.buttonId == BTN_DEPOSIT_INVENTORY }
        ) { event ->
            depositInventory(event.client)
            true
        }

        GameEventBus.on<ButtonClickEvent>(
            condition = { it.buttonId == BTN_DEPOSIT_EQUIPMENT }
        ) { event ->
            depositEquipment(event.client)
            true
        }

        GameEventBus.on<ButtonClickEvent>(
            condition = { it.buttonId == BTN_CLOSE }
        ) { event ->
            close(event.client)
            true
        }
    }

    // ─────────────────────────────────────────────
    // Business Logic
    // ─────────────────────────────────────────────

    private fun depositInventory(client: Client) {
        for (slot in 0 until client.playerItems.size) {
            val itemId = client.playerItems[slot] - 1  // legacy 1-based
            val amount = client.playerItemsN[slot]
            if (itemId >= 0 && amount > 0) {
                client.bank.addItem(itemId, amount)
                client.playerItems[slot] = 0
                client.playerItemsN[slot] = 0
            }
        }
        client.refreshInventory()
        client.refreshBank()
    }

    private fun depositEquipment(client: Client) {
        // Equipment deposit logic — reads playerEquipment array
        for (slot in 0 until client.playerEquipment.size) {
            val equipId = client.playerEquipment[slot] - 1
            val equipAmt = client.playerEquipmentN[slot]
            if (equipId >= 0) {
                client.bank.addItem(equipId, equipAmt)
                client.playerEquipment[slot] = 0
                client.playerEquipmentN[slot] = 0
            }
        }
        client.refreshEquipment()
        client.refreshBank()
    }
}
```

---

## 7. Interface Constants Files

### 7.1 `InterfaceIds.kt`

```kotlin
// net.dodian.uber.game.content.ui.InterfaceIds
package net.dodian.uber.game.content.ui

object InterfaceIds {
    // Main windows
    const val BANK         = 5292
    const val SHOP         = 3824
    const val TRADE_1      = 3323
    const val TRADE_2      = 3443
    const val DUEL_1       = 6575
    const val DUEL_2       = 6671
    const val MAKEOVER     = 3559
    const val SMITHING     = 2311
    const val CRAFTING_MENU = 2311  // note: same as smithing — verify against client
    const val LEVEL_UP     = 6247   // level-up congratulations interface

    // Dialogue interfaces (managed by DialogueService — do not open directly)
    const val NPC_CHAT_1LINE  = 4882
    const val NPC_CHAT_2LINE  = 4887
    const val NPC_CHAT_3LINE  = 4893
    const val NPC_CHAT_4LINE  = 4900
    const val PLAYER_CHAT_1   = 968
    const val PLAYER_CHAT_4   = 972
    const val DIALOGUE_OPTIONS_2 = 2459
    const val DIALOGUE_OPTIONS_3 = 2469
    const val DIALOGUE_OPTIONS_4 = 2480
    const val DIALOGUE_OPTIONS_5 = 2492
}
```

### 7.2 `SidebarIds.kt`

```kotlin
// net.dodian.uber.game.content.ui.SidebarIds
package net.dodian.uber.game.content.ui

object SidebarIds {
    // Sidebar tab indices (used with SetSidebarInterface and SetTabInterface)
    const val COMBAT   = 0
    const val STATS    = 1
    const val QUESTS   = 2
    const val INVENTORY = 3
    const val EQUIPMENT = 4
    const val PRAYER   = 5
    const val MAGIC    = 6
    // Tab 7 = clan/empty
    const val FRIENDS  = 8
    const val IGNORE   = 9
    const val LOGOUT   = 10
    const val SETTINGS = 11
    const val EMOTES   = 12
    const val MUSIC    = 13

    // Default interface IDs mapped to each sidebar tab
    const val INTERFACE_COMBAT    = 2423
    const val INTERFACE_STATS     = 3917
    const val INTERFACE_QUESTS    = 638
    const val INTERFACE_INVENTORY = 3213
    const val INTERFACE_EQUIPMENT = 1644
    const val INTERFACE_PRAYER    = 5608
    const val INTERFACE_MAGIC     = 1151
    const val INTERFACE_FRIENDS   = 5065
    const val INTERFACE_IGNORE    = 5715
    const val INTERFACE_LOGOUT    = 2449
    const val INTERFACE_SETTINGS  = 904
    const val INTERFACE_EMOTES    = 147
    const val INTERFACE_MUSIC     = 4445
}
```

---

## 8. Anti-Pattern Reference (What Content Code Must NOT Do)

The following patterns appear in the existing codebase and must be eliminated during migration:

```kotlin
// ❌ BAD: Directly constructing and sending a Java packet class from content code
client.send(SendString("Hello", 5383))

// ✔ GOOD: Using the Kotlin wrapper
client.setInterfaceText(BankInterface.TITLE_COMPONENT, "Hello")

// ❌ BAD: Referencing raw component IDs as magic numbers
client.send(SendString("You died!", 4497))

// ✔ GOOD: Named constant
client.setInterfaceText(ComponentIds.DEATH_MESSAGE, "You died!")

// ❌ BAD: Opening interface by raw integer
client.send(ShowInterface(5292))

// ✔ GOOD: Named open helper
BankInterface.open(client)

// ❌ BAD: Java packet import in content file
import net.dodian.uber.game.netty.listener.out.SendString  // FORBIDDEN in content
```

---

## 9. Input Requests: String and Integer

The server occasionally needs string or integer input from the player (e.g., "How many do you want to make?"). The existing system uses `client.sendEnterAmount()` or `client.sendEnterName()` Java calls. These must be wrapped:

```kotlin
// net.dodian.uber.game.systems.ui.InputRequestService
object InputRequestService {

    /**
     * Prompts the player to enter an integer.
     * The result is delivered through the event bus as IntegerInputEvent,
     * or – when inside a GameTask – via task.waitReturnValue(key).
     */
    fun requestInteger(client: Client, message: String, key: TaskRequestKey<Int>? = null) {
        client.currentIntRequestKey = key
        client.send(SendEnterName(message))  // reuses SendEnterName packet for integer entry
    }

    /**
     * Prompts the player to enter a string.
     */
    fun requestString(client: Client, message: String, key: TaskRequestKey<String>? = null) {
        client.currentStringRequestKey = key
        client.send(SendEnterName(message))
    }
}
```

---

## 10. The `PlayerUiDeltaProcessor` — What Already Exists

`net.dodian.uber.game.systems.ui.PlayerUiDeltaProcessor` (actual class) already exists and handles delta sending of UI state. Content developers do not call this directly. It is called from the game loop's update phase.

---

## 11. Multi-Tab Interface Handling (Trade/Duel)

Trade and duel screens are two-step interfaces that require synchronization between two players:

```kotlin
// net.dodian.uber.game.content.ui.TradeInterface.kt
object TradeInterface {
    const val SCREEN_1 = InterfaceIds.TRADE_1   // 3323
    const val SCREEN_2 = InterfaceIds.TRADE_2   // 3443

    fun openForBoth(player1: Client, player2: Client) {
        open(player1, player2)
        open(player2, player1)
    }

    private fun open(receiver: Client, partner: Client) {
        receiver.openInterface(SCREEN_1)
        // set partner name in trade header
        receiver.setInterfaceText(TradeComponentIds.PARTNER_NAME, partner.playerName)
    }
}
```

---

## 12. Definition of Done for Spec 06

- [ ] All 20 `content/interfaces/*/` subdirectories are deleted after consolidation
- [ ] Each merged interface file exists in `content/ui/` following the template in Section 6
- [ ] `InterfaceIds.kt` constant file created with all known interface IDs
- [ ] `SidebarIds.kt` constant file created with tab indices and default interface IDs
- [ ] `ComponentIds.kt` constant file created for common component IDs
- [ ] `InterfaceService` Kotlin singleton created wrapping Java packet calls
- [ ] All Client extension functions documented: `openInterface`, `closeInterfaces`, `setInterfaceText`, `refreshInventory`
- [ ] `InputRequestService` created for integer and string input requests
- [ ] No content file directly imports `net.dodian.uber.game.netty.listener.out.*` Java packet classes
- [ ] All Java packet classes remain unmodified
- [ ] `content/interfaces/dialogue/` deleted (dialogue owned by `systems.ui.dialogue`)
- [ ] Bank, Trade, Duel, Prayer, Magic sidebar interfaces verified working after consolidation
- [ ] `PlayerUiDeltaProcessor` is not modified
