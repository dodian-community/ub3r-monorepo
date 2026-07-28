package net.dodian.uber.game.api.plugin

/** Read-only, deterministic route inventory for beta diagnostics and developer tooling. */
data class ContentRouteEntry(
    val moduleId: String,
    val routeType: String,
    val key: String,
    val policy: String? = null,
)

/** Stable ownership inventory for migration reviews and startup diagnostics. */
data class ContentOwnershipEntry(
    val moduleId: String,
    val routeType: String,
    val key: String,
    val sourceClass: String,
    val owner: String,
    val maturity: ContentMaturity,
    val enabled: Boolean,
    val legacy: Boolean,
)

object ContentRouteCatalog {
    fun snapshot(): List<ContentRouteEntry> {
        val manifestsByClass = ContentModuleIndex.pluginCatalog.associate { it.moduleClass to it.manifest }
        return buildList {
            ContentModuleIndex.objectContents.forEach { (_, content) ->
                val manifest = manifestsByClass[content::class.java.name] ?: return@forEach
                content.bindings().forEach { binding ->
                    add(ContentRouteEntry(manifest.id, "object", binding.objectId.toString(), binding.matcher.describe()))
                }
            }
            ContentModuleIndex.itemContents.forEach { content ->
                val manifest = manifestsByClass[content::class.java.name] ?: return@forEach
                content.itemIds.forEach { add(ContentRouteEntry(manifest.id, "item", it.toString())) }
            }
            ContentModuleIndex.npcModules.forEach { module ->
                val manifest = manifestsByClass[module::class.java.name] ?: return@forEach
                module.definition.npcIds.forEach { add(ContentRouteEntry(manifest.id, "npc", it.toString())) }
            }
            ContentModuleIndex.interfaceButtons.forEach { content ->
                val manifest = manifestsByClass[content::class.java.name] ?: return@forEach
                content.bindings.forEach { binding ->
                    binding.rawButtonIds.forEach {
                        add(ContentRouteEntry(manifest.id, "button", "$it:${binding.opIndex ?: -1}:${binding.requiredInterfaceId}"))
                    }
                }
            }
            ContentModuleIndex.skillPlugins.forEach { plugin ->
                val manifest = manifestsByClass[plugin::class.java.name] ?: return@forEach
                plugin.definition.objectBindings.forEach { binding -> binding.objectIds.forEach { add(ContentRouteEntry(manifest.id, "skill-object", "${binding.option}:$it", binding.preset.name)) } }
                plugin.definition.npcBindings.forEach { binding -> binding.npcIds.forEach { add(ContentRouteEntry(manifest.id, "skill-npc", "${binding.option}:$it", binding.preset.name)) } }
                plugin.definition.itemBindings.forEach { binding -> binding.itemIds.forEach { add(ContentRouteEntry(manifest.id, "skill-item", "${binding.option}:$it", binding.preset.name)) } }
                plugin.definition.itemOnItemBindings.forEach { binding -> add(ContentRouteEntry(manifest.id, "skill-item-on-item", "${binding.leftItemId}:${binding.rightItemId}", binding.preset.name)) }
                plugin.definition.itemOnObjectBindings.forEach { binding -> binding.objectIds.forEach { objectId -> binding.itemIds.forEach { itemId -> add(ContentRouteEntry(manifest.id, "skill-item-on-object", "$objectId:$itemId", binding.preset.name)) } } }
                plugin.definition.magicOnObjectBindings.forEach { binding -> binding.objectIds.forEach { objectId -> binding.spellIds.forEach { spellId -> add(ContentRouteEntry(manifest.id, "skill-magic-on-object", "$objectId:$spellId", binding.preset.name)) } } }
                plugin.definition.buttonBindings.forEach { binding -> binding.rawButtonIds.forEach { add(ContentRouteEntry(manifest.id, "skill-button", "$it:${binding.opIndex ?: -1}:${binding.requiredInterfaceId}", binding.preset.name)) } }
                plugin.definition.itemGridBindings.forEach { binding ->
                    add(ContentRouteEntry(manifest.id, "skill-item-grid", binding.interfaceId.toString(), binding.preset.name))
                }
            }
            ContentModuleIndex.skillSupportModules.forEach { module ->
                val manifest = manifestsByClass[module::class.java.name] ?: return@forEach
                module.definition.itemBindings.forEach { binding -> binding.itemIds.forEach { add(ContentRouteEntry(manifest.id, "skill-item", "${binding.option}:$it", binding.preset.name)) } }
                module.definition.buttonBindings.forEach { binding -> binding.rawButtonIds.forEach { add(ContentRouteEntry(manifest.id, "skill-button", "$it:${binding.opIndex ?: -1}:${binding.requiredInterfaceId}", binding.preset.name)) } }
                module.definition.itemGridBindings.forEach { binding ->
                    add(ContentRouteEntry(manifest.id, "skill-item-grid", binding.interfaceId.toString(), binding.preset.name))
                }
            }
        }.sortedWith(compareBy(ContentRouteEntry::moduleId, ContentRouteEntry::routeType, ContentRouteEntry::key))
    }

    fun find(id: Int): List<ContentRouteEntry> = snapshot().filter { entry -> entry.key.split(':').any { it == id.toString() } }
    fun byModule(moduleId: String): List<ContentRouteEntry> = snapshot().filter { it.moduleId == moduleId }

    fun ownershipReport(): List<ContentOwnershipEntry> {
        val manifests = ContentModuleIndex.pluginCatalog.associateBy { it.manifest.id }
        return snapshot().map { route ->
            val catalog = manifests.getValue(route.moduleId)
            ContentOwnershipEntry(
                moduleId = route.moduleId,
                routeType = route.routeType,
                key = route.key,
                sourceClass = catalog.moduleClass,
                owner = catalog.manifest.owner,
                maturity = catalog.manifest.maturity,
                enabled = ContentModuleFeatureState.isEnabled(catalog.manifest),
                legacy = catalog.manifest.maturity == ContentMaturity.LEGACY,
            )
        }.sortedWith(compareBy(ContentOwnershipEntry::moduleId, ContentOwnershipEntry::routeType, ContentOwnershipEntry::key))
    }

    /** Maps dispatcher fault keys back to a stable module id when the route is plugin-owned. */
    fun moduleForBinding(bindingKey: String): String? {
        val (routeType, key) = when {
            bindingKey.startsWith("skill.object:") -> "skill-object" to bindingKey.removePrefix("skill.object:")
            bindingKey.startsWith("skill.npc:") -> "skill-npc" to bindingKey.removePrefix("skill.npc:")
            bindingKey.startsWith("skill.item:") -> "skill-item" to bindingKey.removePrefix("skill.item:")
            bindingKey.startsWith("skill.item-on-item:") -> "skill-item-on-item" to bindingKey.removePrefix("skill.item-on-item:")
            bindingKey.startsWith("skill.item-on-object:") -> "skill-item-on-object" to bindingKey.removePrefix("skill.item-on-object:")
            bindingKey.startsWith("skill.magic-on-object:") -> "skill-magic-on-object" to bindingKey.removePrefix("skill.magic-on-object:")
            bindingKey.startsWith("skill.button:") -> "skill-button" to bindingKey.removePrefix("skill.button:")
            else -> return null
        }
        return snapshot().firstOrNull { entry ->
            entry.routeType == routeType &&
                (entry.key == key || routeType == "skill-button" && entry.key.split(':').take(2) == key.split(':').take(2))
        }?.moduleId
    }
}
