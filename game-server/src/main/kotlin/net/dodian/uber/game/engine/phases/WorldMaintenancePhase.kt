package net.dodian.uber.game.engine.phases

import net.dodian.uber.game.engine.processing.ActionProcessor
import net.dodian.uber.game.engine.processing.ItemProcessor
import net.dodian.uber.game.engine.processing.PlunderDoorProcessor
import net.dodian.uber.game.engine.processing.ShopProcessor
import net.dodian.uber.game.systems.world.WorldMaintenanceService

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
