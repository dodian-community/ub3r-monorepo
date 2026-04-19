package net.dodian.uber.game.engine.phases

import net.dodian.uber.game.Constants
import net.dodian.uber.game.engine.sync.WorldSynchronizationService

open class OutboundPacketProcessor(
    private val syncRunner: (() -> Unit)? = null,
) : Runnable {
    override fun run() {
        runSynchronization()
    }

    protected open fun runSynchronization() {
        val runner = syncRunner
        if (runner != null) {
            runner()
            return
        }
        WorldSynchronizationService.INSTANCE.run()
    }
}
