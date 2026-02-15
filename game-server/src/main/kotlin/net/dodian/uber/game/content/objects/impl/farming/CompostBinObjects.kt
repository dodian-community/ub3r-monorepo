package net.dodian.uber.game.content.objects.impl.farming

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectBinding
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.FarmingData

object CompostBinObjects : ObjectContent {
    private val compostBinIds: IntArray = FarmingData.compostBin.values()
        .map { it.objectId }
        .distinct()
        .sorted()
        .toIntArray()

    override val objectIds: IntArray = compostBinIds

    override fun bindings(): List<ObjectBinding> {
        return objectIds.map { ObjectBinding(objectId = it, priority = 100) }
    }

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        with(client.farming) { client.interactBin(objectId, 1) }
        return true
    }

    override fun onFifthClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        with(client.farming) { client.interactBin(objectId, 5) }
        return true
    }

    override fun onUseItem(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        itemId: Int,
        itemSlot: Int,
        interfaceId: Int,
    ): Boolean {
        return with(client.farming) { client.interactItemBin(objectId, itemId) }
    }
}
