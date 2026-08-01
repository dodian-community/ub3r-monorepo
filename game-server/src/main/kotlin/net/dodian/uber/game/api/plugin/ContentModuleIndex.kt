package net.dodian.uber.game.api.plugin

import com.google.common.reflect.ClassPath
import net.dodian.uber.game.api.plugin.dsl.PluginMetadata as DslPluginMetadata
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillContentModule
import net.dodian.uber.game.api.plugin.skills.SkillSupportModule
import net.dodian.uber.game.api.plugin.skills.routeKeys as supportRouteKeys
import net.dodian.uber.game.api.plugin.skills.routeKeys
import net.dodian.uber.game.api.plugin.quests.QuestPlugin
import net.dodian.uber.game.api.plugin.quests.routeKeys as questRouteKeys
import net.dodian.uber.game.api.plugin.social.SocialPlugin
import net.dodian.uber.game.api.plugin.social.routeKeys as socialRouteKeys
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
    val manifest: ContentModuleManifest,
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
        "net.dodian.uber.social",
        "net.dodian.uber.game.engine.event.bootstrap",
        "net.dodian.uber.game.engine.systems.interaction.commands",
        "net.dodian.uber.game.engine.systems.interaction.items",
        "net.dodian.uber.game.engine.systems.interaction.npcs",
        "net.dodian.uber.game.engine.systems.interaction.objects",
        "net.dodian.uber.skills.agility", "net.dodian.uber.skills.cooking", "net.dodian.uber.skills.crafting",
        "net.dodian.uber.skills.farming", "net.dodian.uber.skills.firemaking", "net.dodian.uber.skills.fishing",
        "net.dodian.uber.skills.fletching", "net.dodian.uber.skills.herblore", "net.dodian.uber.skills.mining",
        "net.dodian.uber.skills.prayer", "net.dodian.uber.skills.runecrafting", "net.dodian.uber.skills.slayer",
        "net.dodian.uber.skills.skillguide", "net.dodian.uber.skills.smithing", "net.dodian.uber.skills.thieving",
        "net.dodian.uber.skills.woodcutting",
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
    @JvmField val npcModules: List<NpcModule>
    @JvmField val skillPlugins: List<SkillPlugin>
    @JvmField val skillSupportModules: List<SkillSupportModule>
    @JvmField val questPlugins: List<QuestPlugin>
    @JvmField val socialPlugins: List<SocialPlugin>
    @JvmField val shopPlugins: List<ShopPlugin>
    @JvmField val eventBootstraps: List<() -> Unit>
    @JvmField val contentBootstraps: List<ContentBootstrap>
    @JvmField val contentPlugins: List<ContentPlugin>
    @JvmField val pluginCatalog: List<PluginCatalogEntry>

    init {
        val classPath = ClassPath.from(Thread.currentThread().contextClassLoader)
        val descriptorClasses = discoverSkillModuleClasses(classPath) + discoverQuestModuleClasses(classPath) +
            discoverContentModuleClasses(classPath)
        val scannedClasses = (discoverClasses(classPath) + descriptorClasses)
            .distinctBy { it.name }
            .sortedBy { it.name }

        lifecycle = IndexLifecycle.DISCOVER
        val discoveredButtons = mutableListOf<Pair<String, InterfaceButtonContent>>()
        val discoveredObjects = mutableListOf<Pair<String, ObjectContent>>()
        val discoveredItems = mutableListOf<Pair<String, ItemContent>>()
        val discoveredCommands = mutableListOf<Pair<String, CommandContent>>()
        val discoveredNpcs = mutableListOf<Pair<String, NpcContentDefinition>>()
        val discoveredNpcModules = mutableListOf<Pair<String, NpcModule>>()
        val discoveredSkills = mutableListOf<Pair<String, SkillPlugin>>()
        val discoveredSkillSupport = mutableListOf<Pair<String, SkillSupportModule>>()
        val discoveredQuests = mutableListOf<Pair<String, QuestPlugin>>()
        val discoveredSocial = mutableListOf<Pair<String, SocialPlugin>>()
        val discoveredShops = mutableListOf<Pair<String, ShopPlugin>>()
        val discoveredEvents = mutableListOf<Pair<String, () -> Unit>>()
        val discoveredBootstraps = mutableListOf<Pair<String, ContentBootstrap>>()
        val discoveredContentPlugins = mutableListOf<Pair<String, ContentPlugin>>()
        val discoveredCatalog = mutableListOf<PluginCatalogEntry>()
        val discoveredManifests = mutableListOf<ContentModuleManifest>()

        for (clazz in scannedClasses) {
            val instance = try {
                clazz.kotlin.objectInstance ?: continue
            } catch (reflectionFailure: Throwable) {
                // Kotlin file facades are part of package scanning but cannot have an object
                // instance. Private Kotlin objects are valid modules, though, and reflection's
                // objectInstance accessor cannot read them on recent JVMs.
                if (clazz.name.endsWith("Kt")) continue
                try {
                    clazz.getDeclaredField("INSTANCE").apply { isAccessible = true }.get(null)
                } catch (instanceFailure: Throwable) {
                    throw IllegalStateException("Failed to initialize content module ${clazz.name}", instanceFailure)
                }
            }

            val className = clazz.name
            val simpleName = clazz.simpleName
            val manifest = resolveManifest(instance, clazz)
            if (instance is SkillPlugin) {
                val actualRoutes = instance.definition.routeKeys()
                require(manifest.declaredRouteKeys == actualRoutes) {
                    "Content module ${manifest.id} declared routes ${manifest.declaredRouteKeys.sorted()} " +
                        "but registry routes are ${actualRoutes.sorted()}"
                }
            }
            if (instance is SkillSupportModule) {
                val actualRoutes = instance.definition.supportRouteKeys()
                require(manifest.declaredRouteKeys == actualRoutes) {
                    "Content module ${manifest.id} declared routes ${manifest.declaredRouteKeys.sorted()} but registry routes are ${actualRoutes.sorted()}"
                }
            }
            if (instance is QuestPlugin) {
                val actualRoutes = instance.definition.questRouteKeys()
                require(manifest.declaredRouteKeys == actualRoutes) {
                    "Content module ${manifest.id} declared routes ${manifest.declaredRouteKeys.sorted()} but registry routes are ${actualRoutes.sorted()}"
                }
            }
            if (instance is SocialPlugin) {
                val actualRoutes = instance.definition.socialRouteKeys()
                require(manifest.declaredRouteKeys == actualRoutes) {
                    "Content module ${manifest.id} declared routes ${manifest.declaredRouteKeys.sorted()} but registry routes are ${actualRoutes.sorted()}"
                }
            }
            val enabled = ContentModuleFeatureState.isEnabled(manifest)
            discoveredManifests += manifest

            var pluginKind: String? = null
            when (instance) {
                is InterfaceButtonContent -> {
                    if (enabled) discoveredButtons += className to instance
                    pluginKind = "interface-button"
                }
                is ObjectContent -> {
                    if (enabled) discoveredObjects += (simpleName to instance)
                    pluginKind = "object-content"
                }
                is ItemContent -> {
                    if (enabled) discoveredItems += className to instance
                    pluginKind = "item-content"
                }
                is CommandContent -> {
                    if (enabled) discoveredCommands += className to instance
                    pluginKind = "command-content"
                }
                is SkillPlugin -> {
                    if (enabled) discoveredSkills += className to instance
                    pluginKind = "skill-plugin"
                }
                is SkillSupportModule -> {
                    if (enabled) discoveredSkillSupport += className to instance
                    pluginKind = "skill-support"
                }
                is QuestPlugin -> {
                    if (enabled) discoveredQuests += className to instance
                    pluginKind = "quest-plugin"
                }
                is SocialPlugin -> {
                    if (enabled) discoveredSocial += className to instance
                    pluginKind = "social-plugin"
                }
                is ShopPlugin -> {
                    if (enabled) discoveredShops += className to instance
                    pluginKind = "shop-plugin"
                }
                is ContentBootstrap -> {
                    if (enabled) discoveredBootstraps += className to instance
                    pluginKind = "bootstrap"
                }
            }

            if (instance is NpcModule) {
                if (enabled) {
                    discoveredNpcs += className to instance.definition
                    discoveredNpcModules += className to instance
                }
                pluginKind = pluginKind ?: "npc-module"
            }

            if (instance is ContentPlugin) {
                if (enabled) discoveredContentPlugins += className to instance
                pluginKind = pluginKind ?: "content-plugin"
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
                if (method != null && enabled) {
                    discoveredEvents += className to { method.invoke(instance) }
                    pluginKind = pluginKind ?: "event-bootstrap"
                }
            }

            if (pluginKind != null) {
                discoveredCatalog += PluginCatalogEntry(
                    moduleClass = className,
                    kind = pluginKind,
                    metadata = resolveMetadata(instance, clazz),
                    manifest = manifest,
                )
            }
        }

        lifecycle = IndexLifecycle.VALIDATE
        validateItemOwnership(discoveredItems)
        validateShopOwnership(discoveredShops)
        validateInterfaceButtonOwnership(discoveredButtons)
        validateSocialOwnership(discoveredSocial)

        interfaceButtons = discoveredButtons.sortedBy { it.first }.map { it.second }
        objectContents = discoveredObjects.sortedBy { it.first }
        itemContents = discoveredItems.sortedBy { it.first }.map { it.second }
        commandContents = discoveredCommands.sortedBy { it.first }.map { it.second }
        npcContents = discoveredNpcs.sortedBy { it.first }.map { it.second }
        npcModules = discoveredNpcModules.sortedBy { it.first }.map { it.second }
        skillPlugins = discoveredSkills.sortedBy { it.first }.map { it.second }
        skillSupportModules = discoveredSkillSupport.sortedBy { it.first }.map { it.second }
        questPlugins = discoveredQuests.sortedBy { it.first }.map { it.second }
        socialPlugins = discoveredSocial.sortedBy { it.first }.map { it.second }
        shopPlugins = discoveredShops.sortedBy { it.first }.map { it.second }
        eventBootstraps = discoveredEvents.sortedBy { it.first }.map { it.second }
        contentBootstraps = discoveredBootstraps.sortedBy { it.first }.map { it.second }
        contentPlugins = discoveredContentPlugins.sortedBy { it.first }.map { it.second }
        pluginCatalog = discoveredCatalog.sortedBy { it.moduleClass }
        ContentPlatformCatalog.publish(discoveredManifests)
        val platformSnapshot = ContentPlatformCatalog.snapshot()
        logger.debug(
            "Content manifest modules enabled=[{}] disabled=[{}]",
            platformSnapshot.modules.filter { it.id in platformSnapshot.enabledModuleIds }.joinToString { it.id },
            platformSnapshot.modules.filter { it.id !in platformSnapshot.enabledModuleIds }.joinToString { it.id },
        )
        lifecycle = IndexLifecycle.FROZEN

        // commands/npcs/skills/shops are intentionally omitted here - CommandContentRegistry,
        // NpcContentRegistry, SkillPluginRegistry, and ShopCatalogRegistry each already log their
        // own count. buttons/objects/items below count content-module classes, a different axis
        // from those subsystems' binding counts, so they're not duplicates.
        logger.info(
            "Plugin index {}: buttons={}, objects={}, items={}, events={}, bootstraps={}, catalog={}, enabledModules={}, disabledModules={}, fingerprint={}",
            lifecycle.name.lowercase(),
            interfaceButtons.size,
            objectContents.size,
            itemContents.size,
            eventBootstraps.size,
            contentBootstraps.size,
            pluginCatalog.size,
            platformSnapshot.enabledCount,
            platformSnapshot.disabledCount,
            platformSnapshot.fingerprint.take(12),
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

    /**
     * Gradle skill modules publish unique descriptors so packaged JARs can be
     * verified without relying solely on a manually maintained package list.
     * Legacy modules remain classpath-scanned until their migration completes.
     */
    private fun discoverSkillModuleClasses(classPath: ClassPath): List<Class<*>> {
        val prefix = "META-INF/ub3r/skill-modules/"
        val descriptors = classPath.resources
            .filter { it.resourceName.startsWith(prefix) && it.resourceName.endsWith(".toml") }
            .sortedBy { it.resourceName }
        val ids = mutableSetOf<String>()
        val classes = mutableListOf<Class<*>>()
        descriptors.forEach { resource ->
            val values = resource.asByteSource().openStream().bufferedReader().useLines { lines ->
                lines.map(String::trim)
                    .filter { line -> line.isNotEmpty() && !line.startsWith("#") && line.contains('=') }
                    .associate { line ->
                        val (key, rawValue) = line.split('=', limit = 2)
                        key.trim() to rawValue.trim().trim('"')
                    }
            }
            require(values["schema_version"] == "1") { "Unsupported skill module descriptor ${resource.resourceName}" }
            val moduleId = requireNotNull(values["module_id"]) { "Missing module_id in ${resource.resourceName}" }
            val implementationClass = requireNotNull(values["implementation_class"]) {
                "Missing implementation_class in ${resource.resourceName}"
            }
            val kind = values["kind"]
            require(kind in setOf("gameplay", "support")) { "Invalid kind in ${resource.resourceName}" }
            require(ids.add(moduleId)) { "Duplicate Gradle skill module descriptor id $moduleId" }
            val loaded = runCatching { Class.forName(implementationClass) }.getOrElse { cause ->
                throw IllegalStateException("Unable to load $implementationClass from ${resource.resourceName}", cause)
            }
            require(
                (kind == "gameplay" && SkillPlugin::class.java.isAssignableFrom(loaded)) ||
                    (kind == "support" && SkillContentModule::class.java.isAssignableFrom(loaded)),
            ) { "Descriptor ${resource.resourceName} declares $kind but $implementationClass does not implement its contract" }
            classes += loaded
        }
        return classes
    }

    /** Descriptor-based discovery for `ub3r.quest-plugin` modules - mirrors [discoverSkillModuleClasses]. */
    private fun discoverQuestModuleClasses(classPath: ClassPath): List<Class<*>> {
        val prefix = "META-INF/ub3r/quest-modules/"
        val descriptors = classPath.resources
            .filter { it.resourceName.startsWith(prefix) && it.resourceName.endsWith(".toml") }
            .sortedBy { it.resourceName }
        val ids = mutableSetOf<String>()
        val classes = mutableListOf<Class<*>>()
        descriptors.forEach { resource ->
            val values = resource.asByteSource().openStream().bufferedReader().useLines { lines ->
                lines.map(String::trim)
                    .filter { line -> line.isNotEmpty() && !line.startsWith("#") && line.contains('=') }
                    .associate { line ->
                        val (key, rawValue) = line.split('=', limit = 2)
                        key.trim() to rawValue.trim().trim('"')
                    }
            }
            require(values["schema_version"] == "1") { "Unsupported quest module descriptor ${resource.resourceName}" }
            val moduleId = requireNotNull(values["module_id"]) { "Missing module_id in ${resource.resourceName}" }
            val implementationClass = requireNotNull(values["implementation_class"]) {
                "Missing implementation_class in ${resource.resourceName}"
            }
            require(ids.add(moduleId)) { "Duplicate Gradle quest module descriptor id $moduleId" }
            val loaded = runCatching { Class.forName(implementationClass) }.getOrElse { cause ->
                throw IllegalStateException("Unable to load $implementationClass from ${resource.resourceName}", cause)
            }
            require(QuestPlugin::class.java.isAssignableFrom(loaded)) {
                "Descriptor ${resource.resourceName} declares $implementationClass which does not implement QuestPlugin"
            }
            classes += loaded
        }
        return classes
    }

    private fun discoverContentModuleClasses(classPath: ClassPath): List<Class<*>> {
        val prefix = "META-INF/ub3r/content-modules/"
        val descriptors = classPath.resources
            .filter { it.resourceName.startsWith(prefix) && it.resourceName.endsWith(".toml") }
            .sortedBy { it.resourceName }
        val ids = mutableSetOf<String>()
        return descriptors.map { resource ->
            val values = resource.asByteSource().openStream().bufferedReader().useLines { lines ->
                lines.map(String::trim)
                    .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
                    .associate { line ->
                        val (key, rawValue) = line.split('=', limit = 2)
                        key.trim() to rawValue.trim().trim('"')
                    }
            }
            require(values["schema_version"] == "1") { "Unsupported content module descriptor ${resource.resourceName}" }
            val moduleId = requireNotNull(values["module_id"]) { "Missing module_id in ${resource.resourceName}" }
            val implementationClass = requireNotNull(values["implementation_class"]) {
                "Missing implementation_class in ${resource.resourceName}"
            }
            require(values["family"]?.matches(Regex("[a-z][a-z0-9-]{1,31}")) == true) {
                "Invalid family in ${resource.resourceName}"
            }
            require(ids.add(moduleId)) { "Duplicate generic content module descriptor id $moduleId" }
            runCatching { Class.forName(implementationClass) }.getOrElse { cause ->
                throw IllegalStateException("Unable to load $implementationClass from ${resource.resourceName}", cause)
            }.also { loaded ->
                require(ContentModuleManifestProvider::class.java.isAssignableFrom(loaded)) {
                    "$implementationClass does not implement ContentModuleManifestProvider"
                }
            }
        }
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

    private fun resolveManifest(instance: Any, clazz: Class<*>): ContentModuleManifest {
        if (instance is ContentModuleManifestProvider) return instance.contentManifest
        val metadata = resolveMetadata(instance, clazz)
        // Existing modules remain bootable during the staged platform rollout.
        return ContentModuleManifest(
            id = clazz.name.lowercase(),
            owner = metadata.owner,
            version = metadata.version,
            maturity = ContentMaturity.LEGACY,
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

    private fun validateSocialOwnership(plugins: List<Pair<String, SocialPlugin>>) {
        val owners = HashMap<String, String>()
        for ((module, plugin) in plugins) {
            for (route in plugin.definition.socialRouteKeys()) {
                val existing = owners.putIfAbsent(route, module)
                require(existing == null) {
                    "Duplicate social route ownership route=$route existing=$existing new=$module"
                }
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
