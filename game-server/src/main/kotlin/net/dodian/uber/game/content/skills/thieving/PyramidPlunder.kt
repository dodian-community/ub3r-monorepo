package net.dodian.uber.game.content.skills.thieving

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.utilities.Misc

data class PyramidPlunderGlobalState(
    val allDoors: Array<Position> = arrayOf(Position(3288, 2799), Position(3293, 2794), Position(3288, 2789), Position(3283, 2794)),
    val nextRoom: IntArray = IntArray(7),
    val start: Position = Position(1927, 4477, 0),
    val end: Position = Position(3289, 2801, 0),
    var currentDoor: Position? = null,
) {
    val roomEntrances: Array<Position> = arrayOf(
        Position(1954, 4477, 0),
        Position(1977, 4471, 0),
        Position(1927, 4453, 0),
        Position(1965, 4444, 0),
        Position(1927, 4424, 0),
        Position(1943, 4421, 0),
        Position(1974, 4420, 0),
    )
}

data class PyramidPlunderPlayerState(
    var ticksRemaining: Int = -1,
    var roomNumber: Int = 0,
    var looting: Boolean = false,
    val obstacles: IntArray = intArrayOf(
        26616, 0, 26626, 0, 26618, 0, 26619, 0, 26620, 0, 26621, 0,
        26580, 0, 26600, 0, 26601, 0, 26602, 0, 26603, 0, 26604, 0,
        26605, 0, 26606, 0, 26607, 0, 26608, 0, 26609, 0, 26610, 0,
        26611, 0, 26612, 0, 26613, 0,
    ),
) {
    val urnConfig: IntArray = intArrayOf(0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28)
    val tombConfig: IntArray = intArrayOf(2, 0, 9, 10, 11, 12)
}

object PyramidPlunder {
    private val globalState =
        PyramidPlunderGlobalState().apply {
            initializeGlobalState(this)
        }

    @JvmStatic
    fun global(): PyramidPlunderGlobalState = globalState

    @JvmStatic
    fun tick(client: Client) {
        val state = client.pyramidPlunderState ?: return
        if (state.ticksRemaining == -1) {
            return
        }
        state.ticksRemaining--
        if (state.ticksRemaining == 0) {
            reset(client, timedOut = true)
        } else if (state.ticksRemaining % 100 == 0) {
            client.sendMessage("You got ${state.ticksRemaining / 100} minute${if ((state.ticksRemaining / 100) == 1) "" else "s"} left.")
        }
    }

    @JvmStatic
    fun start(client: Client) {
        val state = client.pyramidPlunderState ?: PyramidPlunderPlayerState()
        state.ticksRemaining = 500
        state.roomNumber = 0
        state.looting = false
        resetObstacles(client, state)
        client.pyramidPlunderState = state
        client.transport(globalState.start)
        client.sendMessage("Starting plunder test...")
    }

    @JvmOverloads
    @JvmStatic
    fun reset(client: Client, timedOut: Boolean = false) {
        val state = client.pyramidPlunderState ?: return
        if (timedOut) {
            client.sendMessage("You have run out time!")
        }
        state.ticksRemaining = -1
        state.roomNumber = 0
        state.looting = false
        resetObstacles(client, state)
        client.transport(globalState.end)
    }

    @JvmStatic
    fun isLooting(client: Client): Boolean = client.pyramidPlunderState?.looting == true

    @JvmStatic
    fun roomNumber(client: Client): Int = client.pyramidPlunderState?.roomNumber ?: 0

    @JvmStatic
    fun endPosition(client: Client): Position = globalState.end

    @JvmStatic
    fun currentDoor(): Position? = globalState.currentDoor

    @JvmStatic
    fun startPosition(): Position = globalState.start

    @JvmStatic
    fun isEntryDoor(position: Position): Boolean = globalState.currentDoor == position

    @JvmStatic
    fun nextRoomDoorFor(client: Client): Int = globalState.nextRoom[roomNumber(client)] + 26618

    @JvmStatic
    fun canOpenNextRoomDoor(client: Client, objectId: Int): Boolean =
        nextRoomDoorFor(client) == objectId && openDoor(client, objectId)

