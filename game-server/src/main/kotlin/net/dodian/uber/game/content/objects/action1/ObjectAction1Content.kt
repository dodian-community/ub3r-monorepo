package net.dodian.uber.game.content.objects.action1

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

interface ObjectAction1Content {
    val objectIds: IntArray

    /**
     * Return true if handled (skip legacy click1 logic).
     */
    fun onClick1(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean
}

