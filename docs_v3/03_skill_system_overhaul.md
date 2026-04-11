# Phase 3: Skill System Overhaul

## Goal
Create a **uniform, zero-boilerplate skill plugin architecture** with template patterns for gathering skills, production skills, and action skills. After this phase, adding a new skill takes one data file and one plugin object.

## Prerequisites
- Phase 1 (package restructure) complete
- Phase 2 (unified plugin system) complete

---

## 3.1 The Problem with Current Skill Architecture

### Inconsistency
Every skill uses a different pattern. Woodcutting uses `gatheringAction()`, Cooking uses a custom state machine, Agility is still mostly Java, Farming has its own entire runtime service. A developer who learned to write Woodcutting content cannot transfer that knowledge to Cooking.

### Scattered State
Skill state lives on `Client` as ad-hoc properties:
```kotlin
client.woodcuttingState   // WoodcuttingState?
client.fletchingState     // FletchingState?
client.craftingState      // CraftingState?
client.isFiremaking       // Boolean
```
This means every new skill adds a new property to the already-massive `Client` class.

### No Shared Templates
The `ResourceSkillContent` DSL and `gatheringAction()` exist but are only used by 2 out of 15+ skills. Production skills (Cooking, Fletching, Smithing, Herblore) have no shared template at all.

---

## 3.2 Design: Skill Archetypes

Every skill in RuneScape falls into one of four archetypes:

### Archetype 1: Gathering Skill
Click a resource node → animate → wait → roll success → gather item → award XP → repeat.

**Examples**: Woodcutting, Mining, Fishing

**Template data**:
- Resource nodes (object IDs, required level, XP, item produced)
- Tools (item IDs, required level, speed bonus, animation)
- Success formula (level-based, with tool tier bonus)

### Archetype 2: Production Skill
Use item → open make interface → select quantity → animate → wait → produce item → award XP → repeat.

**Examples**: Cooking, Fletching, Smithing, Herblore, Crafting, Runecrafting

**Template data**:
- Recipes (input items, output item, required level, XP)
- Production interface (make-1, make-5, make-all)
- Animation and delay
- Failure chance (e.g., burning food)

### Archetype 3: Action Skill
Click something → check level → perform timed action → award XP. Each action is unique.

**Examples**: Agility (obstacles), Thieving (pickpocketing/stalls), Prayer (bury bones)

**Template data**:
- Actions (trigger ID, required level, XP, animation, delay)
- Success formula (level-based, with modifiers)
- Unique per-action effects (e.g., stun on fail for Thieving)

### Archetype 4: Passive/Complex Skill
Skills with unique mechanics that don't fit the above templates.

**Examples**: Farming (tick-based growth), Slayer (task assignment), Magic (combat integration)

These will have custom implementations but still conform to the `SkillContentPlugin` interface.

---

## 3.3 The Skill State Container

### Replace scattered Client properties with a unified skill state map:

```kotlin
package net.dodian.uber.game.api.skill

/**
 * Container for all active skill states on a player.
 * Replaces ad-hoc Client properties (woodcuttingState, fletchingState, etc.)
 */
class SkillStateContainer {
    private val states = EnumMap<Skill, Any>(Skill::class.java)

    fun <T : Any> get(skill: Skill): T? {
        @Suppress("UNCHECKED_CAST")
        return states[skill] as? T
    }

    fun set(skill: Skill, state: Any?) {
        if (state == null) {
            states.remove(skill)
        } else {
            states[skill] = state
        }
    }

    fun clear(skill: Skill) = states.remove(skill)

    fun clearAll() = states.clear()

    fun activeSkill(): Skill? = states.keys.firstOrNull()

    fun isActive(skill: Skill): Boolean = states.containsKey(skill)
}
```

Then on `Client`:
```kotlin
val skillState = SkillStateContainer()

// Usage:
val wcState: WoodcuttingState? = skillState.get(Skill.WOODCUTTING)
skillState.set(Skill.WOODCUTTING, WoodcuttingState(...))
skillState.clear(Skill.WOODCUTTING)
```

