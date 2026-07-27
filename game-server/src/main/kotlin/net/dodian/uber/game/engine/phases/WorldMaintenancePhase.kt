package net.dodian.uber.game.engine.phases

import net.dodian.uber.game.engine.processing.ActionProcessor
import net.dodian.uber.game.engine.processing.ItemProcessor
import net.dodian.uber.game.engine.processing.PlunderDoorProcessor
import net.dodian.uber.game.engine.processing.ShopProcessor
import net.dodian.uber.game.engine.systems.world.WorldMaintenanceService
import net.dodian.uber.game.model.objects.GlobalObject

class WorldMaintenancePhase(
    private val plunderDoor: PlunderDoorProcessor,
    private val actionProcessor: ActionProcessor,
    private val itemProcessor: ItemProcessor,
    private val shopProcessor: ShopProcessor,
) {
    private val service = WorldMaintenanceService(plunderDoor)

    fun runWorldTasks() {
        actionProcessor.run()
    }

    fun runGroundItems() {
        itemProcessor.run()
    }

    fun runShops() {
        shopProcessor.run()
    }

    /** Single world-level sweep of expired dynamic objects - see [GlobalObject.sweepExpired]. */
    fun runGlobalObjectSweep() {
        GlobalObject.sweepExpired()
    }

    fun runWorldDbInputBuild(cycle: Long) {
        service.runWorldDbInputBuild(cycle)
    }

    fun runWorldDbResultRead(cycle: Long) {
        service.runWorldDbResultRead(cycle)
    }

    fun runWorldDbApply(cycle: Long) {
        service.runWorldDbApply(cycle)
    }


    fun runPlunder(nowMs: Long) {
        service.runPlunder(nowMs)
    }
}