package net.dodian.uber.game.content.objects.thieving

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.thieving.ThievingService
import net.dodian.uber.game.runtime.interaction.ObjectInteractionPolicy

object StallObjects : ObjectContent {
    override val objectIds: IntArray = ThievingObjectComponents.stallObjects

    override fun clickInteractionPolicy(
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): ObjectInteractionPolicy? {
        if (option != 2) {
            return null
        }
        return ObjectInteractionPolicy(
            distanceRule = ObjectInteractionPolicy.DistanceRule.NEAREST_BOUNDARY_CARDINAL,
            requireMovementSettled = true,
            settleTicks = 1,
        )
    }

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        ThievingService.attempt(client, objectId, position)
        return true
    }
}
