# Spec 07: Skilling System Redesign — Brain/Data Model
### Dodian-Flat Final Draft — Based on `game-server old` Actual Codebase Audit

---

> ⛔ **READ [00_MASTER_RULES_READ_FIRST.md](./00_MASTER_RULES_READ_FIRST.md) BEFORE THIS SPEC.**
> This spec restructures **existing skill code only**. The XP rates, level requirements, respawn timers, and item IDs in Section 4's data tables are the **current values that already exist in the codebase** — they are reference documentation, not new design decisions.
> **Do not change any XP value, level requirement, or item drop as part of this migration.** If a value in Section 4 does not match what the actual code currently does, the actual code wins — update the spec, not the code.
> `ProgressionService` must produce identical XP output to the current `playerXP[id] += x` assignments it replaces.

---

## 1. Executive Summary

The skilling system has already undergone significant modernization in `game-server old`. Each skill follows a recognizable pattern: a `*Plugin.kt` (event registration), a `*Service.kt` (game logic), and a `*Definitions.kt` (data). However the directory depth (3 levels: `skill/skillname/subtype/`) violates the 2-level rule and the naming is inconsistent.

This spec defines:
1. The exact files that exist per skill (from the actual codebase crawl)
2. The target flat structure (Brain/Data split in `content/skills/`)
3. What moves to `systems/skills/` (engine infrastructure that is not domain content)
4. The `ProgressionService` — XP math, level-up events, combat level calculation
5. The per-skill data schema (node tables with actual RS2 values)
6. Skill-specific migration notes for complex skills (Smithing, Agility, Farming)

---

## 2. What Currently Exists Per Skill (Actual Files)

### 2.1 Woodcutting

| File | Package | Keep / Move |
|:---|:---|:---|
| `WoodcuttingDefinitions.kt` | `content.skills.woodcutting` | Move to `content/skills/WoodcuttingData.kt` |
| `WoodcuttingPlugin.kt` | `content.skills.woodcutting` | Merge into `content/skills/Woodcutting.kt` (Brain) |
| `WoodcuttingService.kt` | `content.skills.woodcutting` | Merge into `content/skills/Woodcutting.kt` (Brain) |
| `WoodcuttingState.kt` | `content.skills.woodcutting` | Delete — state is per-task, no longer a standalone class |
| `woodcutting/objects/WoodcuttingTreesObjects.kt` | nested | Merge into `Woodcutting.kt` |

### 2.2 Cooking

| File | Package | Keep / Move |
|:---|:---|:---|
| `CookingDefinitions.kt` | `content.skills.cooking` | Move to `content/skills/CookingData.kt` |
| `CookingInputService.kt` | `content.skills.cooking` | Merge into `content/skills/Cooking.kt` |
| `CookingState.kt` | `content.skills.cooking` | Delete |
| `cooking/objects/` | nested | Merge into `Cooking.kt` |

### 2.3 Smithing (Most Complex)

