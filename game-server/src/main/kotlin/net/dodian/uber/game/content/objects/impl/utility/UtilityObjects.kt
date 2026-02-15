package net.dodian.uber.game.content.objects.impl.utility

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.`object`.GlobalObject
import net.dodian.uber.game.model.`object`.Object as GameObject
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.utilities.Misc

object UtilityObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(733, 7962, 25824, 14889)

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId != 733) {
            return false
        }
        val chance = Misc.chance(100)
        if (chance <= 50) {
            client.send(SendMessage("You failed to cut the web!"))
            return true
        }
        if (System.currentTimeMillis() - client.lastAction < 2000) {
            client.lastAction = System.currentTimeMillis()
            return true
        }
        val emptyObj = GameObject(734, position.x, position.y, client.position.z, 10, 1, objectId)
        if (!GlobalObject.addGlobalObject(emptyObj, 30000)) {
            return true
        }
        client.lastAction = System.currentTimeMillis()
        return true
    }

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        return when (objectId) {
            7962 -> {
                client.send(SendMessage("You inspect the monolith, but can't make sense of the inscription."))
                true
            }
            25824, 14889 -> {
                client.spinning = true
                client.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
                true
            }
            else -> false
        }
    }
}
