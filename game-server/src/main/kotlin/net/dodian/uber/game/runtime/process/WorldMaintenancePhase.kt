package net.dodian.uber.game.runtime.process

import net.dodian.jobs.impl.FarmingProcess
import net.dodian.jobs.impl.PlunderDoor
import net.dodian.jobs.impl.WorldProcessor

class WorldMaintenancePhase(
    private val worldProcessor: WorldProcessor,
    private val farmingProcess: FarmingProcess,
    private val plunderDoor: PlunderDoor,
) {
    private var lastPlunderRunMs = 0L

    fun run(cycle: Long, nowMs: Long) {
        if (cycle % 100L == 0L) {
            worldProcessor.run()
            farmingProcess.run()
        }

        if (lastPlunderRunMs == 0L || nowMs - lastPlunderRunMs >= PLUNDER_DOOR_INTERVAL_MS) {
            plunderDoor.run()
            lastPlunderRunMs = nowMs
        }
    }

    private companion object {
        private const val PLUNDER_DOOR_INTERVAL_MS = 900_000L
    }
}
