package net.dodian.uber.game.content.objects.smelting

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.utilities.Utils

object FurnaceObject : ObjectContent {
    override val objectIds: IntArray = intArrayOf(3994, 11666, 16469, 29662)

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        for (i in Utils.smelt_frame.indices) {
            client.sendFrame246(Utils.smelt_frame[i], 150, Utils.smelt_bars[i][0])
        }
        client.sendFrame164(2400)
        return true
    }

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        client.showItemsGold()
        client.showInterface(4161)
        return true
    }
}

