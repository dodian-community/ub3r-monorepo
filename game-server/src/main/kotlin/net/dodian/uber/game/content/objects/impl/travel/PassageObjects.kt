package net.dodian.uber.game.content.objects.impl.travel

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

object PassageObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(23271)

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId == 23271) {
            client.transport(Position(position.x, position.y + if (client.position.y == 3523) -1 else 2, position.z))
            return true
        }
        return false
    }
}
