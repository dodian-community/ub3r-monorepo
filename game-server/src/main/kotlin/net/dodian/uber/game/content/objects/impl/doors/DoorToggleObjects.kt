package net.dodian.uber.game.content.objects.impl.doors

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectBinding
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.`object`.DoorHandler
import net.dodian.uber.game.netty.listener.out.SendMessage

object DoorToggleObjects : ObjectContent {
    override fun bindings(): List<ObjectBinding> {
        val dynamicDoorBindings = DoorHandler.doorId.indices
            .asSequence()
            .map { index ->
                val id = DoorHandler.doorId[index]
                val x = DoorHandler.doorX[index]
                val y = DoorHandler.doorY[index]
                val z = DoorHandler.doorHeight[index]
                if (id <= 0 || x <= 0 || y <= 0) {
                    null
                } else {
                    ObjectBinding.at(id, x, y, z, priority = 300)
                }
            }
            .filterNotNull()
            .distinct()
            .toMutableList()

        dynamicDoorBindings += ObjectBinding.at(1558, 2758, 3482, 0, priority = 400)
        dynamicDoorBindings += ObjectBinding.at(1557, 2757, 3482, 0, priority = 400)
        return dynamicDoorBindings
    }

    override val objectIds: IntArray
        get() = bindings()
            .map { it.objectId }
            .distinct()
            .sorted()
            .toIntArray()

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if ((position.x == 2758 || position.x == 2757) && position.y == 3482 &&
            (objectId == 1558 || (objectId == 1557 && client.distanceToPoint(2758, 3482) < 5 && client.playerRights > 0))
        ) {
            client.ReplaceObject(2758, 3482, 1558, -2, 0)
            client.ReplaceObject(2757, 3482, 1557, 0, 0)
            client.send(SendMessage("Welcome to the Castle"))
            return true
        }

        val matchingIndices = mutableListOf<Int>()
        for (index in DoorHandler.doorId.indices) {
            if (DoorHandler.doorId[index] == objectId &&
                DoorHandler.doorX[index] == position.x &&
                DoorHandler.doorY[index] == position.y &&
                DoorHandler.doorHeight[index] == position.z
            ) {
                matchingIndices += index
            }
        }
        if (matchingIndices.isEmpty()) {
            return false
        }
        if (System.currentTimeMillis() - client.lastDoor <= 1000) {
            return true
        }
        client.lastDoor = System.currentTimeMillis()

        for (index in matchingIndices) {
            val newFace = if (DoorHandler.doorState[index] == 0) {
                DoorHandler.doorState[index] = 1
                DoorHandler.doorFaceOpen[index]
            } else {
                DoorHandler.doorState[index] = 0
                DoorHandler.doorFaceClosed[index]
            }
            DoorHandler.doorFace[index] = newFace
            for (player in PlayerHandler.players) {
                val other = player as? Client ?: continue
                if (other.playerName == null) continue
                if (other.position.z != client.position.z) continue
                if (other.disconnected || other.position.y <= 0 || other.position.x <= 0 || other.dbId <= 0) continue
                other.ReplaceObject(
                    DoorHandler.doorX[index],
                    DoorHandler.doorY[index],
                    DoorHandler.doorId[index],
                    newFace,
                    0,
                )
            }
        }
        return true
    }
}
