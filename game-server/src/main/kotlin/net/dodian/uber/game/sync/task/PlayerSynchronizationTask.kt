package net.dodian.uber.game.sync.task

import net.dodian.uber.game.modelkt.Position
import net.dodian.uber.game.modelkt.entity.Player
import net.dodian.uber.net.protocol.packets.server.PlayerSynchronization

class PlayerSynchronizationTask(private val player: Player) : SynchronizationTask() {

    override fun run() {
        player.send(PlayerSynchronization(
            lastKnownRegion = Position(),
            localPlayers = 0,
            position = Position(),
            hasRegionChanged = false
        ))
    }
}