### Migration from old properties:
1. Replace `client.woodcuttingState` → `client.skillState.get<WoodcuttingState>(Skill.WOODCUTTING)`
2. Replace `client.fletchingState` → `client.skillState.get<FletchingState>(Skill.FLETCHING)`
3. Remove individual state properties from `Client`
4. Provide extension properties for backward compatibility during migration:
```kotlin
var Client.woodcuttingState: WoodcuttingState?
    get() = skillState.get(Skill.WOODCUTTING)
    set(value) = skillState.set(Skill.WOODCUTTING, value)
```

---

## 3.4 Gathering Skill Template

### Data Model
```kotlin
package net.dodian.uber.game.api.skill.template

data class GatheringNodeDef(
    val name: String,
    val objectIds: IntArray,
    val requiredLevel: Int,
    val experience: Double,
    val resourceItemId: Int,
    val depletionChance: Double = 0.0,    // 0.0 = never depletes (fish spot), 1.0 = always
    val depletedObjectId: Int = -1,       // What it turns into when depleted (tree stump)
    val respawnTicks: Int = 0,            // How long until it comes back
    val restThreshold: Int = 0,           // 0 = no auto-rest mechanic
)

data class GatheringToolDef(
    val name: String,
    val itemId: Int,
    val requiredLevel: Int,
    val speedBonus: Double,
    val animationId: Int,
)

data class GatheringSkillDef(
    val skill: Skill,
    val nodes: List<GatheringNodeDef>,
    val tools: List<GatheringToolDef>,
    val startMessage: String = "You swing your tool at the resource...",
    val gatherMessage: (resourceName: String) -> String = { "You manage to get some ${it.lowercase()}." },
    val noToolMessage: String = "You need the right tool to do this.",
    val levelTooLowMessage: (required: Int) -> String = { "You need a level of $it to do this." },
)
```

### DSL Builder
```kotlin
fun gatheringSkill(skill: Skill, block: GatheringSkillDefBuilder.() -> Unit): GatheringSkillDef {
    return GatheringSkillDefBuilder(skill).apply(block).build()
}

class GatheringSkillDefBuilder(private val skill: Skill) {
    private val nodes = mutableListOf<GatheringNodeDef>()
    private val tools = mutableListOf<GatheringToolDef>()
    private var startMessage = "You swing your tool at the resource..."
    // ... more customization points

    fun node(name: String, block: GatheringNodeDefBuilder.() -> Unit) {
        nodes += GatheringNodeDefBuilder(name).apply(block).build()
    }

    fun tool(name: String, block: GatheringToolDefBuilder.() -> Unit) {
        tools += GatheringToolDefBuilder(name).apply(block).build()
    }

    fun build(): GatheringSkillDef { /* ... */ }
}
```

