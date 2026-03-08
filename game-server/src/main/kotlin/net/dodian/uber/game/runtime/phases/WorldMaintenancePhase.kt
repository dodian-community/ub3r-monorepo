package net.dodian.uber.game.runtime.phases

import net.dodian.jobs.impl.ActionProcessor
import net.dodian.jobs.impl.ItemProcessor
import net.dodian.jobs.impl.PlunderDoor
import net.dodian.jobs.impl.ShopProcessor
import net.dodian.uber.game.runtime.world.WorldMaintenanceService

class WorldMaintenancePhase(
    private val plunderDoor: PlunderDoor,
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

    fun runWorldDbInputBuild(cycle: Long) {
        service.runWorldDbInputBuild(cycle)
    }

    fun runWorldDbResultRead(cycle: Long) {
        service.runWorldDbResultRead(cycle)
    }

    fun runWorldDbApply(cycle: Long) {
        service.runWorldDbApply(cycle)
    }

    fun runFarming(cycle: Long) {
        service.runFarming(cycle)
    }

    fun runPlunder(nowMs: Long) {
        service.runPlunder(nowMs)
    }
}
