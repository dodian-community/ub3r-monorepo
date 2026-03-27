package net.dodian.uber.game.content.objects.mining

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.api.content.ContentInteraction
import net.dodian.uber.game.runtime.api.content.ContentObjectInteractionPolicy
import net.dodian.uber.game.skills.mining.MiningDefinitions
import net.dodian.uber.game.skills.mining.MiningPlugin

object MiningRocksObjects : ObjectContent {
    override val objectIds: IntArray = MiningDefinitions.allRockObjectIds

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
        return MiningPlugin.attempt(client, objectId, position)
    }
}
