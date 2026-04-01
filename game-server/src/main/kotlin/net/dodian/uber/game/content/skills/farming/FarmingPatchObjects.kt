package net.dodian.uber.game.content.skills.farming

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectBinding
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

object FarmingPatchObjects : ObjectContent {
    override val objectIds: IntArray = FarmingObjectComponents.patchObjects

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
