package net.dodian.uber.game.skills.prayer

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.prayer.PrayerInteractionService
import net.dodian.uber.game.skills.prayer.PrayerOfferingRequest

object PrayerPlugin {
    @JvmStatic
    fun attempt(client: Client, itemId: Int, itemSlot: Int): Boolean =
        PrayerInteractionService.buryBones(client, itemId, itemSlot)

    @JvmStatic
    fun startOffering(client: Client, request: PrayerOfferingRequest) =
        PrayerInteractionService.startAltarOffering(client, request)
}
