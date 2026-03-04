package net.dodian.uber.game.runtime.process

import net.dodian.jobs.impl.FarmingProcess
import net.dodian.jobs.impl.PlunderDoor
import net.dodian.jobs.impl.WorldProcessor
import net.dodian.uber.game.runtime.world.WorldMaintenanceService

class WorldMaintenancePhase(
    private val worldProcessor: WorldProcessor,
    private val farmingProcess: FarmingProcess,
    private val plunderDoor: PlunderDoor,
) {
    private val service = WorldMaintenanceService(worldProcessor, farmingProcess, plunderDoor)

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
