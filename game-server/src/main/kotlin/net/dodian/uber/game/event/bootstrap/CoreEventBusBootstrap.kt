package net.dodian.uber.game.event.bootstrap

import net.dodian.uber.game.content.ContentModuleIndex

object CoreEventBusBootstrap {
    @JvmStatic
    fun bootstrap() {
        ContentModuleIndex.eventBootstraps.forEach { bootstrap -> bootstrap() }
    }
}
