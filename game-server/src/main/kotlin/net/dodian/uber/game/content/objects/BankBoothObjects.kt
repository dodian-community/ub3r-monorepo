package net.dodian.uber.game.content.objects

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

object BankBoothObjects : ObjectContent {
    override val objectIds: IntArray = BankingObjectComponents.boothObjects

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        client.openUpBankRouted()
        return true
    }

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        client.openUpBankRouted()
        return true
    }

    override fun onThirdClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        client.setRefundList()
        client.refundSlot = 0
        client.setRefundOptions()
        return true
    }

    override fun onFourthClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        client.sendMessage("This bank options are not working currently!")
        return true
    }
}
