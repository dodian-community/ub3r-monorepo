package net.dodian.uber.game.content.shop.plugin

import net.dodian.uber.game.api.plugin.PluginModuleMetadata
import net.dodian.uber.game.api.plugin.PluginModuleMetadataProvider
import net.dodian.uber.game.content.shop.ShopDefinition

interface ShopPlugin : PluginModuleMetadataProvider {
    val definition: ShopDefinition

    override val pluginMetadata: PluginModuleMetadata
        get() = PluginModuleMetadata(
            name = definition.name,
            description = "Shop plugin for shop ${definition.id}",
            version = "1.0.0",
            owner = "commerce",
        )
}
