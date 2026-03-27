package net.dodian.uber.game.content.objects.banking

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

object BankChestObjects : ObjectContent {
    override val objectIds: IntArray = BankingObjectComponents.chestObjects

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        client.openUpBankRouted()
        return true
    }

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId == 6943 || objectId == 6948) {
            client.openUpBankRouted()
        }
        return true
    }
}
