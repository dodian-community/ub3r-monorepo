package net.dodian.uber.game.api.plugin

data class PluginModuleMetadata(
    val name: String,
    val description: String,
    val version: String = "1.0.0",
    val owner: String = "unspecified",
)

interface PluginModuleMetadataProvider {
    val pluginMetadata: PluginModuleMetadata
}
