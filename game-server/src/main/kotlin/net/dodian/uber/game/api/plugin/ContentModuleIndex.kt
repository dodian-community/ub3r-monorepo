package net.dodian.uber.game.api.plugin

import com.google.common.reflect.ClassPath
import net.dodian.uber.game.api.plugin.dsl.PluginMetadata as DslPluginMetadata
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.item.ItemContent
import net.dodian.uber.game.npc.NpcContentDefinition
import net.dodian.uber.game.npc.NpcModule
import net.dodian.uber.game.objects.ObjectContent
import net.dodian.uber.game.shop.ShopPlugin
import net.dodian.uber.game.ui.buttons.InterfaceButtonBinding
import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.engine.systems.interaction.commands.CommandContent
import org.slf4j.LoggerFactory
import java.lang.reflect.Method

data class PluginCatalogEntry(
    val moduleClass: String,
    val kind: String,
    val metadata: PluginModuleMetadata,
)

/**
 * Runtime-scanned plugin module index with deterministic ordering and validation.
 */
object ContentModuleIndex {
    private val logger = LoggerFactory.getLogger(ContentModuleIndex::class.java)

    private enum class IndexLifecycle {
        DISCOVER,
        VALIDATE,
        FROZEN,
    }

    /** Canonical roots for adapted-Luna packaging. */
    private val CANONICAL_SCAN_PACKAGES = listOf(
        "net.dodian.uber.game.api.plugin",
        "net.dodian.uber.game.skill",
        "net.dodian.uber.game.npc",
        "net.dodian.uber.game.item",
        "net.dodian.uber.game.objects",
        "net.dodian.uber.game.combat",
        "net.dodian.uber.game.social",
        "net.dodian.uber.game.ui",
        "net.dodian.uber.game.command",
        "net.dodian.uber.game.activity",
        "net.dodian.uber.game.shop",
        "net.dodian.uber.game.world",
        "net.dodian.uber.game.player",
        "net.dodian.uber.game.engine.event.bootstrap",
        "net.dodian.uber.game.engine.systems.interaction.commands",
        "net.dodian.uber.game.engine.systems.interaction.items",
        "net.dodian.uber.game.engine.systems.interaction.npcs",
        "net.dodian.uber.game.engine.systems.interaction.objects",
    )
    private val SCAN_PACKAGES = CANONICAL_SCAN_PACKAGES.distinct()

    @JvmField val canonicalScanPackages: List<String> = CANONICAL_SCAN_PACKAGES.toList()

    @Volatile
    private var lifecycle: IndexLifecycle = IndexLifecycle.DISCOVER

    @JvmField val interfaceButtons: List<InterfaceButtonContent>
    @JvmField val objectContents: List<Pair<String, ObjectContent>>
    @JvmField val itemContents: List<ItemContent>
    @JvmField val commandContents: List<CommandContent>
    @JvmField val npcContents: List<NpcContentDefinition>
    @JvmField val skillPlugins: List<SkillPlugin>
    @JvmField val shopPlugins: List<ShopPlugin>
    @JvmField val eventBootstraps: List<() -> Unit>
    @JvmField val contentBootstraps: List<ContentBootstrap>
    @JvmField val pluginCatalog: List<PluginCatalogEntry>

