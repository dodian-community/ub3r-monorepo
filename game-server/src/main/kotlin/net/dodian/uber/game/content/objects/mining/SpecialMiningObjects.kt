package net.dodian.uber.game.content.objects.mining

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

object SpecialMiningObjects : ObjectContent {
    // Current Dodian mining parity does not include special mining object behavior.
    override val objectIds: IntArray = intArrayOf()

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean = false
}
