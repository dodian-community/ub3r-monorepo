package net.dodian.uber.game.skills.thieving.plunder

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.thieving.PyramidPlunder

object PyramidPlunderService {
    private val globalState = PyramidPlunder()

    @JvmStatic
    fun global(): PyramidPlunder = globalState

    @JvmStatic
    fun tick(client: Client) {
        client.getPlunder.reduceTicks()
    }

    @JvmStatic
    fun start(client: Client) {
        client.getPlunder.startPlunder()
    }

    @JvmStatic
    fun reset(client: Client) {
        client.getPlunder.resetPlunder()
    }

    @JvmStatic
    fun isLooting(client: Client): Boolean = client.getPlunder.looting

    @JvmStatic
    fun roomNumber(client: Client): Int = client.getPlunder.roomNr

    @JvmStatic
    fun endPosition(client: Client): Position = client.getPlunder.end

    @JvmStatic
    fun currentDoor(): Position? = globalState.currentDoor

    @JvmStatic
    fun startPosition(): Position = globalState.start

    @JvmStatic
    fun isEntryDoor(position: Position): Boolean = globalState.getEntryDoor(position)

    @JvmStatic
    fun nextRoomDoorFor(client: Client): Int = globalState.nextRoom[client.getPlunder.roomNr] + 26618

    @JvmStatic
    fun canOpenNextRoomDoor(client: Client, objectId: Int): Boolean =
        nextRoomDoorFor(client) == objectId && client.getPlunder.openDoor(objectId)

    @JvmStatic
    fun openDoor(client: Client, objectId: Int): Boolean = client.getPlunder.openDoor(objectId)

    @JvmStatic
    fun advanceRoom(client: Client) {
        client.getPlunder.nextRoom()
    }

    @JvmStatic
    fun toggleObstacle(client: Client, objectId: Int) {
        client.getPlunder.toggleObstacles(objectId)
    }
}
