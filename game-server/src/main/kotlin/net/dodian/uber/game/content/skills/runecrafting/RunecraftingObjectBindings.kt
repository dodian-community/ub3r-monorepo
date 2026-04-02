package net.dodian.uber.game.content.skills.runecrafting

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.content.skills.runecrafting.RunecraftingData
import net.dodian.uber.game.content.skills.runecrafting.Runecrafting

object RunecraftingObjectBindings : ObjectContent {
    override val objectIds: IntArray = RunecraftingData.altarObjectIds

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        val altar = RunecraftingData.byObjectId(objectId) ?: return false
        return Runecrafting.start(client, altar.request)
    }
}
