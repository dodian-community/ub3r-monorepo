package net.dodian.uber.game.skill.prayer

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.action.PlayerActionType
import net.dodian.uber.game.engine.systems.action.playerAction

object PrayerActions {
    @JvmStatic
    fun startAltarOfferingAction(client: Client) {
        playerAction(
            player = client,
            type = PlayerActionType.ALTAR_BONES,
            actionName = PrayerActionIds.ALTAR_BONES,
            onStop = { player, _ ->
                player.clearPrayerOfferingState()
            },
        ) {
            while (player.prayerOfferingState != null) {
                val boneItemId = player.prayerOfferingState?.boneItemId ?: return@playerAction
                emitCycle(PrayerActionIds.ALTAR_BONES)
                if (!Prayer.altarBones(player, boneItemId)) return@playerAction
                emitSuccess(PrayerActionIds.ALTAR_BONES)
                waitTicks(3)
            }
        }
    }
}
