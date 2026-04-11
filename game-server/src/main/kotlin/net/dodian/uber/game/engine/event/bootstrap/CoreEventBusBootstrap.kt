package net.dodian.uber.game.engine.event.bootstrap

import net.dodian.uber.game.systems.plugin.ContentModuleIndex

object CoreEventBusBootstrap {
    @JvmStatic
    fun bootstrap() {
        ContentModuleIndex.eventBootstraps.forEach { bootstrap: () -> Unit -> bootstrap() }
    }
}
