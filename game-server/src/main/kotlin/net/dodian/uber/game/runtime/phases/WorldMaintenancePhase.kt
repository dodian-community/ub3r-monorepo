package net.dodian.uber.game.runtime.phases

import net.dodian.jobs.impl.PlunderDoor
import net.dodian.uber.game.runtime.world.WorldMaintenanceService

class WorldMaintenancePhase(
    private val plunderDoor: PlunderDoor,
) {
    private val service = WorldMaintenanceService(plunderDoor)

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
