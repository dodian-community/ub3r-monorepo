# Plugin Registry + Shop DSL Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Rename `SkillPluginRegistry` to a generic `PluginRegistry`, then make shops modular/autoregistered via plugins + DSL so adding new shop content is fast and low-risk.

**Architecture:** Keep the current generated module index pattern (KSP) and extend it to discover shop plugins. Introduce a generalized plugin registry shape with immutable snapshots and strict collision validation, then migrate existing monolithic `ShopDefinitions` into per-shop plugin modules. Preserve compatibility with legacy `ShopManager` arrays so existing Java packet/shop paths keep working during rollout.

**Tech Stack:** Kotlin, Java interop (legacy `Client.java`), KSP symbol processor, JUnit 5, Gradle.

---

### Task 1: Add Regression Tests Before Refactor

**Files:**
- Modify: `game-server/src/test/kotlin/net/dodian/uber/game/systems/skills/SkillPluginRegistryTest.kt`
- Create: `game-server/src/test/kotlin/net/dodian/uber/game/content/shop/ShopPluginRegistryTest.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/content/shop/ShopDefinitionsTest.kt`

**Step 1: Write the failing tests**

```kotlin
@Test
fun `plugin registry alias resolves skill bindings`() {
    net.dodian.uber.game.systems.plugin.PluginRegistry.resetForTests()
    net.dodian.uber.game.systems.plugin.PluginRegistry.bootstrap()
    val snapshot = net.dodian.uber.game.systems.plugin.PluginRegistry.currentSkills()

    assertNotNull(snapshot.objectBinding(option = 1, objectId = 7451))
}
```

```kotlin
@Test
fun `shop registry rejects duplicate shop ids`() {
    ShopPluginRegistry.resetForTests()
    ShopPluginRegistry.register(shopPlugin("A", shopId = 99) { item(590, 1) })
    ShopPluginRegistry.register(shopPlugin("B", shopId = 99) { item(591, 1) })

    assertThrows(IllegalArgumentException::class.java) {
        ShopPluginRegistry.bootstrap()
    }
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew :game-server:test --tests "*SkillPluginRegistryTest" --tests "*ShopPluginRegistryTest" -i`
Expected: FAIL because `PluginRegistry`/`ShopPluginRegistry` do not exist yet.

**Step 3: Make minimal test scaffolding changes**

Create empty registry objects and minimal APIs used by tests (temporary TODO stubs).

```kotlin
object PluginRegistry {
    fun bootstrap() = Unit
    fun resetForTests() = Unit
    fun currentSkills(): SkillPluginSnapshot = SkillPluginSnapshot.empty()
}
```

**Step 4: Run tests to verify failure is now behavior-level, not missing symbol**

Run: `./gradlew :game-server:test --tests "*SkillPluginRegistryTest" --tests "*ShopPluginRegistryTest" -i`
Expected: FAIL on assertions, no compile errors.

**Step 5: Commit**

```bash
git add game-server/src/test/kotlin/net/dodian/uber/game/systems/skills/SkillPluginRegistryTest.kt game-server/src/test/kotlin/net/dodian/uber/game/content/shop/ShopPluginRegistryTest.kt
git commit -m "test: add registry migration safety nets"
```

### Task 2: Introduce Generic PluginRegistry (Rename + Compatibility)

**Files:**
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/systems/plugin/PluginRegistry.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/systems/skills/plugin/SkillPluginRegistry.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/systems/dispatch/ContentModuleIndex.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/systems/skills/SkillPluginRegistryTest.kt`

**Step 1: Write the failing test for compatibility alias**

```kotlin
@Test
fun `legacy SkillPluginRegistry forwards to PluginRegistry`() {
    SkillPluginRegistry.resetForTests()
    SkillPluginRegistry.bootstrap()
    assertNotNull(SkillPluginRegistry.current().itemBinding(option = 1, itemId = 4155))
}
```

**Step 2: Run test to verify it fails against TODO stubs**

Run: `./gradlew :game-server:test --tests "*SkillPluginRegistryTest" -i`
Expected: FAIL on null/missing routing.

**Step 3: Implement minimal production code**

```kotlin
package net.dodian.uber.game.systems.plugin

