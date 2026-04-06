package net.dodian.uber.game.systems.net

import net.dodian.uber.game.model.entity.player.Client

object PacketGameplayFacade {
    @JvmStatic
    fun handleWalk(player: Client?, request: WalkRequest?) {
        error(
            "PacketGameplayFacade.handleWalk is not wired yet; Task 3 will route walk packets into PacketWalkingService.",
        )
    }
}