| File | Package | Keep / Move |
|:---|:---|:---|
| `SmithingDefinitions.kt` | `content.skills.smithing` | Move to `content/skills/SmithingData.kt` |
| `SmithingPlugin.kt` | `content.skills.smithing` | Merge into `content/skills/Smithing.kt` |
| `SmithingDsl.kt` | `content.skills.smithing` | Keep as `content/skills/SmithingDsl.kt` (optional inner DSL) |
| `SmithingFrameDefinitions.kt` | `content.skills.smithing` | Move to `SmithingData.kt` |
| `SmithingFrameEntry.kt` | `content.skills.smithing` | Move to `SmithingData.kt` |
| `SmithingInterfaceService.kt` | `content.skills.smithing` | Keep as `content/skills/SmithingInterface.kt` |
| `SmithingRequest.kt` | `content.skills.smithing` | Keep as `content/skills/SmithingRequest.kt` |
| `SmithingState.kt` | `content.skills.smithing` | Delete |
| `SmithingTier.kt` | `content.skills.smithing` | Move to `SmithingData.kt` |
| `SmithingDisplayItem.kt` | `content.skills.smithing` | Move to `SmithingData.kt` |
| `SmeltingActionService.kt` | `content.skills.smithing` | Merge into `Smithing.kt` |
| `SmeltingRecipe.kt` | `content.skills.smithing` | Move to `SmithingData.kt` |
| `SmeltingButtonSet.kt` | `content.skills.smithing` | Merge into `SmithingData.kt` |
| `SmeltingButtons.kt` | `content.skills.smithing` | Merge into `SmithingData.kt` |
| `SmeltingInterfaceService.kt` | `content.skills.smithing` | Merge into `SmithingInterface.kt` |
| `SmeltingRequest.kt` | `content.skills.smithing` | Delete |
| `SmeltingState.kt` | `content.skills.smithing` | Delete |
| `FurnaceButtonMapping.kt` | `content.skills.smithing` | Merge into `SmithingData.kt` |
| `OreRequirement.kt` | `content.skills.smithing` | Merge into `SmithingData.kt` |
| `SuperheatService.kt` | `content.skills.smithing` | Keep as `content/skills/Superheat.kt` |
| `objects/AnvilObjects.kt` | nested | Merge into `Smithing.kt` |
| `objects/FurnaceObjects.kt` | nested | Merge into `Smithing.kt` |
| `objects/SmithingObjectComponents.kt` | nested | Merge into `SmithingInterface.kt` |

### 2.4 Agility

| File | Package | Keep / Move |
|:---|:---|:---|
| `AgilityCourseService.kt` | `content.skills.agility` | Keep as `content/skills/Agility.kt` (Brain) |
| `AgilityDefinitions.kt` | `content.skills.agility` | Move to `content/skills/AgilityData.kt` |
| `AgilityPlugin.kt` | `content.skills.agility` | Merge into `Agility.kt` |
| `DesertCarpetService.kt` | `content.skills.agility` | Keep as `content/skills/AgilityTravel.kt` |
| `WerewolfCourseService.kt` | `content.skills.agility` | Keep as `content/skills/AgilityWerewolf.kt` |
| `objects/BarbarianCourseObjectBindings.kt` | nested | Merge into `Agility.kt` |
| `objects/GnomeCourseObjectBindings.kt` | nested | Merge into `Agility.kt` |
| `objects/WerewolfCourseObjectBindings.kt` | nested | Merge into `AgilityWerewolf.kt` |
| `objects/WildernessCourseObjectBindings.kt` | nested | Merge into `Agility.kt` |
| (all `*ObjectComponents.kt` files) | nested | Merge into their respective Bindings file |

### 2.5 Thieving

| File | Package | Keep / Move |
|:---|:---|:---|
| `ThievingDefinitions.kt` | `content.skills.thieving` | Move to `content/skills/ThievingData.kt` |
| `ThievingDefinition.kt` | `content.skills.thieving` | Merge into `ThievingData.kt` |
| `ThievingPlugin.kt` | `content.skills.thieving` | Merge into `content/skills/Thieving.kt` |
| `ThievingService.kt` | `content.skills.thieving` | Merge into `Thieving.kt` |
| `objects/***Objects.kt` | nested | Merge into `Thieving.kt` |
| `objects/ThievingObjectComponents.kt` | nested | Merge into `ThievingData.kt` |
| `plunder/PyramidPlunderGlobalState.kt` | nested | Keep as `content/skills/PyramidPlunder.kt` |
| `plunder/PyramidPlunderPlayerState.kt` | nested | Merge into `PyramidPlunder.kt` |
| `plunder/PyramidPlunderService.kt` | nested | Merge into `PyramidPlunder.kt` |

### 2.6 Slayer

| File | Package | Keep / Move |
|:---|:---|:---|
| `SlayerDefinitions.kt` | `content.skills.slayer` | Move to `content/skills/SlayerData.kt` |
| `SlayerPlugin.kt` | `content.skills.slayer` | Merge into `content/skills/Slayer.kt` |
| `SlayerService.kt` | `content.skills.slayer` | Merge into `Slayer.kt` |
| `SlayerTaskDefinition.kt` | `content.skills.slayer` | Move to `SlayerData.kt` |
| `items/SlayerGemItems.kt` | nested | Merge into `Slayer.kt` |
| `items/SlayerMaskItems.kt` | nested | Merge into `Slayer.kt` |

