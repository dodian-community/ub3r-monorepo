package net.dodian.uber.game.content.objects.impl.mining

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.interaction.ObjectInteractionPolicy
import net.dodian.uber.game.skills.mining.MiningData
import net.dodian.uber.game.skills.mining.MiningService

object MiningRocksObjects : ObjectContent {
    override val objectIds: IntArray = MiningData.allRockObjectIds

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
        val rock = MiningData.rockByObjectId[objectId] ?: return false
        return MiningService.startMining(client, rock, position)
    }
}