    @JvmStatic
    fun openDoor(client: Client, objectId: Int): Boolean {
        val state = client.pyramidPlunderState ?: return false
        val checkSlot = state.obstacles.size / (state.tombConfig.size + state.urnConfig.size)
        for (i in 2 until state.tombConfig.size) {
            if (state.obstacles[i * checkSlot] == objectId && state.obstacles[i * checkSlot + 1] == 1) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun advanceRoom(client: Client) {
        val state = client.pyramidPlunderState ?: return
        if (state.roomNumber + 1 == 8) {
            return
        }
        val level = 31 + (state.roomNumber * 10)
        if (client.getLevel(Skill.THIEVING) < level) {
            client.sendMessage("You need level $level thieving to enter next room!")
            return
        }
        client.transport(globalState.roomEntrances[state.roomNumber])
        resetObstacles(client, state)
        state.roomNumber++
    }

    @JvmStatic
    fun toggleObstacle(client: Client, objectId: Int) {
        val state = client.pyramidPlunderState ?: return
        if (state.looting) {
            return
        }
        var found = false
        val checkSlot = state.obstacles.size / (state.tombConfig.size + state.urnConfig.size)
        for (i in state.tombConfig.size until state.tombConfig.size + state.urnConfig.size) {
            if (found) {
                break
            }
            if (state.obstacles[i * checkSlot] == objectId && state.obstacles[i * checkSlot + 1] == 0) {
                state.obstacles[i * checkSlot + 1] = Misc.chance(2)
                displayUrns(client, state)
                found = true
            }
        }
        if (!found && state.obstacles[0] == objectId && state.obstacles[1] == 0) {
            state.obstacles[1] = 1
            displayTombs(client, state)
            client.sendMessage("Room: ${state.roomNumber} trying to do gold chest!")
            found = true
        }
        if (!found && state.obstacles[2] == objectId && state.obstacles[3] == 0) {
            state.obstacles[3] = 1
            displayTombs(client, state)
            client.sendMessage("Sarcophagus!")
            found = true
        }
        for (i in 2 until state.tombConfig.size) {
            if (found) {
                break
            }
            if (state.obstacles[i * checkSlot] == objectId && state.obstacles[i * checkSlot + 1] == 0) {
                state.obstacles[i * checkSlot + 1] = 1
                displayTombs(client, state)
                found = true
            }
        }
    }

    @JvmStatic
    fun hindersTeleport(client: Client): Boolean = (client.pyramidPlunderState?.ticksRemaining ?: -1) != -1

    @JvmStatic
    fun resetGlobalCycleState() {
        initializeGlobalState(globalState)
    }

    private fun initializeGlobalState(state: PyramidPlunderGlobalState) {
        state.currentDoor = state.allDoors[Misc.random(state.allDoors.size - 1)]
        for (i in state.nextRoom.indices) {
            state.nextRoom[i] = Misc.random(3)
        }
    }

    private fun resetObstacles(client: Client, state: PyramidPlunderPlayerState) {
        client.varbit(820, 0)
        client.varbit(821, 0)
        val checkSlot = state.obstacles.size / (state.tombConfig.size + state.urnConfig.size)
        for (i in 0 until state.tombConfig.size + state.urnConfig.size) {
            state.obstacles[i * checkSlot + 1] = 0
        }
    }

    private fun displayUrns(client: Client, state: PyramidPlunderPlayerState) {
        var config = 0
        val checkSlot = state.obstacles.size / (state.tombConfig.size + state.urnConfig.size)
        for (i in state.tombConfig.size until state.tombConfig.size + state.urnConfig.size) {
            val slot = i - state.tombConfig.size
            if (state.obstacles[i * checkSlot + 1] == 1) {
                config = config or (1 shl state.urnConfig[slot])
            } else if (state.obstacles[i * checkSlot + 1] == 2) {
                config = config or (1 shl (state.urnConfig[slot] + 1))
            }
        }
        client.varbit(820, config)
    }

    private fun displayTombs(client: Client, state: PyramidPlunderPlayerState) {
        var config = 0
        val checkSlot = state.obstacles.size / (state.tombConfig.size + state.urnConfig.size)
        for (i in state.tombConfig.indices) {
            if (state.obstacles[i * checkSlot + 1] == 1) {
                config = config or (1 shl state.tombConfig[i])
            }
        }
        client.varbit(821, config)
    }
}
