package net.dodian.uber.game.engine.event.bootstrap

import net.dodian.uber.game.api.plugin.ContentModuleIndex

object CoreEventBusBootstrap {
    @JvmStatic
    fun bootstrap() {
        val bootstraps = ContentModuleIndex.eventBootstraps
        for (bootstrap in bootstraps) {
            bootstrap()
        }
    }

    @JvmStatic
    fun bootstrapCount(): Int {
        return ContentModuleIndex.eventBootstraps.size
    }
}
