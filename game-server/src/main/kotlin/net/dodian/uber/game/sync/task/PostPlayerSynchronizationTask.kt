package net.dodian.uber.game.sync.task

import net.dodian.uber.game.modelkt.entity.player.Player

class PostPlayerSynchronizationTask(
    private val player: Player
) : SynchronizationTask() {

    override fun run() {
        player.isTeleporting = false
        player.regionChanged = false
        player.resetBlockSet()

        // TODO: View distance decrement/increment
    }
}