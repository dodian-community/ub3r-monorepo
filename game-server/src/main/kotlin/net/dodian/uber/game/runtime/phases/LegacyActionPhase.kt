package net.dodian.uber.game.runtime.phases

import kotlin.system.measureNanoTime
import net.dodian.jobs.impl.ActionProcessor
import net.dodian.jobs.impl.ItemProcessor
import net.dodian.jobs.impl.ObjectProcess
import net.dodian.jobs.impl.ShopProcessor
import net.dodian.utilities.runtimePhaseWarnMs
import org.slf4j.LoggerFactory

class LegacyActionPhase(
    private val actionProcessor: ActionProcessor,
    private val itemProcessor: ItemProcessor,
    private val shopProcessor: ShopProcessor,
    private val objectProcess: ObjectProcess,
) {
    private val logger = LoggerFactory.getLogger(LegacyActionPhase::class.java)

    fun run() {
        val actionNs = measureNanoTime { actionProcessor.run() }
        val itemNs = measureNanoTime { itemProcessor.run() }
        val shopNs = measureNanoTime { shopProcessor.run() }
        val objectNs = measureNanoTime { objectProcess.run() }
        val totalNs = actionNs + itemNs + shopNs + objectNs

        val totalMs = totalNs / 1_000_000L
        if (totalMs >= runtimePhaseWarnMs) {
            logger.warn(
                "Legacy actions slow: total={}ms action={}ms item={}ms shop={}ms object={}ms",
                totalMs,
                actionNs / 1_000_000L,
                itemNs / 1_000_000L,
                shopNs / 1_000_000L,
                objectNs / 1_000_000L,
            )
        }
    }
}