    init {
        val classPath = ClassPath.from(Thread.currentThread().contextClassLoader)
        val scannedClasses = discoverClasses(classPath)

        lifecycle = IndexLifecycle.DISCOVER
        val discoveredButtons = mutableListOf<Pair<String, InterfaceButtonContent>>()
        val discoveredObjects = mutableListOf<Pair<String, ObjectContent>>()
        val discoveredItems = mutableListOf<Pair<String, ItemContent>>()
        val discoveredCommands = mutableListOf<Pair<String, CommandContent>>()
        val discoveredNpcs = mutableListOf<Pair<String, NpcContentDefinition>>()
        val discoveredSkills = mutableListOf<Pair<String, SkillPlugin>>()
        val discoveredShops = mutableListOf<Pair<String, ShopPlugin>>()
        val discoveredEvents = mutableListOf<Pair<String, () -> Unit>>()
        val discoveredBootstraps = mutableListOf<Pair<String, ContentBootstrap>>()
        val discoveredCatalog = mutableListOf<PluginCatalogEntry>()

        for (clazz in scannedClasses) {
            val instance = try {
                clazz.kotlin.objectInstance ?: continue
            } catch (_: Throwable) {
                continue
            }

            val className = clazz.name
            val simpleName = clazz.simpleName

            var pluginKind: String? = null
            when (instance) {
                is InterfaceButtonContent -> {
                    discoveredButtons += className to instance
                    pluginKind = "interface-button"
                }
                is ObjectContent -> {
                    discoveredObjects += (simpleName to instance)
                    pluginKind = "object-content"
                }
                is ItemContent -> {
                    discoveredItems += className to instance
                    pluginKind = "item-content"
                }
                is CommandContent -> {
                    discoveredCommands += className to instance
                    pluginKind = "command-content"
                }
                is SkillPlugin -> {
                    discoveredSkills += className to instance
                    pluginKind = "skill-plugin"
                }
                is ShopPlugin -> {
                    discoveredShops += className to instance
                    pluginKind = "shop-plugin"
                }
                is ContentBootstrap -> {
                    discoveredBootstraps += className to instance
                    pluginKind = "bootstrap"
                }
            }

            if (instance is NpcModule) {
                discoveredNpcs += className to instance.definition
                pluginKind = pluginKind ?: "npc-module"
            }

            if (clazz.name.startsWith("net.dodian.uber.game.engine.event.bootstrap") &&
                clazz.simpleName.endsWith("Bootstrap") &&
                clazz.simpleName != "CoreEventBusBootstrap"
            ) {
                val method: Method? = try {
                    clazz.getMethod("bootstrap")
                } catch (_: NoSuchMethodException) {
                    null
                }
                if (method != null) {
                    discoveredEvents += className to { method.invoke(instance) }
                    pluginKind = pluginKind ?: "event-bootstrap"
                }
            }

            if (pluginKind != null) {
                discoveredCatalog += PluginCatalogEntry(
                    moduleClass = className,
                    kind = pluginKind,
                    metadata = resolveMetadata(instance, clazz),
                )
            }
        }

        lifecycle = IndexLifecycle.VALIDATE
        validateItemOwnership(discoveredItems)
        validateShopOwnership(discoveredShops)
        validateInterfaceButtonOwnership(discoveredButtons)

        interfaceButtons = discoveredButtons.sortedBy { it.first }.map { it.second }
        objectContents = discoveredObjects.sortedBy { it.first }
        itemContents = discoveredItems.sortedBy { it.first }.map { it.second }
        commandContents = discoveredCommands.sortedBy { it.first }.map { it.second }
        npcContents = discoveredNpcs.sortedBy { it.first }.map { it.second }
        skillPlugins = discoveredSkills.sortedBy { it.first }.map { it.second }
        shopPlugins = discoveredShops.sortedBy { it.first }.map { it.second }
        eventBootstraps = discoveredEvents.sortedBy { it.first }.map { it.second }
        contentBootstraps = discoveredBootstraps.sortedBy { it.first }.map { it.second }
        pluginCatalog = discoveredCatalog.sortedBy { it.moduleClass }
        lifecycle = IndexLifecycle.FROZEN

        logger.info(
            "Plugin index {}: buttons={}, objects={}, items={}, commands={}, npcs={}, skills={}, shops={}, events={}, bootstraps={}, catalog={}, canonicalRoots={}, legacyRoots={}",
            lifecycle.name.lowercase(),
            interfaceButtons.size,
            objectContents.size,
            itemContents.size,
            commandContents.size,
            npcContents.size,
            skillPlugins.size,
            shopPlugins.size,
            eventBootstraps.size,
            contentBootstraps.size,
            pluginCatalog.size,
            CANONICAL_SCAN_PACKAGES.size,
            0,
        )
    }

    private fun discoverClasses(classPath: ClassPath): List<Class<*>> {
        val loadedByName = LinkedHashMap<String, Class<*>>()
        for (scanPackage in SCAN_PACKAGES) {
            val classInfos = classPath.getTopLevelClassesRecursive(scanPackage).sortedBy { it.name }
            for (info in classInfos) {
                if (loadedByName.containsKey(info.name)) continue
                val loaded = try {
                    info.load()
                } catch (e: Throwable) {
                    logger.warn("Failed to load class {}: {}", info.name, e.message)
                    null
                }
                if (loaded != null) {
                    loadedByName[info.name] = loaded
                }
            }
        }
        return loadedByName.values.sortedBy { it.name }
    }

