package net.dodian.uber.game.content.skills.prayer

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.prayer.Bones
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.content.skills.prayer.PrayerOfferingRequest
import net.dodian.uber.game.content.skills.prayer.PrayerPlugin

object AltarObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(409, 20377)

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (client.currentPrayer < client.maxPrayer) {
            client.pray(client.maxPrayer)
            client.sendMessage("You restore your prayer points!")
        } else {
            client.sendMessage("You are at maximum prayer points!")
        }
        return true
    }

    override fun onUseItem(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        itemId: Int,
        itemSlot: Int,
        interfaceId: Int,
    ): Boolean {
        if (objectId != 409) {
            return false
        }
        if (Bones.getBone(itemId) == null || !client.playerHasItem(itemId) || client.randomed) {
            client.resetAction()
            return false
        }
        client.setInteractionAnchor(position.x, position.y, position.z)
        PrayerPlugin.startOffering(client, PrayerOfferingRequest(itemId, position.x, position.y))
        return true
    }
}
