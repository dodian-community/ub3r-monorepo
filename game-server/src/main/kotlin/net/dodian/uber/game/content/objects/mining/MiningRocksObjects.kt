package net.dodian.uber.game.content.objects.mining

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.interaction.ObjectInteractionPolicy
import net.dodian.uber.game.skills.mining.MiningDefinitions
import net.dodian.uber.game.skills.mining.MiningPlugin

object MiningRocksObjects : ObjectContent {
    override val objectIds: IntArray = MiningDefinitions.allRockObjectIds

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
        return MiningPlugin.attempt(client, objectId, position)
    }
}