### 2.7 Runecrafting

| File | Package | Keep / Move |
|:---|:---|:---|
| `RunecraftingDefinitions.kt` | `content.skills.runecrafting` | Move to `content/skills/RunecraftingData.kt` |
| `RunecraftingPlugin.kt` | `content.skills.runecrafting` | Merge into `content/skills/Runecrafting.kt` |
| `RunecraftingPouchService.kt` | `content.skills.runecrafting` | Merge into `Runecrafting.kt` |
| `RunecraftingRequest.kt` | `content.skills.runecrafting` | Delete — transient |
| `RunecraftingService.kt` | `content.skills.runecrafting` | Merge into `Runecrafting.kt` |
| `RunecraftingState.kt` | `content.skills.runecrafting` | Delete |
| `items/RunePouchItems.kt` | nested | Merge into `Runecrafting.kt` |
| `objects/RunecraftingObjectBindings.kt` | nested | Merge into `Runecrafting.kt` |
| `objects/RunecraftingObjectComponents.kt` | nested | Merge into `Runecrafting.kt` |

### 2.8 Prayer

| File | Package | Keep / Move |
|:---|:---|:---|
| `prayer/items/BuryBonesItems.kt` | nested | Move to `content/skills/Prayer.kt` |
| `prayer/objects/AltarObjects.kt` | nested | Merge into `Prayer.kt` |

### 2.9 Skills Core (MUST Move to Systems Layer)

| File | Current Package | Target Package |
|:---|:---|:---|
| `GatheringTask.kt` | `content.skills.core.runtime` | `systems.skills.GatheringTask` |
| `ActionStopReason.kt` | `content.skills.core.runtime` | `systems.skills.ActionStopReason` |
| `core/events/` | `content.skills.core.events` | `systems.skills` (skilling events moved to `events.skilling`) |
| `core/progression/` | `content.skills.core.progression` | `systems.skills` (see ProgressionService below) |
| `core/requirements/Requirement.kt` | `content.skills.core.requirements` | `systems.skills.requirements` |
| `core/requirements/ValidationResult.kt` | `content.skills.core.requirements` | `systems.skills.requirements` |
| `core/resource/` | `content.skills.core.resource` | `systems.skills` |
| `core/runtime/` (non-GatheringTask) | `content.skills.core.runtime` | `systems.skills` |

---

## 3. The `ProgressionService` (New — Systems Layer)

All XP gain must go through `ProgressionService`. This ensures XP events fire consistently, level-up detection happens in one place, and no skill bypasses the event system.