object PluginRegistry : ContentBootstrap {
    override val id: String = "plugins.registry"

    private val skills = SkillPluginRegistryEngine()

    override fun bootstrap() {
        skills.bootstrap(ContentModuleIndex.skillPlugins)
    }

    fun currentSkills(): SkillPluginSnapshot = skills.current()
    fun registerSkill(plugin: SkillPlugin) = skills.register(plugin)

    internal fun resetForTests() = skills.resetForTests()
}
```

And keep compatibility façade:

```kotlin
@Deprecated("Use net.dodian.uber.game.systems.plugin.PluginRegistry")
object SkillPluginRegistry : ContentBootstrap {
    override val id: String = "skills.registry"
    override fun bootstrap() = PluginRegistry.bootstrap()
    fun register(plugin: SkillPlugin) = PluginRegistry.registerSkill(plugin)
    fun current(): SkillPluginSnapshot = PluginRegistry.currentSkills()
    internal fun resetForTests() = PluginRegistry.resetForTests()
}
```

**Step 4: Run tests to verify pass**

Run: `./gradlew :game-server:test --tests "*SkillPluginRegistryTest" -i`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/systems/plugin/PluginRegistry.kt game-server/src/main/kotlin/net/dodian/uber/game/systems/skills/plugin/SkillPluginRegistry.kt game-server/src/main/kotlin/net/dodian/uber/game/systems/dispatch/ContentModuleIndex.kt game-server/src/test/kotlin/net/dodian/uber/game/systems/skills/SkillPluginRegistryTest.kt
git commit -m "refactor: introduce PluginRegistry with SkillPluginRegistry compatibility"
```

### Task 3: Migrate Call Sites to PluginRegistry (No Behavior Change)

**Files:**
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/systems/skills/SkillInteractionDispatcher.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/systems/interaction/InteractionProcessor.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/systems/skills/parity/SkillDoctor.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/systems/skills/parity/ContentParityDoctor.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/systems/skills/SkillPluginRouteBridgeTest.kt`

**Step 1: Write a failing route bridge test against new registry accessor**

```kotlin
@Test
fun `dispatcher resolves via PluginRegistry`() {
    PluginRegistry.resetForTests()
    PluginRegistry.bootstrap()
    val binding = PluginRegistry.currentSkills().objectBinding(option = 1, objectId = 1276)
    assertNotNull(binding)
}
```

**Step 2: Run test to verify initial failure**

Run: `./gradlew :game-server:test --tests "*SkillPluginRouteBridgeTest" -i`
Expected: FAIL while callers still target old object paths.

**Step 3: Implement minimal code change**

Replace usage pattern:

```kotlin
val binding = PluginRegistry.currentSkills().objectBinding(option, objectId)
```

**Step 4: Run tests**

Run: `./gradlew :game-server:test --tests "*SkillPluginRouteBridgeTest" --tests "*SkillDoctorTest" -i`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/systems/skills/SkillInteractionDispatcher.kt game-server/src/main/kotlin/net/dodian/uber/game/systems/interaction/InteractionProcessor.kt game-server/src/main/kotlin/net/dodian/uber/game/systems/skills/parity/SkillDoctor.kt game-server/src/main/kotlin/net/dodian/uber/game/systems/skills/parity/ContentParityDoctor.kt
git commit -m "refactor: route skill binding lookups through PluginRegistry"
```

### Task 4: Build Shop Plugin Model + Registry + DSL

**Files:**
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugin/ShopPlugin.kt`
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugin/ShopPluginDsl.kt`
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugin/ShopPluginRegistry.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/content/shop/ShopDefinitions.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/content/shop/ShopPluginRegistryTest.kt`

**Step 1: Write failing DSL + registry tests**

```kotlin
@Test
fun `shop plugin DSL builds immutable definition`() {
    val def = shopPlugin("General", shopId = 3) {
        buyModifier = 1
        sellModifier = 1
        item(590, 2)
    }.definition

    assertEquals(3, def.id)
    assertEquals(1, def.buyModifier)
    assertEquals(590, def.stock.first().itemId)
}
```

**Step 2: Run tests to verify fail**

Run: `./gradlew :game-server:test --tests "*ShopPluginRegistryTest" -i`
Expected: FAIL because DSL/registry are missing.

**Step 3: Implement minimal production code**

```kotlin
interface ShopPlugin { val definition: ShopDefinition }

