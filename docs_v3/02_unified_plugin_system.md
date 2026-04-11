# Phase 2: Unified Plugin System

## Goal
Replace the current 6+ disparate content registration interfaces with a **single, cohesive plugin system** that uses one base contract, one discovery mechanism, and one dispatch pipeline.

## Prerequisites
- Phase 1 (package restructure) complete
- All builds and tests passing

---

## 2.1 The Problem with the Current System

Today, the server has these separate content registration interfaces:

| Interface | How it's discovered | How it's dispatched |
|-----------|-------------------|-------------------|
| `ObjectContent` | KSP by interface match | `ObjectContentRegistry` → `InteractionProcessor` |
| `NpcModule` | KSP by package path + exclude list | `NpcContentRegistry` → `NpcInteractionActionService` |
| `ItemContent` | KSP by interface match | `ItemContentRegistry` |
| `InterfaceButtonContent` | KSP by package + interface | `ClickingButtonsListener` |
| `SkillPlugin` | Manual wiring through `ContentModuleIndex` | `SkillPluginRouteBridge` → `SkillInteractionDispatcher` |
| `ShopPlugin` | Manual wiring | `ShopManager` |
| `CommandContent` | Referenced in `ContentModuleIndex` | `CommandProcessor` |
| `ContentBootstrap` | KSP by interface match | Called during startup |

**Problems**:
1. 8 different discovery/dispatch mechanisms
2. Adding a new content type requires modifying KSP processor, adding a new registry, and wiring dispatch
3. Content developers must know which interface to implement for their use case
4. No way to compose behaviors (e.g., an NPC that is also a shopkeeper with dialogue)

---

## 2.2 Design: The Unified `ContentPlugin` System

### Core Marker Interface

```kotlin
package net.dodian.uber.game.api.plugin

/**
 * Base marker interface for all content plugins.
 * Every piece of game content is a ContentPlugin that KSP discovers at compile time.
 *
 * Content plugins are Kotlin `object` singletons. They are stateless definitions
 * that describe what they handle and how. Runtime state belongs on the Player/NPC/World.
 */
interface ContentPlugin {
    /**
     * Human-readable name for logging and debugging.
     * Example: "Woodcutting", "BankBooth", "Gerrant", "Dragon Dagger Special"
     */
    val pluginName: String
}
```

### Specialized Plugin Interfaces

Each content type extends `ContentPlugin` with its specific contract:

```kotlin
// --- Object Interactions ---
interface ObjectPlugin : ContentPlugin {
    /** Object IDs this plugin handles */
    val objectIds: IntArray

    fun onFirstClick(player: Client, objectId: Int, position: Position, data: GameObjectData?): Boolean = false
    fun onSecondClick(player: Client, objectId: Int, position: Position, data: GameObjectData?): Boolean = false
    fun onThirdClick(player: Client, objectId: Int, position: Position, data: GameObjectData?): Boolean = false
    fun onItemOnObject(player: Client, objectId: Int, position: Position, data: GameObjectData?,
                       itemId: Int, slot: Int): Boolean = false
}

// --- NPC Interactions ---
interface NpcPlugin : ContentPlugin {
    /** NPC IDs this plugin handles */
    val npcIds: IntArray

    /** Optional spawn definitions owned by this plugin */
    val spawns: List<NpcSpawnDef> get() = emptyList()

    fun onFirstClick(player: Client, npc: Npc): Boolean = false
    fun onSecondClick(player: Client, npc: Npc): Boolean = false
    fun onThirdClick(player: Client, npc: Npc): Boolean = false
    fun onFourthClick(player: Client, npc: Npc): Boolean = false
    fun onAttack(player: Client, npc: Npc): Boolean = false
}

// --- Item Interactions ---
interface ItemPlugin : ContentPlugin {
    /** Item IDs this plugin handles */
    val itemIds: IntArray

    fun onFirstClick(player: Client, itemId: Int, slot: Int): Boolean = false
    fun onSecondClick(player: Client, itemId: Int, slot: Int): Boolean = false
    fun onThirdClick(player: Client, itemId: Int, slot: Int): Boolean = false
    fun onItemOnItem(player: Client, itemUsed: Int, otherItem: Int): Boolean = false
}

// --- Interface Buttons ---
interface ButtonPlugin : ContentPlugin {
    /** Raw button IDs this plugin handles */
    val buttonIds: IntArray

    fun onClick(player: Client, buttonId: Int): Boolean = false
}

// --- Commands ---
interface CommandPlugin : ContentPlugin {
    /** Command names (without ::) this plugin handles */
    val commands: Array<String>

    /** Required permission level (0 = player, 1 = mod, 2 = admin, 3 = dev) */
    val requiredPermission: Int get() = 0

    fun execute(player: Client, command: String, args: Array<String>): Boolean = false
}

// --- Skills (extends ObjectPlugin for tree/rock clicks) ---
interface SkillContentPlugin : ContentPlugin {
    /** The skill this plugin provides */
    val skill: Skill

    /** All interaction bindings for this skill */
    val bindings: SkillBindings
}

// --- Shops ---
interface ShopContentPlugin : ContentPlugin {
    /** Shop definition */
    val shop: ShopDefinition
}

// --- Startup Bootstrap ---
interface BootstrapPlugin : ContentPlugin {
    /** Called once during server startup after all plugins are indexed */
    fun bootstrap()
}
```