```kotlin
// net.dodian.uber.game.systems.skills.ProgressionService
package net.dodian.uber.game.systems.skills

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.LevelUpEvent
import net.dodian.uber.game.netty.listener.out.RefreshSkill

object ProgressionService {

    /**
     * Adds XP to the given skill for the player.
     * Fires LevelUpEvent if the gain crosses a level boundary.
     * Updates player arrays and sends a RefreshSkill packet immediately.
     *
     * @param client  The player receiving XP.
     * @param skillId The skill index (use constants from Skills object).
     * @param xp      The amount of XP to add.
     */
    fun addXp(client: Client, skillId: Int, xp: Double) {
        val currentXp = client.playerXP[skillId].toLong()
        val newXp = (currentXp + xp).toLong().coerceAtMost(MAX_XP)
        val oldLevel = levelForXp(currentXp)
        val newLevel = levelForXp(newXp)

        client.playerXP[skillId] = newXp.toInt()
        client.levelForXp[skillId] = newLevel

        client.send(RefreshSkill(skillId, client.playerXP[skillId], client.levelForXp[skillId]))

        if (newLevel > oldLevel) {
            for (level in (oldLevel + 1)..newLevel) {
                GameEventBus.post(LevelUpEvent(client, skillId, level))
                // The LevelUpEvent listener in content will send the level-up interface
            }
        }
    }

    /**
     * Returns the level (1-99) for the given XP amount.
     * Uses the standard RS2 XP table.
     */
    fun levelForXp(xp: Long): Int {
        for (level in 99 downTo 1) {
            if (xp >= XP_FOR_LEVEL[level]) return level
        }
        return 1
    }

    /**
     * Returns the XP required to be at the start of the given level.
     */
    fun xpForLevel(level: Int): Long = XP_FOR_LEVEL[level.coerceIn(1, 99)]

    /**
     * Calculates the combat level from current skill levels.
     * Formula: (Attack + Strength + Defence + HP) / 4 + max(Ranged/2, Magic/2, (Prayer/2 + Attack + Strength) / 2) / 4
     * Standard 317 combat level formula.
     */
    fun calculateCombatLevel(client: Client): Int {
        val attack  = client.levelForXp[Skills.ATTACK]
        val defence = client.levelForXp[Skills.DEFENCE]
        val strength = client.levelForXp[Skills.STRENGTH]
        val hp      = client.levelForXp[Skills.HITPOINTS]
        val prayer  = client.levelForXp[Skills.PRAYER]
        val ranged  = client.levelForXp[Skills.RANGED]
        val magic   = client.levelForXp[Skills.MAGIC]

        val base = (defence + hp + Math.floor(prayer / 2.0)).toInt()
        val melee = attack + strength
        val range = Math.floor(ranged * 1.5).toInt()
        val mage  = Math.floor(magic * 1.5).toInt()
        val special = maxOf(melee, range, mage)
        return Math.floor((base + special) / 4.0).toInt()
    }

    const val MAX_XP = 200_000_000L

    // Standard RS2 XP table — cumulative XP needed to reach each level
    private val XP_FOR_LEVEL = intArrayOf(
        0, 0, 83, 174, 276, 388, 512, 650, 801, 969, 1154, // 0-10
        1358, 1584, 1833, 2107, 2411, 2746, 3115, 3523, 3973, 4470, // 11-20
        5018, 5624, 6291, 7028, 7842, 8740, 9730, 10824, 12031, 13363, // 21-30
        14833, 16456, 18247, 20224, 22406, 24815, 27473, 30408, 33648, 37224, // 31-40
        41171, 45529, 50339, 55649, 61512, 67983, 75127, 83014, 91721, 101333, // 41-50
        111945, 123660, 136594, 150872, 166636, 184040, 203254, 224466, 247886, 273742, // 51-60
        302288, 333804, 368599, 407015, 449428, 496254, 547953, 605032, 668051, 737627, // 61-70
        814445, 899257, 992895, 1096278, 1210421, 1336443, 1475581, 1629200, 1798808, 1986068, // 71-80
        2192818, 2421087, 2673114, 2951373, 3258594, 3597792, 3972294, 4385776, 4842295, 5346332, // 81-90
        5902831, 6517253, 7195629, 7944614, 8771558, 9684577, 10692629, 11805606, 13034431, 200000000 // 91-99+
    )
}
```

---

## 4. Skill Node Data Schema (Authoritative Values)

### 4.1 Woodcutting Tree Data

| Tree | Object ID | Level Req | XP | Log Item ID | Respawn (ticks) | Tool Animation |
|:---|:---|:---|:---|:---|:---|:---|
| Normal | 1276, 2037 | 1 | 25 | 1511 | 25 | 879 (bronze axe) |
| Oak | 1281 | 15 | 37.5 | 1521 | 40 | 875 |
| Willow | 1308 | 30 | 67.5 | 1519 | 70 | 875 |
| Maple | 1307 | 45 | 100 | 1517 | 100 | 875 |
| Yew | 1309 | 60 | 175 | 1515 | 200 | 875 |
| Magic | 1306 | 75 | 250 | 1513 | 400 | 875 |
| Teaks | 9036 | 35 | 85 | 6333 | 35 | 875 |
| Mahogany | 9034 | 50 | 125 | 6332 | 70 | 875 |

### 4.2 Mining Rock Data

