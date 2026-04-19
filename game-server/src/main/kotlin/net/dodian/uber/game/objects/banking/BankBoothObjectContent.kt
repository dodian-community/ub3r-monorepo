package net.dodian.uber.game.objects.banking

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.api.content.ContentInteraction
import net.dodian.uber.game.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

object BankBoothObjectContent : ObjectContent {
    override val objectIds: IntArray = BankingObjectIds.boothObjects

    override fun clickInteractionPolicy(
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ) = when (option) {
        1, 2 -> ContentInteraction.nearestBoundaryCardinalPolicy()
        else -> null
    }

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

