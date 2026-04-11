# Phase 4: KSP Processor v2

## Goal
Rewrite the KSP processor to support the unified plugin system from Phase 2. Replace fragile heuristics (package path matching, name-based exclusion lists) with **clean interface-based discovery**.

## Prerequisites
- Phase 2 (unified plugin system) complete
- Understanding of KSP APIs (already used in the project)

---

## 4.1 Problems with Current KSP Processor

| Problem | Current Behavior | Impact |
|---------|-----------------|--------|
| **Package-based discovery** | NPC modules discovered by scanning `content.npcs.spawns.*` | Moving packages breaks discovery |
| **Name-based exclusion** | Hardcoded exclude list: `BankerGenerated`, `NpcClickMetrics`, etc. | Must manually maintain, error-prone |
| **Button package coupling** | Buttons discovered only in `content.interfaces.*` | Can't put buttons in feature packages |
| **Separate NPC wiring** | NPC modules use `NpcModuleDefinitionBuilder.fromModule(...)` | Different codegen path than other types |
| **Missing types** | `SkillPlugin`, `ShopPlugin`, `CommandContent` NOT in KSP | Manual wiring still required |
| **No validation** | Duplicate ID registration not checked | Runtime crashes instead of build errors |

---

## 4.2 New Processor Design

### Single Discovery Rule
The new processor scans for **all Kotlin `object` declarations that implement `ContentPlugin` (or any subtype)**. No package path heuristics. No name-based exclusions.

```kotlin
class PluginModuleIndexSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    companion object {
        // The ONLY interface we scan for
        const val CONTENT_PLUGIN_FQN = "net.dodian.uber.game.api.plugin.ContentPlugin"

        // Subtypes we categorize into separate lists
        val PLUGIN_TYPES = mapOf(
            "net.dodian.uber.game.api.plugin.ObjectPlugin" to "objectPlugins",
            "net.dodian.uber.game.api.plugin.NpcPlugin" to "npcPlugins",
            "net.dodian.uber.game.api.plugin.ItemPlugin" to "itemPlugins",
            "net.dodian.uber.game.api.plugin.ButtonPlugin" to "buttonPlugins",
            "net.dodian.uber.game.api.plugin.CommandPlugin" to "commandPlugins",
            "net.dodian.uber.game.api.plugin.SkillContentPlugin" to "skillPlugins",
            "net.dodian.uber.game.api.plugin.ShopContentPlugin" to "shopPlugins",
            "net.dodian.uber.game.api.plugin.BootstrapPlugin" to "bootstrapPlugins",
        )
    }
}
```

### Discovery Algorithm

```
1. Iterate ALL files in the project
2. For each file, find all top-level object declarations
3. For each object, check if it implements ContentPlugin (directly or transitively)
4. If yes:
   a. Determine which specific subtype(s) it implements
   b. Add it to the appropriate category list(s)
   c. An object can appear in MULTIPLE lists (e.g., NpcPlugin + CommandPlugin)
5. Generate the index file with all categorized lists
```

### Validation Rules (Compile-Time Errors)

```kotlin
private fun validate(allPlugins: Map<String, List<DiscoveredPlugin>>) {
    // Rule 1: Must be a Kotlin object (singleton)
    // Already handled by step 2 — we only scan objects

    // Rule 2: No duplicate object IDs
    validateUniqueIds(allPlugins["objectPlugins"], "objectIds", "ObjectPlugin")

    // Rule 3: No duplicate NPC IDs
    validateUniqueIds(allPlugins["npcPlugins"], "npcIds", "NpcPlugin")

    // Rule 4: No duplicate command names
    validateUniqueIds(allPlugins["commandPlugins"], "commands", "CommandPlugin")

    // Rule 5: No duplicate button IDs
    validateUniqueIds(allPlugins["buttonPlugins"], "buttonIds", "ButtonPlugin")

    // Rule 6: No duplicate skill registrations
    validateUniqueIds(allPlugins["skillPlugins"], "skill", "SkillContentPlugin")

    // Rule 7: Warn if a ContentPlugin implements no specific subtype
    for ((_, plugins) in allPlugins) {
        for (plugin in plugins) {
            if (plugin.categories.isEmpty()) {
                logger.warn("${plugin.fqcn} implements ContentPlugin but no specific subtype — it will not be dispatched.")
            }
        }
    }
}
```

If any validation fails, the build fails with a clear error message:
```
error: Duplicate object ID registration: Object ID 2213 is claimed by both
  net.dodian.uber.game.object.bank.BankBoothPlugin and
  net.dodian.uber.game.object.bank.BankChestPlugin
```

