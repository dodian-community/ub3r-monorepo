package net.dodian.uber.game.content.objects.action1.smelting

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.action1.ObjectAction1Content
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.utilities.Utils

object FurnaceSmelt : ObjectAction1Content {
    override val objectIds: IntArray = intArrayOf(3994, 11666, 16469, 29662)

    override fun onClick1(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        for (i in Utils.smelt_frame.indices) {
            client.sendFrame246(Utils.smelt_frame[i], 150, Utils.smelt_bars[i][0])
        }
        client.sendFrame164(2400)
        return true
    }
}

