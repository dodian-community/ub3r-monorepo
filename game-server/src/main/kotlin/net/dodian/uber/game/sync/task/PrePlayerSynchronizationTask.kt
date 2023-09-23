package net.dodian.uber.game.sync.task

import net.dodian.cache.region.RegionCoordinates
import net.dodian.uber.game.modelkt.Position
import net.dodian.uber.game.modelkt.entity.Player

class PrePlayerSynchronizationTask(
    private val player: Player
) : SynchronizationTask() {

    override fun run() {

    }

    private fun sendUpdates(position: Position, differences: Set<RegionCoordinates>, full: Set<RegionCoordinates>) {

    }
}