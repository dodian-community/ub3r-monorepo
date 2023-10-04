package net.dodian.uber.net.protocol.handlers

import com.github.michaelbull.logging.InlineLogger
import net.dodian.uber.game.modelkt.World
import net.dodian.uber.game.modelkt.entity.player.Player
import net.dodian.uber.net.protocol.packets.client.WalkMessage

private val logger = InlineLogger()

class WalkMessageHandler(world: World) : MessageHandler<WalkMessage>(world) {

    override fun handle(player: Player, message: WalkMessage) {
        val queue = player.walkingQueue

        val steps = message.steps
        for (index in steps.indices) {
            if (index == 0) queue.addFirstStep(steps[index])
            else queue.addStep(steps[index])
        }

        queue.running = message.isRunning || player.isRunning
        player.interfaceSet.close()

        if (queue.isNotEmpty())
            player.stopAction()

        if (player.hasInteractingMob)
            player.resetInteractingMob()
    }
}