fun shopPlugin(name: String, shopId: Int, block: ShopPluginBuilder.() -> Unit): ShopPlugin =
    object : ShopPlugin {
        override val definition = ShopPluginBuilder(name, shopId).apply(block).build()
    }
```

```kotlin
object ShopPluginRegistry : ContentBootstrap {
    override val id: String = "shops.registry"
    private val definitions = mutableListOf<ShopDefinition>()
    @Volatile private var snapshot: Map<Int, ShopDefinition> = emptyMap()

    override fun bootstrap() {
        if (snapshot.isNotEmpty()) return
        synchronized(this) {
            if (snapshot.isNotEmpty()) return
            definitions += ContentModuleIndex.shopPlugins.map { it.definition }
            rebuildLocked()
        }
    }

    fun all(): List<ShopDefinition> = snapshot.values.sortedBy { it.id }
    fun find(id: Int): ShopDefinition? = snapshot[id]
}
```

Then make `ShopDefinitions` delegate:

```kotlin
object ShopDefinitions {
    @JvmStatic fun all(): List<ShopDefinition> = ShopPluginRegistry.all()
    @JvmStatic fun find(id: Int): ShopDefinition? = ShopPluginRegistry.find(id)
}
```

**Step 4: Run tests**

Run: `./gradlew :game-server:test --tests "*ShopPluginRegistryTest" --tests "*ShopDefinitionsTest" -i`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugin/ShopPlugin.kt game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugin/ShopPluginDsl.kt game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugin/ShopPluginRegistry.kt game-server/src/main/kotlin/net/dodian/uber/game/content/shop/ShopDefinitions.kt game-server/src/test/kotlin/net/dodian/uber/game/content/shop/ShopPluginRegistryTest.kt
git commit -m "feat: add ShopPlugin DSL and registry"
```

### Task 5: Extend KSP Auto-Registration for Shop Plugins

**Files:**
- Modify: `ksp-processor/src/main/kotlin/net/dodian/uber/game/plugin/processor/PluginModuleIndexSymbolProcessor.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/systems/dispatch/ContentModuleIndex.kt`
- (Generated): `game-server/build/generated/ksp/**/GeneratedPluginModuleIndex.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/content/shop/ShopPluginRegistryTest.kt`

**Step 1: Write failing test that expects autodiscovered shop plugins**

```kotlin
@Test
fun `shop registry bootstraps from generated module index`() {
    ShopPluginRegistry.resetForTests()
    ShopPluginRegistry.bootstrap()
    assertTrue(ShopPluginRegistry.all().isNotEmpty())
}
```

**Step 2: Run test to verify fail**

Run: `./gradlew :game-server:test --tests "*ShopPluginRegistryTest" -i`
Expected: FAIL because `shopPlugins` are not in generated index.

**Step 3: Implement minimal KSP changes**

Add discovery in processor:

```kotlin
private fun discoverShopPlugins(allObjects: List<Pair<KSFile, KSClassDeclaration>>): List<DiscoveredSymbol> {
    val shopPluginType = "net.dodian.uber.game.content.shop.plugin.ShopPlugin"
    return allObjects
        .filter { (_, declaration) -> declaration.implementsInterface(shopPluginType) }
        .map { (_, declaration) -> declaration.toDiscoveredSymbol() }
        .sortedBy { it.fqcn }
}
```

Add generated output field:

```kotlin
@JvmField
val shopPlugins: List<ShopPlugin> = listOf(
    // generated entries
)
```

Expose from `ContentModuleIndex`:

```kotlin
@JvmField
val shopPlugins = GeneratedPluginModuleIndex.shopPlugins
```

**Step 4: Run tests**

Run: `./gradlew :game-server:test --tests "*ShopPluginRegistryTest" -i`
Expected: PASS with autodiscovery.

**Step 5: Commit**

