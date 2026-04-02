package net.dodian.uber.game.content.skills.mining

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

object GemRocksObjectBindings : ObjectContent {
    override val objectIds: IntArray = GemRocksObjectComponents.objectIds

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean = false
}
