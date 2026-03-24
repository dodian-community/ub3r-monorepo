package net.dodian.uber.game.content.objects.runecrafting

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.runecrafting.RunecraftingDefinitions
import net.dodian.uber.game.skills.runecrafting.api.RunecraftingPlugin

object RunecraftingObjectBindings : ObjectContent {
    override val objectIds: IntArray = RunecraftingDefinitions.altarObjectIds

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        val altar = RunecraftingDefinitions.byObjectId(objectId) ?: return false
        return RunecraftingPlugin.start(client, altar.request)
    }
}