---

## 4.3 Generated Output Format

### Current (v2) — Multiple separate lists with different wiring styles:
```kotlin
object GeneratedPluginModuleIndex {
    val interfaceButtons: List<InterfaceButtonContent> = listOf(...)
    val objectContents: List<Pair<String, ObjectContent>> = listOf(...)  // Why Pair<String,...>??
    val itemContents: List<ItemContent> = listOf(...)
    val npcContents: List<NpcContentDefinition> = listOf(
        NpcModuleDefinitionBuilder.fromModule(module = ..., explicitName = "", ownsSpawnDefinitions = false)  // Why??
    )
    val eventBootstraps: List<() -> Unit> = listOf(...)
}
```

### New (v3) — Clean, uniform, type-safe:
```kotlin
package net.dodian.uber.game.plugin

import net.dodian.uber.game.api.plugin.*

/**
 * Auto-generated by KSP at compile time.
 * DO NOT EDIT — this file is regenerated on every build.
 *
 * Generated: 2026-04-11T10:00:00
 * Plugins discovered: 285
 */
object GeneratedPluginModuleIndex {

    @JvmField
    val objectPlugins: List<ObjectPlugin> = listOf(
        net.dodian.uber.game.object.bank.BankBoothPlugin,
        net.dodian.uber.game.object.door.DoorPlugin,
        // ...sorted by FQCN
    )

    @JvmField
    val npcPlugins: List<NpcPlugin> = listOf(
        net.dodian.uber.game.npc.banker.BankerPlugin,
        net.dodian.uber.game.npc.shopkeeper.GerrantPlugin,
        // ...sorted by FQCN
    )

    @JvmField
    val itemPlugins: List<ItemPlugin> = listOf(
        net.dodian.uber.game.item.food.FoodPlugin,
        net.dodian.uber.game.item.potion.PotionPlugin,
        // ...
    )

    @JvmField
    val buttonPlugins: List<ButtonPlugin> = listOf(
        // ...
    )

    @JvmField
    val commandPlugins: List<CommandPlugin> = listOf(
        net.dodian.uber.game.command.admin.TeleportCommand,
        net.dodian.uber.game.command.player.HomeCommand,
        // ...
    )

    @JvmField
    val skillPlugins: List<SkillContentPlugin> = listOf(
        net.dodian.uber.game.skill.woodcutting.WoodcuttingPlugin,
        net.dodian.uber.game.skill.mining.MiningPlugin,
        // ...
    )

    @JvmField
    val shopPlugins: List<ShopContentPlugin> = listOf(
        // ...
    )

    @JvmField
    val bootstrapPlugins: List<BootstrapPlugin> = listOf(
        // ...
    )
}
```

---

## 4.4 Implementation Steps

### Step 1: Create new processor file
Create `ksp-processor/src/main/kotlin/net/dodian/uber/game/plugin/processor/PluginModuleIndexSymbolProcessorV2.kt`

Keep the old processor temporarily until migration is complete.

### Step 2: Implement interface-based scanning

```kotlin
override fun process(resolver: Resolver): List<KSAnnotated> {
    if (generated) return emptyList()

    // Step 1: Find all Kotlin objects
    val allObjects = resolver.getAllFiles().flatMap { file ->
        file.declarations
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.classKind == ClassKind.OBJECT }
    }.toList()

    // Step 2: Filter to ContentPlugin implementors
    val contentPlugins = allObjects.filter { declaration ->
        declaration.implementsInterface(CONTENT_PLUGIN_FQN)
    }

    // Step 3: Categorize by subtype
    val categorized = PLUGIN_TYPES.mapValues { (interfaceFqn, _) ->
        contentPlugins.filter { it.implementsInterface(interfaceFqn) }
            .map { it.toDiscoveredPlugin() }
            .sortedBy { it.fqcn }
    }

    // Step 4: Validate
    validate(categorized)

    // Step 5: Generate
    val output = buildOutput(categorized)
    writeOutput(resolver, output)

    generated = true
    logger.info("Generated PluginModuleIndex: ${contentPlugins.size} total plugins")
    for ((type, plugins) in categorized) {
        if (plugins.isNotEmpty()) {
            logger.info("  $type: ${plugins.size}")
        }
    }

    return emptyList()
}
```

### Step 3: Implement ID uniqueness validation

