package net.dodian.uber.game.content.objects.travel

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.`object`.GlobalObject
import net.dodian.uber.game.model.`object`.Object as GameObject
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.systems.api.content.ContentInteraction
import net.dodian.utilities.Misc

object WebObstacleObjects : ObjectContent {
    override val objectIds: IntArray = TravelObjectComponents.webObstacleObjects

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (Misc.chance(100) <= 50) {
            client.send(SendMessage("You failed to cut the web!"))
            return true
        }
        if (!ContentInteraction.tryAcquireMs(client, ContentInteraction.WEB_OBSTACLE, 2000L)) {
            return true
        }
        val emptyObj = GameObject(734, position.x, position.y, client.position.z, 10, 1, objectId)
        if (!GlobalObject.addGlobalObject(emptyObj, 30000)) {
            return true
        }
        return true
    }
}