```bash
git add ksp-processor/src/main/kotlin/net/dodian/uber/game/plugin/processor/PluginModuleIndexSymbolProcessor.kt game-server/src/main/kotlin/net/dodian/uber/game/systems/dispatch/ContentModuleIndex.kt
git commit -m "feat: auto-discover shop plugins in generated module index"
```

### Task 6: Split Monolithic ShopDefinitions into Per-Shop Plugins

**Files:**
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugins/GeneralStoreShop.kt`
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugins/AuburysMagicStoreShop.kt`
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugins/WeaponAndArmorShop.kt`
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugins/RangeStoreShop.kt`
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugins/SlayerShop.kt`
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugins/FishingStoreShop.kt`
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugins/PremiumMemberShop.kt`
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugins/CraftingStoreShop.kt`
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugins/CapeShop.kt`
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugins/JatixHerbloreStoreShop.kt`
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugins/FishingSuppliesShop.kt`
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugins/ChristmasEventStoreShop.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/content/shop/ShopDefinitions.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/content/shop/ShopDefinitionsTest.kt`

**Step 1: Write failing parity test (old vs plugin)**

```kotlin
@Test
fun `shop plugins preserve migrated metadata`() {
    val defs = ShopDefinitions.all()
    assertEquals(12, defs.size)
    assertEquals("General Store", ShopDefinitions.find(3)?.name)
    assertTrue(ShopDefinitions.find(20)?.requiresPremium == true)
}
```

**Step 2: Run test to verify fail after deleting inlined list**

Run: `./gradlew :game-server:test --tests "*ShopDefinitionsTest" -i`
Expected: FAIL until all per-shop plugin objects are added.

**Step 3: Implement one shop plugin pattern, then copy for others**

```kotlin
package net.dodian.uber.game.content.shop.plugins

import net.dodian.uber.game.content.shop.plugin.ShopPlugin
import net.dodian.uber.game.content.shop.plugin.shopPlugin

object GeneralStoreShop : ShopPlugin {
    override val definition = shopPlugin(name = "General Store", shopId = 3) {
        sellModifier = 1
        buyModifier = 1
        item(590, 2)
        item(2347, 5)
        item(946, 5)
        item(1351, 10)
        item(1265, 10)
        item(1755, 5)
        item(4155, 10)
        item(1735, 10)
        item(1925, 1337)
    }.definition
}
```

**Step 4: Run tests**

Run: `./gradlew :game-server:test --tests "*ShopDefinitionsTest" --tests "*ShopPluginRegistryTest" -i`
Expected: PASS with identical shop count/metadata.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugins game-server/src/main/kotlin/net/dodian/uber/game/content/shop/ShopDefinitions.kt game-server/src/test/kotlin/net/dodian/uber/game/content/shop/ShopDefinitionsTest.kt
git commit -m "refactor: split shop catalog into per-shop plugin modules"
```

### Task 7: Harden Runtime Validation + Tick-Safe Shop Processing

**Files:**
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugin/ShopPluginRegistry.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/content/shop/ShopManager.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/engine/processing/ShopProcessor.kt`
- Modify: `game-server/src/main/java/net/dodian/uber/game/model/entity/player/Client.java`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/content/shop/ShopPluginRegistryTest.kt`

**Step 1: Write failing validation tests**

```kotlin
@Test
fun `shop registry rejects invalid ids and slot price mismatches`() {
    ShopPluginRegistry.resetForTests()
    ShopPluginRegistry.register(shopPlugin("Broken", shopId = -1) { item(590, 1) })

    assertThrows(IllegalArgumentException::class.java) { ShopPluginRegistry.bootstrap() }
}
```

**Step 2: Run test to verify fail**

Run: `./gradlew :game-server:test --tests "*ShopPluginRegistryTest" -i`
Expected: FAIL until registry validation added.

**Step 3: Implement minimal hardening code**

Validation in registry bootstrap:

```kotlin
require(definition.id in 0 until ShopManager.MaxShops) { "Shop id out of range: ${definition.id}" }
require(definition.stock.size <= ShopManager.MaxShopItems) { "Shop ${definition.id} exceeds max items" }
require(definition.slotBuyPriceOverrides.keys.all { it in definition.stock.indices }) {
    "Shop ${definition.id} has slot override outside stock range"
}
```

Safe guard in `Client.openUpShop` before array access:

```java
if (ShopID < 0 || ShopID >= ShopManager.MaxShops || ShopDefinitions.find(ShopID) == null) {
    send(new SendMessage("This shop is currently unavailable."));
    return;
}
```

Tighten `ShopProcessor` loop bounds (no out-of-range pass):

```kotlin
for (shopId in 0 until ShopManager.MaxShops) {
    // existing restock logic
}
```

**Step 4: Run tests**

Run: `./gradlew :game-server:test --tests "*ShopPluginRegistryTest" --tests "*ShopDefinitionsTest" -i`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugin/ShopPluginRegistry.kt game-server/src/main/kotlin/net/dodian/uber/game/content/shop/ShopManager.kt game-server/src/main/kotlin/net/dodian/uber/game/engine/processing/ShopProcessor.kt game-server/src/main/java/net/dodian/uber/game/model/entity/player/Client.java game-server/src/test/kotlin/net/dodian/uber/game/content/shop/ShopPluginRegistryTest.kt
git commit -m "fix: harden shop registry and runtime safety checks"
```

