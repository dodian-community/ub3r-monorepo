package net.dodian.uber.game.content.objects.action2

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

interface ObjectAction2Content {
    val objectIds: IntArray


    fun onClick2(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean
}