### Complete Woodcutting Example (After Refactor)
```kotlin
package net.dodian.uber.game.skill.woodcutting

object WoodcuttingPlugin : SkillContentPlugin {
    override val pluginName = "Woodcutting"
    override val skill = Skill.WOODCUTTING

    override val bindings = gatheringSkill(Skill.WOODCUTTING) {
        startMessage = "You swing your axe at the tree..."
        gatherMessage = { "You cut some ${it.lowercase()}." }
        noToolMessage = "You need an axe to chop down this tree."
        levelTooLowMessage = { "You need a Woodcutting level of $it to cut this tree." }

        // Tools (highest tier first for resolution)
        tool("Dragon Axe")      { itemId = 6739;  requiredLevel = 61; speedBonus = 0.15; animationId = 2846 }
        tool("Rune Axe")        { itemId = 1359;  requiredLevel = 41; speedBonus = 0.10; animationId = 867 }
        tool("Adamant Axe")     { itemId = 1357;  requiredLevel = 31; speedBonus = 0.08; animationId = 869 }
        tool("Mithril Axe")     { itemId = 1355;  requiredLevel = 21; speedBonus = 0.06; animationId = 871 }
        tool("Black Axe")       { itemId = 1361;  requiredLevel = 11; speedBonus = 0.04; animationId = 873 }
        tool("Steel Axe")       { itemId = 1353;  requiredLevel = 6;  speedBonus = 0.03; animationId = 875 }
        tool("Iron Axe")        { itemId = 1349;  requiredLevel = 1;  speedBonus = 0.01; animationId = 877 }
        tool("Bronze Axe")      { itemId = 1351;  requiredLevel = 1;  speedBonus = 0.00; animationId = 879 }

        // Resource nodes
        node("Normal Tree")     { objectIds(1276, 1278); requiredLevel = 1;  experience = 25.0;  resourceItemId = 1511; depletionChance = 1.0; depletedObjectId = 1342; respawnTicks = 50 }
        node("Oak Tree")        { objectIds(1281);       requiredLevel = 15; experience = 37.5;  resourceItemId = 1521; depletionChance = 0.125; depletedObjectId = 1342; respawnTicks = 100 }
        node("Willow Tree")     { objectIds(1308, 5551); requiredLevel = 30; experience = 67.5;  resourceItemId = 1519; depletionChance = 0.125; depletedObjectId = 1342; respawnTicks = 120 }
        node("Maple Tree")      { objectIds(1307);       requiredLevel = 45; experience = 100.0; resourceItemId = 1517; depletionChance = 0.125; depletedObjectId = 1342; respawnTicks = 150 }
        node("Yew Tree")        { objectIds(1309);       requiredLevel = 60; experience = 175.0; resourceItemId = 1515; depletionChance = 0.125; depletedObjectId = 1342; respawnTicks = 200 }
        node("Magic Tree")      { objectIds(1306);       requiredLevel = 75; experience = 250.0; resourceItemId = 1513; depletionChance = 0.125; depletedObjectId = 1342; respawnTicks = 300 }
    }
}
```

**That's the entire Woodcutting plugin.** All the action loop, delay calculation, requirement checking, XP awarding, animation, messaging, and depletion handling is done by the `GatheringSkillRuntime` using the definition.

---

## 3.5 Production Skill Template

### Data Model
```kotlin
data class RecipeDef(
    val name: String,
    val requiredLevel: Int,
    val experience: Double,
    val inputs: List<Pair<Int, Int>>,     // itemId to quantity
    val output: Pair<Int, Int>,           // itemId to quantity
    val animationId: Int = -1,
    val delayTicks: Int = 3,
    val failChance: Double = 0.0,         // 0.0 = never fails
    val failOutput: Int = -1,             // Item produced on fail (e.g., burnt food)
    val failMessage: String = "You failed.",
    val successMessage: String = "",
)

data class ProductionSkillDef(
    val skill: Skill,
    val recipes: List<RecipeDef>,
    val startMessage: String = "You begin crafting...",
    val requiresInterface: Boolean = true, // Show make-X interface?
)
```

### Complete Cooking Example
```kotlin
package net.dodian.uber.game.skill.cooking

object CookingPlugin : SkillContentPlugin {
    override val pluginName = "Cooking"
    override val skill = Skill.COOKING

    override val bindings = productionSkill(Skill.COOKING) {
        startMessage = "You begin cooking..."

        // Object IDs for cooking ranges/fires
        triggerObjects(114, 2728, 2729, 2732)  // Cooking ranges
        triggerObjects(2732)                      // Fire

        recipe("Shrimp")        { input(317);  output(315);  requiredLevel = 1;  experience = 30.0;  failOutput = 323;  failChance = level(1, 33) }
        recipe("Trout")         { input(335);  output(333);  requiredLevel = 15; experience = 70.0;  failOutput = 343;  failChance = level(15, 50) }
        recipe("Lobster")       { input(377);  output(379);  requiredLevel = 40; experience = 120.0; failOutput = 381;  failChance = level(40, 74) }
        recipe("Swordfish")     { input(371);  output(373);  requiredLevel = 45; experience = 140.0; failOutput = 375;  failChance = level(45, 86) }
        recipe("Shark")         { input(383);  output(385);  requiredLevel = 80; experience = 210.0; failOutput = 387;  failChance = level(80, 94) }
    }
}
```