### Key Design Decisions

1. **All interfaces extend `ContentPlugin`** — KSP only needs to scan for `ContentPlugin` subtypes
2. **All plugins are `object` singletons** — enforced by KSP at compile time
3. **Default implementations on all handler methods** — only override what you need
4. **IDs are declared on the interface** — enables pre-indexed lookup tables
5. **No manual registration** — KSP generates the index automatically

---

## 2.3 The Unified Plugin Registry

Replace all existing registries with a single `PluginRegistry`:

```kotlin
package net.dodian.uber.game.api.plugin

object PluginRegistry {
    // Populated by GeneratedPluginModuleIndex at startup
    private lateinit var objectPlugins: Map<Int, ObjectPlugin>       // objectId → plugin
    private lateinit var npcPlugins: Map<Int, NpcPlugin>             // npcId → plugin
    private lateinit var itemPlugins: Map<Int, ItemPlugin>           // itemId → plugin
    private lateinit var buttonPlugins: Map<Int, ButtonPlugin>       // buttonId → plugin
    private lateinit var commandPlugins: Map<String, CommandPlugin>  // command → plugin
    private lateinit var skillPlugins: Map<Skill, SkillContentPlugin>
    private lateinit var shopPlugins: List<ShopContentPlugin>
    private lateinit var bootstrapPlugins: List<BootstrapPlugin>

    fun initialize(index: GeneratedPluginModuleIndex) {
        // Build lookup maps from the generated lists
        objectPlugins = index.objectPlugins.flatMap { plugin ->
            plugin.objectIds.map { id -> id to plugin }
        }.toMap()

        npcPlugins = index.npcPlugins.flatMap { plugin ->
            plugin.npcIds.map { id -> id to plugin }
        }.toMap()

        // ... etc for each type
    }

    // --- Lookup API ---
    fun objectPlugin(objectId: Int): ObjectPlugin? = objectPlugins[objectId]
    fun npcPlugin(npcId: Int): NpcPlugin? = npcPlugins[npcId]
    fun itemPlugin(itemId: Int): ItemPlugin? = itemPlugins[itemId]
    fun buttonPlugin(buttonId: Int): ButtonPlugin? = buttonPlugins[buttonId]
    fun commandPlugin(command: String): CommandPlugin? = commandPlugins[command.lowercase()]
    fun skillPlugin(skill: Skill): SkillContentPlugin? = skillPlugins[skill]
    fun allShops(): List<ShopContentPlugin> = shopPlugins
    fun allBootstraps(): List<BootstrapPlugin> = bootstrapPlugins

    // --- Debug/Admin API ---
    fun allPlugins(): List<ContentPlugin> {
        return listOf(
            objectPlugins.values.toSet(),
            npcPlugins.values.toSet(),
            itemPlugins.values.toSet(),
            buttonPlugins.values.toSet(),
            commandPlugins.values.toSet(),
            skillPlugins.values.toSet(),
            shopPlugins.toSet(),
            bootstrapPlugins.toSet(),
        ).flatten()
    }

    fun pluginCount(): Int = allPlugins().size
}
```