```kotlin
private fun validateUniqueIds(
    plugins: List<DiscoveredPlugin>?,
    propertyName: String,
    typeName: String,
) {
    if (plugins == null) return

    // For compile-time validation, we'd need to resolve the actual values
    // which requires reading the property from the KSP symbol tree.
    // For IntArray properties, we can read the initializer expression.
    // This is advanced KSP usage but critical for safety.

    // Simplified version: warn about potential duplicates
    // Full version: resolve initializer expressions and check values
}
```

**Note**: Full ID uniqueness validation at compile time requires reading property initializer values from KSP, which is complex but doable for `IntArray` literals. A simpler approach is to do this check at runtime during `PluginRegistry.initialize()` and fail-fast on startup.

### Step 4: Switch processor provider

```kotlin
class PluginModuleIndexSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return PluginModuleIndexSymbolProcessorV2(  // <-- New processor
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
        )
    }
}
```

### Step 5: Update build.gradle.kts if needed
The KSP dependency should already be configured. Just verify:
```kotlin
dependencies {
    ksp(project(":ksp-processor"))
}
```

### Step 6: Delete old processor
Remove `PluginModuleIndexSymbolProcessor.kt` (v1) after verifying v2 works.

---

## 4.5 Multi-Interface Support

A plugin that implements both `NpcPlugin` and `CommandPlugin` will appear in BOTH generated lists:

```kotlin
object TannerPlugin : NpcPlugin, CommandPlugin {
    override val pluginName = "Tanner"
    override val npcIds = intArrayOf(804)
    override val commands = arrayOf("tan")
    // ...
}
```

Generated output:
```kotlin
val npcPlugins: List<NpcPlugin> = listOf(
    net.dodian.uber.game.npc.utility.TannerPlugin,
    // ...
)

val commandPlugins: List<CommandPlugin> = listOf(
    net.dodian.uber.game.npc.utility.TannerPlugin,  // Same object, different list
    // ...
)
```

`PluginRegistry` handles this naturally — it builds separate lookup maps, and the same object reference is in both maps.

---

## 4.6 Startup Validation (Runtime)

Even with compile-time KSP validation, add a runtime check during `PluginRegistry.initialize()`:

```kotlin
fun initialize(index: GeneratedPluginModuleIndex) {
    // Build object plugin map with duplicate detection
    val objectMap = HashMap<Int, ObjectPlugin>()
    for (plugin in index.objectPlugins) {
        for (id in plugin.objectIds) {
            val existing = objectMap.put(id, plugin)
            if (existing != null) {
                throw IllegalStateException(
                    "Duplicate ObjectPlugin registration for object ID $id: " +
                    "${existing.pluginName} and ${plugin.pluginName}"
                )
            }
        }
    }
    objectPlugins = objectMap

    // ... similar for all types

    logger.info("PluginRegistry initialized: ${allPlugins().size} plugins")
    logger.info("  Objects: ${objectPlugins.size} IDs across ${index.objectPlugins.size} plugins")
    logger.info("  NPCs: ${npcPlugins.size} IDs across ${index.npcPlugins.size} plugins")
    logger.info("  Items: ${itemPlugins.size} IDs across ${index.itemPlugins.size} plugins")
    logger.info("  Buttons: ${buttonPlugins.size} IDs across ${index.buttonPlugins.size} plugins")
    logger.info("  Commands: ${commandPlugins.size} commands")
    logger.info("  Skills: ${skillPlugins.size} skills")
    logger.info("  Shops: ${shopPlugins.size} shops")
    logger.info("  Bootstraps: ${bootstrapPlugins.size} bootstraps")
}
```

---

## 4.7 Verification Checklist

- [ ] Old KSP processor removed
- [ ] New processor discovers all `ContentPlugin` subtypes by interface, not package path
- [ ] No hardcoded exclude lists in the processor
- [ ] Duplicate ID registration produces a compile error (or fast startup failure)
- [ ] Multi-interface plugins appear in all relevant lists
- [ ] Generated index is sorted by FQCN for stable diffs
- [ ] Build log shows plugin counts by type
- [ ] `./gradlew clean build` passes
- [ ] `./gradlew :game-server:test` passes
- [ ] Server starts successfully with correct plugin count

---

## 4.8 Estimated Effort

| Step | Effort |
|------|--------|
| Write new processor | 3–4 hours |
| Implement validation | 2 hours |
| Test with full project | 2 hours |
| Delete old processor | 30 min |
| Update architecture tests | 1 hour |
| **Total** | **~10 hours** |

