package net.dodian.uber.game.content.objects

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

interface ObjectContent {
    val objectIds: IntArray

    fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean = false
    fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean = false
    fun onThirdClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean = false
    fun onFourthClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean = false
    fun onFifthClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean = false
}

