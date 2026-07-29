package net.dodian.uber.game.api.plugin.economy

import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.ContentModuleManifestProvider
import net.dodian.uber.game.api.plugin.PluginModuleMetadata
import net.dodian.uber.game.api.plugin.PluginModuleMetadataProvider

interface EconomyPlugin : PluginModuleMetadataProvider, ContentModuleManifestProvider {
    val id: String
    val name: String
    val routes: Set<String>

    override val pluginMetadata: PluginModuleMetadata
        get() = PluginModuleMetadata(name, "$name economy content", owner = "gameplay")
    override val contentManifest: ContentModuleManifest
        get() = ContentModuleManifest(id = id, owner = "gameplay", version = "1.0.0", declaredRouteKeys = routes)
}
