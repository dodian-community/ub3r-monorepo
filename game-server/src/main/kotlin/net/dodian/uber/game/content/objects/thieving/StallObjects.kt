package net.dodian.uber.game.content.objects.thieving

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.api.content.ContentInteraction
import net.dodian.uber.game.runtime.api.content.ContentObjectInteractionPolicy
import net.dodian.uber.game.skills.thieving.ThievingPlugin

object StallObjects : ObjectContent {
    override val objectIds: IntArray = ThievingObjectComponents.stallObjects

    override fun clickInteractionPolicy(
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): ContentObjectInteractionPolicy? {
        if (option != 2) {
            return null
        }
        return ContentInteraction.nearestBoundaryCardinalPolicy()
    }

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        ThievingPlugin.attempt(client, objectId, position)
        return true
    }
}
