package net.dodian.uber.game.sync.task

import net.dodian.uber.game.modelkt.entity.player.Player
import net.dodian.uber.net.protocol.packets.server.PlayerSynchronizationMessage

class PlayerSynchronizationTask(private val player: Player) : SynchronizationTask() {

    override fun run() {
        player.send(PlayerSynchronizationMessage(
            lastKnownRegion = player.lastKnownRegion ?: player.position,
            localPlayers = 0,
            position = player.position,
            hasRegionChanged = player.regionChanged
        ))
    }
}