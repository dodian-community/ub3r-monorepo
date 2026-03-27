package net.dodian.uber.game.content.skills.thieving.plunder

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.utilities.Misc

object PyramidPlunderService {
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
