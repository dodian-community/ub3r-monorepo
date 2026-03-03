package net.dodian.uber.game.runtime.process

import net.dodian.jobs.impl.ActionProcessor
import net.dodian.jobs.impl.ItemProcessor
import net.dodian.jobs.impl.ObjectProcess
import net.dodian.jobs.impl.ShopProcessor

class LegacyActionPhase(
    private val actionProcessor: ActionProcessor,
    private val itemProcessor: ItemProcessor,
    private val shopProcessor: ShopProcessor,
    private val objectProcess: ObjectProcess,
) {
    fun run() {
        actionProcessor.run()
        itemProcessor.run()
        shopProcessor.run()
        objectProcess.run()
    }
}