---

## 3.6 Action Skill Template

### Data Model
```kotlin
data class ActionDef(
    val name: String,
    val triggerType: TriggerType,         // OBJECT_CLICK, NPC_CLICK, ITEM_CLICK
    val triggerIds: IntArray,
    val triggerOption: Int = 1,
    val requiredLevel: Int,
    val experience: Double,
    val animationId: Int = -1,
    val delayTicks: Int = 1,
    val successChance: Double = 1.0,      // 1.0 = always succeeds
    val failPenalty: FailPenalty? = null,  // Stun, damage, etc.
    val reward: ActionReward? = null,      // Items gained on success
    val successMessage: String = "",
    val failMessage: String = "",
)

enum class TriggerType { OBJECT_CLICK, NPC_CLICK, ITEM_CLICK, ITEM_ON_OBJECT }

data class FailPenalty(
    val stunTicks: Int = 0,
    val damage: Int = 0,
    val message: String = "",
)

data class ActionReward(
    val itemId: Int,
    val quantity: Int = 1,
)
```

### Complete Thieving Example
```kotlin
package net.dodian.uber.game.skill.thieving

object ThievingPlugin : SkillContentPlugin {
    override val pluginName = "Thieving"
    override val skill = Skill.THIEVING

    override val bindings = actionSkill(Skill.THIEVING) {

        // Pickpocketing NPCs
        action("Pickpocket Man") {
            triggerType = NPC_CLICK
            triggerIds(1, 2, 3)         // Man/Woman NPC IDs
            triggerOption = 2           // "Pickpocket" is option 2
            requiredLevel = 1
            experience = 8.0
            animationId = 881
            delayTicks = 2
            successChance = levelScaled(1, 0.5, 99, 0.99)
            failPenalty { stunTicks = 5; damage = 1; message = "You fail to pick the pocket." }
            reward { itemId = 995; quantity = 3 } // 3 gp
        }

        action("Pickpocket Farmer") {
            triggerType = NPC_CLICK
            triggerIds(7, 1757)
            triggerOption = 2
            requiredLevel = 10
            experience = 14.5
            animationId = 881
            delayTicks = 2
            successChance = levelScaled(10, 0.5, 99, 0.99)
            failPenalty { stunTicks = 5; damage = 1 }
            reward { itemId = 5318; quantity = 1 } // Potato seed
        }

        // Stall stealing
        action("Steal from Baker's Stall") {
            triggerType = OBJECT_CLICK
            triggerIds(2561)
            triggerOption = 2           // "Steal-from" option
            requiredLevel = 5
            experience = 16.0
            animationId = 832
            delayTicks = 3
            reward { itemId = 2309; quantity = 1 } // Bread
        }
    }
}
```

---

## 3.7 Skill Runtime Services

Each archetype has a runtime service that executes the action:

```kotlin
// Gathering runtime — handles the gather loop
class GatheringSkillRuntime {
    fun start(player: Client, def: GatheringSkillDef, nodeObjectId: Int, position: Position): Boolean
    fun stop(player: Client, reason: ActionStopReason)
}

// Production runtime — handles make-X interface + production loop
class ProductionSkillRuntime {
    fun start(player: Client, def: ProductionSkillDef, recipeIndex: Int, quantity: Int): Boolean
    fun stop(player: Client, reason: ActionStopReason)
}

// Action runtime — handles one-off actions with success/fail
class ActionSkillRuntime {
    fun start(player: Client, def: ActionSkillDef, actionIndex: Int): Boolean
}
```

These runtimes use `ContentScheduling` internally and should NOT be called by content code directly. The `SkillContentPlugin` bindings + `PluginDispatcher` handle routing.

---