| Rock | Object ID | Level Req | XP | Ore Item ID | Respawn (ticks) |
|:---|:---|:---|:---|:---|:---|
| Clay | 2108 | 1 | 5 | 434 | 3 |
| Copper | 2090 | 1 | 17.5 | 436 | 7 |
| Tin | 2094 | 1 | 17.5 | 438 | 7 |
| Iron | 2092 | 15 | 35 | 440 | 9 |
| Coal | 2096 | 30 | 50 | 453 | 25 |
| Gold | 2098 | 40 | 65 | 444 | 50 |
| Mithril | 2102 | 55 | 80 | 447 | 100 |
| Adamantite | 2104 | 70 | 95 | 449 | 180 |
| Runite | 2106 | 85 | 125 | 451 | 500 |
| Sandstone | 11386 | 35 | 30 | 6971 | 7 |
| Granite | 11387 | 45 | 50 | 6979 | 7 |

### 4.3 Fishing Spot Data

| Spot Type | NPC ID | Level Req | XP | Fish Item ID | Tool | Tool Item ID |
|:---|:---|:---|:---|:---|:---|:---|
| Net/Bait | 1518 | 1 (shrimp) | 10/20 | 317/327 | Small Net/Rod | 303/307 |
| Lure/Bait | 1519 | 20 (trout) | 50/70 | 335/329 | Fly Rod/Rod | 309/307 |
| Cage/Harpoon | 1520 | 40 (lobster) | 90/100 | 377/371 | Lobster Cage/Harpoon | 301/311 |
| Net/Harpoon | 1521 | 62 (monkfish) | 120 | 7944 | Small Net | 303 |
| Shark | 1522 | 76 | 110 | 383 | Harpoon | 311 |

### 4.4 Cooking Recipe Data

| Food (Raw) | Item ID | Cooked Item ID | Level Req | XP | Stop Burn Level | Animation |
|:---|:---|:---|:---|:---|:---|:---|
| Shrimp | 317 | 315 | 1 | 30 | 34 | 883 |
| Sardine | 327 | 325 | 1 | 40 | 38 | 883 |
| Herring | 345 | 347 | 5 | 50 | 41 | 883 |
| Mackerel | 353 | 355 | 10 | 60 | 45 | 883 |
| Trout | 335 | 333 | 15 | 70 | 50 | 883 |
| Cod | 341 | 339 | 18 | 75 | 52 | 883 |
| Pike | 349 | 351 | 20 | 80 | 58 | 883 |
| Salmon | 329 | 331 | 25 | 90 | 58 | 883 |
| Tuna | 359 | 361 | 30 | 100 | 63 | 883 |
| Lobster | 377 | 379 | 40 | 120 | 74 | 883 |
| Bass | 363 | 365 | 43 | 130 | 80 | 883 |
| Swordfish | 371 | 373 | 45 | 140 | 81 | 883 |
| Monkfish | 7944 | 7946 | 62 | 150 | 92 | 883 |
| Shark | 383 | 385 | 80 | 210 | 94 | 883 |

### 4.5 Prayer Bone Data

| Bone | Item ID | XP | Type |
|:---|:---|:---|:---|
| Bones | 526 | 4.5 | Standard |
| Big Bones | 532 | 15 | Standard |
| Baby Dragon Bones | 2806 | 30 | Dragon |
| Dragon Bones | 536 | 72 | Dragon |
| Dagannoth Bones | 6729 | 125 | Dagannoth |
| Wyvern Bones | 6812 | 50 | Wyvern |
| Fayrg Bones | 4830 | 84 | Fayrg |
| Raurg Bones | 4832 | 96 | Raurg |
| Ourg Bones | 4834 | 140 | Ourg |

---

## 5. Target Directory Layout After Migration

