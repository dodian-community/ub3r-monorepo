package net.dodian.uber.game.objects.doors

import net.dodian.uber.game.objects.ObjectBinding
import net.dodian.uber.game.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.objects.DoorRegistry
import net.dodian.uber.game.engine.systems.interaction.ObjectClipService
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry

import net.dodian.uber.game.api.interaction.ObjectInteractionContext

object DoorToggleObjectContent : ObjectContent {
    override fun bindings(): List<ObjectBinding> {
        val dynamicDoorBindings = DoorRegistry.doorId.indices
            .asSequence()
            .map { index ->
                val id = DoorRegistry.doorId[index]
                val x = DoorRegistry.doorX[index]
                val y = DoorRegistry.doorY[index]
                val z = DoorRegistry.doorHeight[index]
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

    override fun onFirstClick(context: ObjectInteractionContext): Boolean {
        val client = context.player
        val objectId = context.objectId
        val position = context.position
        if ((position.x == 2758 || position.x == 2757) && position.y == 3482 &&
            (objectId == 1558 || (objectId == 1557 && client.distanceToPoint(2758, 3482) < 5 && client.playerRights > 0))
        ) {
            client.ReplaceObject(2758, 3482, 1558, -2, 0)
            client.ReplaceObject(2757, 3482, 1557, 0, 0)
            client.sendMessage("Welcome to the Castle")
            return true
        }

        val matchingIndices = mutableListOf<Int>()
        for (index in DoorRegistry.doorId.indices) {
            if (DoorRegistry.doorId[index] == objectId &&
                DoorRegistry.doorX[index] == position.x &&
                DoorRegistry.doorY[index] == position.y &&
                DoorRegistry.doorHeight[index] == position.z
            ) {
                matchingIndices += index
            }
        }
        if (matchingIndices.isEmpty()) {
            return false
        }
        if (GameCycleClock.currentCycle() - client.contentRuntimeState.getLastDoorToggleCycle() <= 2) {
            return true
        }
        client.contentRuntimeState.setLastDoorToggleCycle(GameCycleClock.currentCycle())

        for (index in matchingIndices) {
            val newFace = if (DoorRegistry.doorState[index] == 0) {
                DoorRegistry.doorState[index] = 1
                DoorRegistry.doorFaceOpen[index]
            } else {
                DoorRegistry.doorState[index] = 0
                DoorRegistry.doorFaceClosed[index]
            }
            DoorRegistry.doorFace[index] = newFace
            ObjectClipService.applyDoor(
                position = Position(DoorRegistry.doorX[index], DoorRegistry.doorY[index], DoorRegistry.doorHeight[index]),
                objectId = DoorRegistry.doorId[index],
                face = newFace,
            )
            for (player in PlayerRegistry.players) {
                val other = player as? Client ?: continue
                if (other.playerName == null) continue
                if (other.position.z != client.position.z) continue
                if (other.disconnected || other.position.y <= 0 || other.position.x <= 0 || other.dbId <= 0) continue
                // ReplaceObject now guards range itself, but skip the call (and its SetMap/packet
                // cost) for players nowhere near this door - avoids an O(players) broadcast on
                // every toggle when only nearby viewers can ever see it anyway.
                if (other.distanceToPoint(DoorRegistry.doorX[index], DoorRegistry.doorY[index]) > 60) continue
                other.ReplaceObject(
                    DoorRegistry.doorX[index],
                    DoorRegistry.doorY[index],
                    DoorRegistry.doorId[index],
                    newFace,
                    0,
                )
            }
        }
        return true
    }
}