## 3.8 XP & Progression (Unified)

### Single entry point for all XP awards:
```kotlin
package net.dodian.uber.game.api.skill

object SkillXp {
    /**
     * Award experience to a player. Handles:
     * - Server XP rate multiplier
     * - Bonus XP weekends
     * - Level-up detection + fireworks + dialogue
     * - Event bus notification (PlayerLevelUpEvent)
     * - Audit logging
     */
    fun award(player: Client, skill: Skill, baseXp: Double) {
        SkillProgressionService.awardExperience(player, skill, baseXp)
    }

    /** Get the player's current level in a skill */
    fun level(player: Client, skill: Skill): Int = player.getLevel(skill)

    /** Check if the player meets a level requirement */
    fun hasLevel(player: Client, skill: Skill, required: Int): Boolean = level(player, skill) >= required
}
```

Content code uses `SkillXp.award(player, Skill.WOODCUTTING, 25.0)` — nothing else.

---

## 3.9 Migration Steps

### Step 1: Create archetype data models and DSL builders
Files to create:
- `api/skill/template/GatheringSkillDef.kt`
- `api/skill/template/ProductionSkillDef.kt`
- `api/skill/template/ActionSkillDef.kt`
- `api/skill/template/GatheringSkillRuntime.kt`
- `api/skill/template/ProductionSkillRuntime.kt`
- `api/skill/template/ActionSkillRuntime.kt`
- `api/skill/SkillStateContainer.kt`
- `api/skill/SkillXp.kt`

### Step 2: Implement the gathering runtime
Port the existing `gatheringAction()` logic into `GatheringSkillRuntime`, making it data-driven from `GatheringSkillDef`.

### Step 3: Convert Woodcutting and Mining first
These already use `gatheringAction()`, so conversion is mostly reshaping data.

### Step 4: Implement the production runtime
Build the make-X interface integration and production loop.

### Step 5: Convert Cooking and Fletching
Test the production runtime with real content.

### Step 6: Implement the action runtime
Build the one-off action system with success/fail.

### Step 7: Convert Thieving and Prayer
Test the action runtime.

### Step 8: Convert remaining skills one at a time
- Fishing (gathering)
- Crafting (production)
- Smithing (production)
- Herblore (production)
- Runecrafting (production)
- Firemaking (action)
- Agility (action — requires obstacle course abstraction)
- Slayer (passive/complex — custom impl, but still SkillContentPlugin)
- Farming (passive/complex — keep custom runtime, but conform to SkillContentPlugin)

### Step 9: Add SkillStateContainer to Client
Replace individual state properties with the unified container.

### Step 10: Remove old skill infrastructure
- Delete `systems/skills/plugin/SkillPlugin.kt` (old interface)
- Delete `systems/skills/plugin/SkillPluginDsl.kt`
- Delete individual state properties from Client

---

## 3.10 Verification Checklist

- [ ] All 15+ skills implement `SkillContentPlugin`
- [ ] All gathering skills use `GatheringSkillDef`
- [ ] All production skills use `ProductionSkillDef`
- [ ] All action skills use `ActionSkillDef`
- [ ] Complex skills (Farming, Slayer) implement `SkillContentPlugin` with custom bindings
- [ ] `SkillStateContainer` replaces all individual Client state properties
- [ ] XP awards all go through `SkillXp.award()`
- [ ] Skill Doctor validates all skill definitions at startup
- [ ] All skills work correctly in-game (manual testing required)
- [ ] `./gradlew clean build` and `./gradlew :game-server:test` pass

---

## 3.11 Estimated Effort

| Step | Effort |
|------|--------|
| Archetype data models + DSLs | 2–3 hours |
| Gathering runtime | 3–4 hours |
| Production runtime | 4–5 hours |
| Action runtime | 2–3 hours |
| Converting all 15 skills | 8–12 hours |
| SkillStateContainer migration | 2–3 hours |
| Testing + bug fixes | 4–6 hours |
| **Total** | **~30 hours** |

