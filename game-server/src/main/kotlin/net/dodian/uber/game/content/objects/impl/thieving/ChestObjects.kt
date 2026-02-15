package net.dodian.uber.game.content.objects.impl.thieving

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.thieving.Thieving
import net.dodian.uber.game.netty.listener.out.SendMessage

object ChestObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(378, 20873, 11729, 11730, 11731, 11732, 11733, 11734)

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId == 20873) {
            Thieving.attemptSteal(client, objectId, position)
            return true
        }
        return false
    }

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        return when (objectId) {
            378 -> {
                client.send(SendMessage("This chest is empty!"))
                true
            }
            20873, 11729, 11730, 11731, 11732, 11733, 11734 -> {
                Thieving.attemptSteal(client, objectId, position)
                true
            }
            else -> false
        }
    }
}
