package net.dodian.uber.game.content.objects.woodcutting

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.interaction.ObjectInteractionPolicy
import net.dodian.uber.game.skills.woodcutting.WoodcuttingDefinitions
import net.dodian.uber.game.skills.woodcutting.api.WoodcuttingPlugin

object WoodcuttingTreesObjects : ObjectContent {
    override val objectIds: IntArray = WoodcuttingDefinitions.allTreeObjectIds

    override fun clickInteractionPolicy(
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): ObjectInteractionPolicy? {
        if (option != 1) {
            return null
        }
        return ObjectInteractionPolicy(
            distanceRule = ObjectInteractionPolicy.DistanceRule.NEAREST_BOUNDARY_CARDINAL,
            requireMovementSettled = true,
            settleTicks = 1,
        )
    }

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        return WoodcuttingPlugin.attempt(client, objectId, position, obj)
    }
}
