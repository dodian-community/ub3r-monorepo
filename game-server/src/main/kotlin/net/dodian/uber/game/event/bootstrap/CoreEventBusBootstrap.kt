package net.dodian.uber.game.event.bootstrap

import net.dodian.uber.game.plugin.PluginModuleIndex

object CoreEventBusBootstrap {
    @JvmStatic
    fun bootstrap() {
        PluginModuleIndex.eventBootstraps.forEach { bootstrap -> bootstrap() }
    }
}
