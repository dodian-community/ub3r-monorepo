package net.dodian.uber.game.content.objects.impl.banking

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

object BankBoothObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(2213, 2214, 3045, 5276, 6084)

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        val objectName = obj?.name?.lowercase() ?: ""
        if (objectName.startsWith("bank") || objectName.contains("bank")) {
            client.openUpBank()
        }
        client.skillX = position.x
        client.setSkillY(position.y)
        client.WanneBank = 1
        client.WanneShop = -1
        return true
    }

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        client.skillX = position.x
        client.setSkillY(position.y)
        client.WanneBank = 1
        client.WanneShop = -1
        return true
    }

    override fun onThirdClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        client.setRefundList()
        client.refundSlot = 0
        client.setRefundOptions()
        return true
    }

    override fun onFourthClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        client.send(SendMessage("This bank options are not working currently!"))
        return true
    }
}
