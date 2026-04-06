package net.dodian.uber.game.systems.net

import net.dodian.uber.game.model.entity.player.Client

object PacketGameplayFacade {
    @JvmStatic
    fun handleWalk(player: Client, request: WalkRequest) {
        PacketWalkingService.handle(player, request)
    }

    @JvmStatic
    fun rejectMalformedWalk(
        player: Client,
        opcode: Int,
        packetSize: Int,
        firstStepXAbs: Int,
        firstStepYAbs: Int,
        reason: String,
    ) {
        PacketWalkingService.rejectMalformedWalk(player, opcode, packetSize, firstStepXAbs, firstStepYAbs, reason)
    }
}
