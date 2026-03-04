package net.dodian.uber.game.content.objects.impl.mining

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

object MiningRocksObjects : ObjectContent {
    override val objectIds: IntArray = MiningDefinitions.standardRocks.map { it.objectId }.toIntArray()

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        val rock = MiningDefinitions.rocksByObjectId[objectId] ?: return false
        return MiningService.startMining(client, rock, position)
    }
}
