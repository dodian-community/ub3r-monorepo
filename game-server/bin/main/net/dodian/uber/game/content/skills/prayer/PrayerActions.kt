package net.dodian.uber.game.content.skills.prayer

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.action.PlayerActionType
import net.dodian.uber.game.systems.action.playerAction

object PrayerActions {
    @JvmStatic
    fun startAltarOfferingAction(client: Client) {
        playerAction(
            player = client,
            type = PlayerActionType.ALTAR_BONES,
            actionName = "altar_bones",
            onStop = { player, _ ->
                player.clearPrayerOfferingState()
            },
        ) {
            while (player.prayerOfferingState != null) {
                val boneItemId = player.prayerOfferingState?.boneItemId ?: return@playerAction
                emitCycle("altar_bones")
                if (!Prayer.altarBones(player, boneItemId)) return@playerAction
                emitSuccess("altar_bones")
                waitTicks(3)
            }
        }
    }
}
