package net.dodian.uber.game.content.objects.action2.smelting

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.action2.ObjectAction2Content
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

object FurnaceGoldClick : ObjectAction2Content {
    override val objectIds: IntArray = intArrayOf(3994, 11666, 16469, 29662)

    override fun onClick2(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        client.showItemsGold()
        client.showInterface(4161)
        return true
    }
}

