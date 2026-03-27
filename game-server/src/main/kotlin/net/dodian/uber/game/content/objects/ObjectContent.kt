package net.dodian.uber.game.content.objects

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.api.content.ContentObjectInteractionPolicy

interface ObjectContent {
    val objectIds: IntArray
        get() = intArrayOf()

    fun bindings(): List<ObjectBinding> {
        return objectIds
            .sorted()
            .map { ObjectBinding(objectId = it) }
    }

    fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean = false
    fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean = false
    fun onThirdClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean = false
    fun onFourthClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean = false
    fun onFifthClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean = false
    fun onUseItem(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        itemId: Int,
        itemSlot: Int,
        interfaceId: Int,
    ): Boolean = false
    fun onMagic(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        spellId: Int,
    ): Boolean = false

    fun clickInteractionPolicy(
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): ContentObjectInteractionPolicy? = null

    fun itemOnObjectInteractionPolicy(
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        itemId: Int,
        itemSlot: Int,
        interfaceId: Int,
    ): ContentObjectInteractionPolicy? = null

    fun magicOnObjectInteractionPolicy(
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        spellId: Int,
    ): ContentObjectInteractionPolicy? = null
}