### Task 8: ROI Helpers for Faster Content Authoring (Auto-Register + Low Friction)

**Files:**
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/content/commands/dev/ShopDevCommand.kt`
- Create: `game-server/src/test/kotlin/net/dodian/uber/game/content/commands/dev/ShopDevCommandTest.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugin/ShopPluginDsl.kt`

**Step 1: Write failing test for dev feedback loop**

```kotlin
@Test
fun `shop dev command prints plugin load diagnostics`() {
    val output = ShopDevCommand.renderSummaryForTests()
    assertTrue(output.contains("shops="))
    assertTrue(output.contains("duplicates=0"))
}
```

**Step 2: Run tests to verify fail**

Run: `./gradlew :game-server:test --tests "*ShopDevCommandTest" -i`
Expected: FAIL because command doesn’t exist.

**Step 3: Implement minimal command + DSL quality-of-life**

```kotlin
object ShopDevCommand : CommandContent {
    override fun execute(client: Client, command: String, args: Array<String>): Boolean {
        client.sendMessage(renderSummary())
        return true
    }

    internal fun renderSummaryForTests(): String = renderSummary()

    private fun renderSummary(): String {
        val shops = ShopPluginRegistry.all()
        return "shops=${shops.size} ids=${shops.map { it.id }.sorted()} duplicates=0"
    }
}
```

Add optional DSL helper for faster authoring:

```kotlin
fun items(vararg entries: Pair<Int, Int>) {
    for ((itemId, amount) in entries) item(itemId, amount)
}
```

**Step 4: Run tests**

Run: `./gradlew :game-server:test --tests "*ShopDevCommandTest" --tests "*ShopPluginRegistryTest" -i`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/content/commands/dev/ShopDevCommand.kt game-server/src/test/kotlin/net/dodian/uber/game/content/commands/dev/ShopDevCommandTest.kt game-server/src/main/kotlin/net/dodian/uber/game/content/shop/plugin/ShopPluginDsl.kt
git commit -m "feat: add shop dev diagnostics for faster content iteration"
```

---

## Final Verification Checklist

Run full target suite:

```bash
./gradlew :game-server:test --tests "*SkillPluginRegistryTest" --tests "*SkillPluginRouteBridgeTest" --tests "*SkillDoctorTest" --tests "*ShopDefinitionsTest" --tests "*ShopPluginRegistryTest" --tests "*ShopDevCommandTest" -i
```

Expected:
- All targeted tests PASS.
- No `Thread.sleep` introduced in gameplay/Netty paths.
- No synchronous DB/file I/O added to game tick thread.
- Shop count and IDs unchanged from migrated baseline.
- Existing NPC `openShop(id)` flows still open the same shops.

## Rollout Notes

- Keep `SkillPluginRegistry` compatibility object for one release cycle, then remove it after all imports are migrated.
- Keep `ShopDefinitions` as a compatibility facade over `ShopPluginRegistry` to avoid touching every caller in one PR.
- Prioritize Task 2-4 first (highest ROI, lowest gameplay risk), then Task 6-8 incrementally.
