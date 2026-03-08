package net.dodian.uber.game.content.objects

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.interaction.ObjectInteractionPolicy

class ObjectContentBuilder {
    private val bindings = mutableListOf<ObjectBinding>()
    private var onFirstClick: (Client, Int, Position, GameObjectData?) -> Boolean = { _, _, _, _ -> false }
    private var onSecondClick: (Client, Int, Position, GameObjectData?) -> Boolean = { _, _, _, _ -> false }
    private var onThirdClick: (Client, Int, Position, GameObjectData?) -> Boolean = { _, _, _, _ -> false }
    private var onFourthClick: (Client, Int, Position, GameObjectData?) -> Boolean = { _, _, _, _ -> false }
    private var onFifthClick: (Client, Int, Position, GameObjectData?) -> Boolean = { _, _, _, _ -> false }
    private var onUseItem:
        (Client, Int, Position, GameObjectData?, Int, Int, Int) -> Boolean =
        { _, _, _, _, _, _, _ -> false }
    private var onMagic:
        (Client, Int, Position, GameObjectData?, Int) -> Boolean =
        { _, _, _, _, _ -> false }
    private var clickPolicy: (Int, Int, Position, GameObjectData?) -> ObjectInteractionPolicy? = { _, _, _, _ -> null }

    fun bind(vararg objectIds: Int) {
        objectIds.sorted().forEach { objectId -> bindings += ObjectBinding(objectId = objectId) }
    }

    fun onFirstClick(handler: (Client, Int, Position, GameObjectData?) -> Boolean) {
        onFirstClick = handler
    }

    fun onSecondClick(handler: (Client, Int, Position, GameObjectData?) -> Boolean) {
        onSecondClick = handler
    }

    fun onThirdClick(handler: (Client, Int, Position, GameObjectData?) -> Boolean) {
        onThirdClick = handler
    }

    fun onFourthClick(handler: (Client, Int, Position, GameObjectData?) -> Boolean) {
        onFourthClick = handler
    }

    fun onFifthClick(handler: (Client, Int, Position, GameObjectData?) -> Boolean) {
        onFifthClick = handler
    }

    fun onUseItem(handler: (Client, Int, Position, GameObjectData?, Int, Int, Int) -> Boolean) {
        onUseItem = handler
    }

    fun onMagic(handler: (Client, Int, Position, GameObjectData?, Int) -> Boolean) {
        onMagic = handler
    }

    fun clickInteractionPolicy(handler: (Int, Int, Position, GameObjectData?) -> ObjectInteractionPolicy?) {
        clickPolicy = handler
    }

    fun build(): ObjectContent {
        val resolvedBindings = bindings.toList()
        return object : ObjectContent {
            override fun bindings(): List<ObjectBinding> = resolvedBindings

            override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean =
                onFirstClick(client, objectId, position, obj)

            override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean =
                onSecondClick(client, objectId, position, obj)

            override fun onThirdClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean =
                onThirdClick(client, objectId, position, obj)

            override fun onFourthClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean =
                onFourthClick(client, objectId, position, obj)

            override fun onFifthClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean =
                onFifthClick(client, objectId, position, obj)

            override fun onUseItem(
                client: Client,
                objectId: Int,
                position: Position,
                obj: GameObjectData?,
                itemId: Int,
                itemSlot: Int,
                interfaceId: Int,
            ): Boolean = onUseItem(client, objectId, position, obj, itemId, itemSlot, interfaceId)

            override fun onMagic(
                client: Client,
                objectId: Int,
                position: Position,
                obj: GameObjectData?,
                spellId: Int,
            ): Boolean = onMagic(client, objectId, position, obj, spellId)

            override fun clickInteractionPolicy(
                option: Int,
                objectId: Int,
                position: Position,
                obj: GameObjectData?,
            ): ObjectInteractionPolicy? = clickPolicy(option, objectId, position, obj)
        }
    }
}

fun objectContent(init: ObjectContentBuilder.() -> Unit): ObjectContent {
    return ObjectContentBuilder().apply(init).build()
}
