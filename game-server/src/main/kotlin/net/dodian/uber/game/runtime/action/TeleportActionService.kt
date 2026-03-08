package net.dodian.uber.game.runtime.action

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client

object TeleportActionService {
    @JvmStatic
    fun startTeleport(
        client: Client,
        targetX: Int,
        targetY: Int,
        targetHeight: Int,
        emote: Int,
    ) {
        PlayerActionCancellationService.cancel(
            player = client,
            reason = PlayerActionCancelReason.TELEPORT,
            fullResetAnimation = false,
            resetLegacyState = true,
        )
        client.setTeleportStage(1)
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.TELEPORT,
            replaceReason = PlayerActionCancelReason.NEW_ACTION,
            interruptPolicy = PlayerActionInterruptPolicy.TELEPORT,
            onStop = { player, _ ->
                player.setTeleportStage(0)
                player.UsingAgility = false
            },
        ) {
            player.setTeleportStage(2)
            player.requestAnim(emote, 0)
            if (player.ancients == 1) {
                player.gfx0(392)
            }

            wait(2)
            if (!isActive()) {
                return@start
            }

            if (player.ancients != 1) {
                player.setTeleportStage(4)
                player.callGfxMask(308, 100)
            }

            wait(1)
            if (!isActive()) {
                return@start
            }

            player.setTeleportStage(5)
            player.transport(Position(targetX, targetY, targetHeight))
            player.requestAnim(715, 0)
            player.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
            player.GetBonus(true)
        }
    }
}
