package net.dodian.uber.game.content.objects.impl.farming

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectBinding
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.FarmingData

object FarmingPatchObjects : ObjectContent {
    private val patchIds: IntArray = FarmingData.patches.values()
        .flatMap { it.objectId.toList() }
        .distinct()
        .sorted()
        .toIntArray()

    override val objectIds: IntArray = patchIds

    override fun bindings(): List<ObjectBinding> {
        return objectIds.map { ObjectBinding(objectId = it, priority = 100) }
    }

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        return with(client.farming) { client.clickPatch(objectId) }
    }

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId == 7962) {
            return false
        }
        with(client.farming) { client.inspectPatch(objectId) }
        return true
    }
}
