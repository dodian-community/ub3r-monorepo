package net.dodian.uber.game.content.objects.impl.prayer

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.prayer.Prayer
import net.dodian.uber.game.netty.listener.out.SendMessage

object AltarObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(409, 20377)

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (client.currentPrayer < client.maxPrayer) {
            client.pray(client.maxPrayer)
            client.send(SendMessage("You restore your prayer points!"))
        } else {
            client.send(SendMessage("You are at maximum prayer points!"))
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
        if (!Prayer.altarBones(client, itemId)) {
            return false
        }
        client.skillX = position.x
        client.setSkillY(position.y)
        client.boneItem = itemId
        Prayer.altarBones(client, client.boneItem)
        return true
    }
}
