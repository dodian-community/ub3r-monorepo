package net.dodian.uber.game.content.skills.woodcutting

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.api.content.ContentInteraction
import net.dodian.uber.game.systems.api.content.ContentObjectInteractionPolicy
import net.dodian.uber.game.content.skills.woodcutting.WoodcuttingDefinitions
import net.dodian.uber.game.content.skills.woodcutting.WoodcuttingPlugin

object WoodcuttingTreesObjects : ObjectContent {
    override val objectIds: IntArray = WoodcuttingDefinitions.allTreeObjectIds

    override fun clickInteractionPolicy(
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): ContentObjectInteractionPolicy? {
        if (option != 1) {
            return null
        }
        return ContentInteraction.nearestBoundaryCardinalPolicy()
    }

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        return WoodcuttingPlugin.attempt(client, objectId, position, obj)
    }
}
