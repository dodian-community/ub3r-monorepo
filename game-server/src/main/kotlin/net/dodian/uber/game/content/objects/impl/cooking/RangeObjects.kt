package net.dodian.uber.game.content.objects.impl.cooking

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

object RangeObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(26181, 2728, 2781)

    override fun onUseItem(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        itemId: Int,
        itemSlot: Int,
        interfaceId: Int,
    ): Boolean {
        if (objectId == 26181 && itemId == 401) {
            val amount = client.getInvAmt(401)
            for (i in 0 until amount) {
                client.deleteItem(401, 1)
                client.addItem(1781, 1)
            }
            client.checkItemUpdate()
            client.send(SendMessage("You burn all your seaweed into ashes."))
            return true
        }

        client.skillX = position.x
        client.setSkillY(position.y)
        client.startCooking(itemId)
        return true
    }
}