```
content/skills/
    Woodcutting.kt          ← Brain (service, plugin, tree objects merged)
    WoodcuttingData.kt      ← Data (tree definitions, tool definitions)
    Cooking.kt              ← Brain
    CookingData.kt          ← Data
    Smithing.kt             ← Brain
    SmithingData.kt         ← Data
    SmithingInterface.kt    ← UI-specific helpers for the smithing menu
    Superheat.kt            ← Superheat spell logic (standalone)
    Fishing.kt              ← Brain
    FishingData.kt          ← Data
    Firemaking.kt           ← Brain
    FiremakingData.kt       ← Data
    Crafting.kt             ← Brain
    CraftingData.kt         ← Data
    Fletching.kt            ← Brain
    FletchingData.kt        ← Data
    Herblore.kt             ← Brain
    HerbloreData.kt         ← Data
    Mining.kt               ← Brain
    MiningData.kt           ← Data
    Prayer.kt               ← Brain
    PrayerData.kt           ← Data
    Runecrafting.kt         ← Brain
    RunecraftingData.kt     ← Data
    Slayer.kt               ← Brain
    SlayerData.kt           ← Data
    Agility.kt              ← Brain (Gnome, Barbarian, Wilderness courses merged)
    AgilityData.kt          ← Data (course definitions)
    AgilityWerewolf.kt      ← Werewolf Agility Arena (separate due to unique mechanics)
    AgilityTravel.kt        ← Desert carpet/agility travel (similar to Agility)
    Thieving.kt             ← Brain
    ThievingData.kt         ← Data
    PyramidPlunder.kt       ← Pyramid Plunder minigame (complex enough for own file)
    Farming.kt              ← Brain
    FarmingData.kt          ← Data
    SkillGuide.kt           ← Skill guide popup (moved from guide/ subdirectory)
    SkillGuideData.kt       ← Skill guide definitions
```

---

## 6. The Skilling Interruption Contract

Any running `GatheringTask` or `ProductionTask` must stop when:
1. Player clicks to walk (`wQueueReadPtr != wQueueWritePtr` in the next tick)
2. Player enters combat (detected via flag on `Client`)
3. Player opens a conflicting interface
4. Player logs out

The `GatheringTask.onTick()` implementation already checks `!client.isActive || client.disconnected`. The `PlayerActionCancellationService` must call `GatheringTask.cancel()` on the currently running task when the above conditions trigger.

**Current State:** Each skill plugin already calls `cancel()` from its event listeners when the relevant cancel conditions are detected. This pattern must be preserved in the merged Brain files.

---

## 7. Farming — Tick-Based System

Farming uses `FarmingScheduler` at `systems.world.farming.FarmingScheduler`. This is a systems-layer class that manages patch growth independent of player sessions. It must NOT be moved — it is correctly placed.

The `Farming.kt` brain file in `content/skills/` handles only:
- Player clicks on patches (planting, harvesting, checking health)
- Interface display for compost/herb choices

The growth calculation and cycle management remain in `systems.world.farming`.

---

## 8. Definition of Done for Spec 07

- [ ] `content/skills/core/` directory deleted; all meaningful classes moved to `systems/skills/`
- [ ] `ProgressionService` created in `systems.skills` with `addXp()`, `levelForXp()`, `xpForLevel()`, `calculateCombatLevel()`
- [ ] `LevelUpEvent` is fired from `ProgressionService.addXp()` — not from skill brain files
- [ ] Every skill has at most 2 files in `content/skills/` (Brain + Data), with complex skills allowed a third for UI-specific code
- [ ] All nested `objects/` and `items/` sub-packages under skill directories are deleted
- [ ] `WoodcuttingData`, `MiningData`, `FishingData`, `CookingData` contain the data tables from Section 4
- [ ] `GatheringTask` is in `systems.skills`, not `content`
- [ ] `FarmingScheduler` remains in `systems.world.farming` — not moved
- [ ] No skill brain file imports `java.sql.*` or `io.netty.*`
- [ ] `ProgressionService.addXp()` is the only way XP is granted — no direct `playerXP[id] += x` in content
- [ ] All 21 skills (Attack, Defence, Strength, Hitpoints, Ranged, Prayer, Magic, Cooking, Woodcutting, Fletching, Fishing, Firemaking, Crafting, Smithing, Mining, Herblore, Agility, Thieving, Slayer, Farming, Runecrafting) are accounted for in the target structure
- [ ] MySQL schema (`character_stats` table) is unchanged
