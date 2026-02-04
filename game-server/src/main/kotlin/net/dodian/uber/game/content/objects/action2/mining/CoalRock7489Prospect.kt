package net.dodian.uber.game.content.objects.action2.mining

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.action2.ObjectAction2Content
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

object CoalRock7489Prospect : ObjectAction2Content {
    override val objectIds: IntArray = intArrayOf(7489)

    override fun onClick2(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        client.send(SendMessage("This rock contains coal."))
        return true
    }
}

