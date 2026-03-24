package net.dodian.uber.game.content.items.prayer

import net.dodian.uber.game.content.items.ItemContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.prayer.Bones
import net.dodian.uber.game.skills.prayer.PrayerInteractionService

object BuryBonesItems : ItemContent {
    override val itemIds: IntArray = Bones.values().map { it.getItemId() }.toIntArray()

    override fun onFirstClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        return PrayerInteractionService.buryBones(client, itemId, itemSlot)
    }
}