---

## 2.4 The Unified Dispatch Pipeline

Replace all the separate dispatchers with a single pipeline:

```kotlin
package net.dodian.uber.game.api.plugin

object PluginDispatcher {

    fun dispatchObjectClick(player: Client, option: Int, objectId: Int,
                            position: Position, data: GameObjectData?): Boolean {
        val plugin = PluginRegistry.objectPlugin(objectId) ?: return false
        return when (option) {
            1 -> plugin.onFirstClick(player, objectId, position, data)
            2 -> plugin.onSecondClick(player, objectId, position, data)
            3 -> plugin.onThirdClick(player, objectId, position, data)
            else -> false
        }
    }

    fun dispatchNpcClick(player: Client, option: Int, npc: Npc): Boolean {
        val plugin = PluginRegistry.npcPlugin(npc.npcId) ?: return false
        return when (option) {
            1 -> plugin.onFirstClick(player, npc)
            2 -> plugin.onSecondClick(player, npc)
            3 -> plugin.onThirdClick(player, npc)
            4 -> plugin.onFourthClick(player, npc)
            5 -> plugin.onAttack(player, npc)
            else -> false
        }
    }

    fun dispatchItemClick(player: Client, option: Int, itemId: Int, slot: Int): Boolean {
        val plugin = PluginRegistry.itemPlugin(itemId) ?: return false
        return when (option) {
            1 -> plugin.onFirstClick(player, itemId, slot)
            2 -> plugin.onSecondClick(player, itemId, slot)
            3 -> plugin.onThirdClick(player, itemId, slot)
            else -> false
        }
    }

    fun dispatchCommand(player: Client, command: String, args: Array<String>): Boolean {
        val plugin = PluginRegistry.commandPlugin(command) ?: return false
        if (player.permissionLevel < plugin.requiredPermission) {
            player.sendMessage("You don't have permission to use this command.")
            return true
        }
        return plugin.execute(player, command, args)
    }

    fun dispatchButton(player: Client, buttonId: Int): Boolean {
        val plugin = PluginRegistry.buttonPlugin(buttonId) ?: return false
        return plugin.onClick(player, buttonId)
    }
}
```

---

## 2.5 Migration from Old Interfaces

### Step 1: Create the new interfaces
Create all files in `api/plugin/`:
- `ContentPlugin.kt`
- `ObjectPlugin.kt`
- `NpcPlugin.kt`
- `ItemPlugin.kt`
- `ButtonPlugin.kt`
- `CommandPlugin.kt`
- `SkillContentPlugin.kt`
- `ShopContentPlugin.kt`
- `BootstrapPlugin.kt`
- `PluginRegistry.kt`
- `PluginDispatcher.kt`

### Step 2: Add compatibility layer
Make the old interfaces extend the new ones temporarily:
```kotlin
// In object/_shared/ObjectContent.kt
interface ObjectContent : ObjectPlugin {
    // Bridge old API to new API
    override val pluginName: String get() = this::class.simpleName ?: "UnknownObject"
    // ... bridge methods
}
```

### Step 3: Migrate content one type at a time
1. **Objects first** (simplest, ~30 plugins)
2. **Commands** (~20 plugins)
3. **Items** (~15 plugins)
4. **Buttons** (~10 plugins)
5. **NPCs** (~200 plugins — can be batch-scripted)
6. **Skills** (~15 plugins — most complex, do carefully)
7. **Shops** (~5 plugins)

For each plugin:
1. Change `implements ObjectContent` → `implements ObjectPlugin`
2. Add `override val pluginName = "..."`
3. Ensure handler signatures match
4. Run tests

### Step 4: Remove old interfaces
Once all plugins use the new interfaces:
1. Delete `content/objects/ObjectContent.kt` (old)
2. Delete `content/npcs/NpcContent.kt` (old)
3. Delete `content/items/ItemContent.kt` (old)
4. Delete `systems/ui/buttons/InterfaceButtonContent.kt` (old)
5. Delete all old registry classes
6. Delete all old dispatcher classes

