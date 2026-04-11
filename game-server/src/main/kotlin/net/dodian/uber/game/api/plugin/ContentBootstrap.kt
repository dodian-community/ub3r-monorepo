package net.dodian.uber.game.api.plugin

interface ContentBootstrap : PluginModuleMetadataProvider {
    val id: String

    override val pluginMetadata: PluginModuleMetadata
        get() = PluginModuleMetadata(
            name = id,
            description = "Engine bootstrap module $id",
            version = "1.0.0",
            owner = "engine",
        )

    fun bootstrap()
}