    private fun resolveMetadata(instance: Any, clazz: Class<*>): PluginModuleMetadata {
        if (instance is PluginModuleMetadataProvider) {
            return validateMetadata(instance.pluginMetadata, clazz.name)
        }

        val metadataGetter = clazz.methods.firstOrNull { it.parameterCount == 0 && it.name == "getMetadata" }
        val dslMetadata = metadataGetter?.invoke(instance) as? DslPluginMetadata
        if (dslMetadata != null) {
            return validateMetadata(
                PluginModuleMetadata(
                    name = dslMetadata.name?.trim().orEmpty().ifBlank { clazz.simpleName },
                    description = dslMetadata.description?.trim().orEmpty().ifBlank { "No description provided." },
                    version = dslMetadata.version.trim().ifBlank { "1.0.0" },
                    owner = dslMetadata.owner?.trim().orEmpty().ifBlank { "unspecified" },
                ),
                clazz.name,
            )
        }

        if (instance is SkillPlugin) {
            return PluginModuleMetadata(
                name = instance.definition.name,
                description = "Skill plugin for ${instance.definition.skill.name.lowercase()}",
                version = "1.0.0",
                owner = "unspecified",
            )
        }

        return PluginModuleMetadata(
            name = clazz.simpleName,
            description = "Undocumented plugin module.",
            version = "1.0.0",
            owner = "unspecified",
        )
    }

    private fun validateMetadata(metadata: PluginModuleMetadata, moduleClass: String): PluginModuleMetadata {
        require(metadata.name.isNotBlank()) { "Plugin metadata name is blank for module=$moduleClass" }
        require(metadata.description.isNotBlank()) { "Plugin metadata description is blank for module=$moduleClass" }
        require(metadata.version.isNotBlank()) { "Plugin metadata version is blank for module=$moduleClass" }
        require(metadata.owner.isNotBlank()) { "Plugin metadata owner is blank for module=$moduleClass" }
        return metadata
    }

    private fun validateItemOwnership(items: List<Pair<String, ItemContent>>) {
        val owners = HashMap<Int, String>()
        for ((module, content) in items) {
            for (itemId in content.itemIds) {
                val existing = owners.putIfAbsent(itemId, module)
                require(existing == null) {
                    "Duplicate ItemContent ownership for itemId=$itemId existing=$existing new=$module"
                }
            }
        }
    }

    private fun validateShopOwnership(shops: List<Pair<String, ShopPlugin>>) {
        val owners = HashMap<Int, String>()
        for ((module, plugin) in shops) {
            val existing = owners.putIfAbsent(plugin.definition.id, module)
            require(existing == null) {
                "Duplicate ShopPlugin ownership for shopId=${plugin.definition.id} existing=$existing new=$module"
            }
        }
    }

    private fun validateInterfaceButtonOwnership(buttons: List<Pair<String, InterfaceButtonContent>>) {
        val routeOwners = HashMap<Long, String>()
        val semanticOwners = HashMap<String, String>()
        for ((module, content) in buttons) {
            for (binding in content.bindings) {
                validateButtonBinding(module, binding, routeOwners, semanticOwners)
            }
        }
    }

    private fun validateButtonBinding(
        module: String,
        binding: InterfaceButtonBinding,
        routeOwners: MutableMap<Long, String>,
        semanticOwners: MutableMap<String, String>,
    ) {
        val semanticKey = "${binding.interfaceId}:${binding.componentId}:${binding.opIndex ?: -1}:${binding.componentKey}"
        val semanticOwner = semanticOwners.putIfAbsent(semanticKey, module)
        require(semanticOwner == null) {
            "Duplicate interface semantic binding key=$semanticKey existing=$semanticOwner new=$module"
        }

        val op = binding.opIndex ?: -1
        for (rawButtonId in binding.rawButtonIds) {
            val routeKey = (rawButtonId.toLong() shl 32) or (op.toLong() and 0xffffffffL)
            val existing = routeOwners.putIfAbsent(routeKey, module)
            require(existing == null) {
                "Duplicate interface route ownership rawButtonId=$rawButtonId op=$op existing=$existing new=$module"
            }
        }
    }
}