### Step 5: Update packet listeners
Update the packet listener classes to call `PluginDispatcher` instead of the old registries.

---

## 2.6 What a Plugin Looks Like After Migration

### Before (Current — Object)
```kotlin
package net.dodian.uber.game.content.objects.banking

object BankBoothContent : ObjectContent {
    override val objectIds = intArrayOf(2213, 3045, 5276)

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        client.openBank()
        return true
    }
}
```

### After (v3 — Object)
```kotlin
package net.dodian.uber.game.object.bank

object BankBoothPlugin : ObjectPlugin {
    override val pluginName = "Bank Booth"
    override val objectIds = intArrayOf(2213, 3045, 5276)

    override fun onFirstClick(player: Client, objectId: Int, position: Position, data: GameObjectData?): Boolean {
        player.openBank()
        return true
    }
}
```

### Before (Current — NPC with DSL)
```kotlin
package net.dodian.uber.game.content.npcs

object Gerrant : NpcModule {
    override val definition = NpcContentDefinition(
        name = "Gerrant",
        npcIds = intArrayOf(558),
        onFirstClick = { client, npc -> /* ... */ true },
        onSecondClick = { client, npc -> /* ... */ true },
    )
}
```

### After (v3 — NPC)
```kotlin
package net.dodian.uber.game.npc.shopkeeper

object Gerrant : NpcPlugin {
    override val pluginName = "Gerrant"
    override val npcIds = intArrayOf(558)

    override fun onFirstClick(player: Client, npc: Npc): Boolean {
        player.dialogue {
            npc(558, "Welcome to my fishing shop!", "Would you like to see my wares?")
        }
        return true
    }

    override fun onSecondClick(player: Client, npc: Npc): Boolean {
        ShopManager.open(player, ShopId.GERRANT_FISHING)
        return true
    }
}
```

---

## 2.7 Composite Plugins

Sometimes content spans multiple interaction types. The unified system supports this via multiple interface implementation:

```kotlin
package net.dodian.uber.game.npc.utility

/**
 * Tanner: Talk to him (NPC click) or use a cowhide on him (item-on-NPC).
 * He also has a ::tan command for admins.
 */
object TannerPlugin : NpcPlugin, CommandPlugin {
    override val pluginName = "Tanner"
    override val npcIds = intArrayOf(804)
    override val commands = arrayOf("tan")
    override val requiredPermission = 2 // Admin only

    override fun onFirstClick(player: Client, npc: Npc): Boolean {
        player.dialogue {
            npc(804, "I can tan your hides for you. Just bring me some cowhides!")
        }
        return true
    }

    override fun execute(player: Client, command: String, args: Array<String>): Boolean {
        // Admin: tan all hides in inventory instantly
        tanAllHides(player)
        return true
    }
}
```

---

## 2.8 Verification Checklist

- [ ] All old interface types removed
- [ ] All content plugins implement the appropriate `*Plugin` interface extending `ContentPlugin`
- [ ] `PluginRegistry` initializes successfully from `GeneratedPluginModuleIndex`
- [ ] Every object click, NPC click, item click, button click, and command routes through `PluginDispatcher`
- [ ] Legacy fallback paths still work for un-migrated Java content
- [ ] `::plugins` admin command shows total plugin count
- [ ] Architecture tests updated to enforce new boundaries
- [ ] `./gradlew clean build` and `./gradlew :game-server:test` pass

---

## 2.9 Architecture Test Additions

```kotlin
@Test
fun `all content plugins extend ContentPlugin`() {
    // Scan all Kotlin objects in content packages
    // Verify they implement ContentPlugin or a subtype
}

@Test
fun `no duplicate ID registrations`() {
    // Verify no two ObjectPlugins claim the same object ID
    // Verify no two NpcPlugins claim the same NPC ID
    // Verify no two CommandPlugins claim the same command name
}

@Test
fun `old content interfaces are not used`() {
    // Scan for imports of old ObjectContent, NpcModule, ItemContent, etc.
    // Fail if found
}
```

