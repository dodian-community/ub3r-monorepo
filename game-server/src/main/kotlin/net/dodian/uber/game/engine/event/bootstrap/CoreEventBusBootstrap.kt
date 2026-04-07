package net.dodian.uber.game.engine.event.bootstrap

import net.dodian.uber.game.systems.dispatch.ContentModuleIndex

object CoreEventBusBootstrap {
    @JvmStatic
    fun bootstrap() {
        PacketGameplayEventWiring.bootstrap()
        ContentModuleIndex.eventBootstraps.forEach { bootstrap -> bootstrap() }
    }
}
