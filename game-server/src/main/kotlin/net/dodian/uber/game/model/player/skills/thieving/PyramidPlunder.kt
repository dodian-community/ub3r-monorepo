package net.dodian.uber.game.model.player.skills.thieving

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.utilities.Misc

class PyramidPlunder() {
    @JvmField var currentDoor: Position? = null
    @JvmField var allDoors: Array<Position> = arrayOf(Position(3288, 2799), Position(3293, 2794), Position(3288, 2789), Position(3283, 2794))
    @JvmField var nextRoom: IntArray = intArrayOf(-1, -1, -1, -1, -1, -1, -1)
    @JvmField var start: Position = Position(1927, 4477, 0)
    @JvmField var end: Position = Position(3289, 2801, 0)
    @JvmField var looting: Boolean = false
    @JvmField var obstacles: IntArray = intArrayOf(
        26616, 0, 26626, 0, 26618, 0, 26619, 0, 26620, 0, 26621, 0,
        26580, 0, 26600, 0, 26601, 0, 26602, 0, 26603, 0, 26604, 0,
        26605, 0, 26606, 0, 26607, 0, 26608, 0, 26609, 0, 26610, 0,
        26611, 0, 26612, 0, 26613, 0
    )
    @JvmField var urn_config: IntArray = intArrayOf(0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28)
    @JvmField var tomb_config: IntArray = intArrayOf(2, 0, 9, 10, 11, 12)

    private val room: Array<Position> = arrayOf(
        Position(1954, 4477, 0),
        Position(1977, 4471, 0),
        Position(1927, 4453, 0),
        Position(1965, 4444, 0),
        Position(1927, 4424, 0),
        Position(1943, 4421, 0),
        Position(1974, 4420, 0),
    )

    private var c: Client? = null
    private var ticks: Int = -1
    @JvmField var roomNr: Int = 0

    init {
        val doorCopy = allDoors.clone()
        currentDoor = doorCopy[Misc.random(allDoors.size - 1)]
        for (i in nextRoom.indices) {
            nextRoom[i] = Misc.random(3)
        }
    }

    constructor(c: Client) : this() {
        this.c = c
    }

    fun getEntryDoor(pos: Position): Boolean = currentDoor?.equals(pos) == true

    fun reduceTicks() {
        val client = c ?: return
        if (ticks == -1) return
        ticks--
        if (ticks == 0) {
            resetPlunder()
        } else if (ticks % 100 == 0) {
            client.send(SendMessage("You got ${ticks / 100} minute${if ((ticks / 100) == 1) "" else "s"} left."))
        }
    }

    fun startPlunder() {
        val client = c ?: return
        ticks = 500
        client.transport(start)
        client.send(SendMessage("Starting plunder test..."))
    }

    fun resetPlunder() {
        val client = c ?: return
        if (ticks == 0) client.send(SendMessage("You have run out time!"))
        ticks = -1
        client.transport(end)
        roomNr = 0
        resetObstacles()
    }

    fun toggleObstacles(`object`: Int) {
        val client = c ?: return
        if (looting) return
        var found = false
        for (i in tomb_config.size until tomb_config.size + urn_config.size) {
            if (found) break
            val checkSlot = obstacles.size / (tomb_config.size + urn_config.size)
            if (obstacles[i * checkSlot] == `object` && obstacles[i * checkSlot + 1] == 0) {
                obstacles[i * checkSlot + 1] = Misc.chance(2)
                displayUrns()
                found = true
            }
        }
        if (!found && obstacles[0] == `object`) {
            found = true
            if (obstacles[1] == 0) {
                obstacles[1] = 1
                displayTomb()
                client.send(SendMessage("Room: $roomNr trying to do gold chest!"))
            }
        }
        if (!found && obstacles[2] == `object`) {
            found = true
            if (obstacles[3] == 0) {
                obstacles[3] = 1
                displayTomb()
                client.send(SendMessage("Sarcophagus!"))
            }
        }
        for (i in 2 until tomb_config.size) {
            if (found) break
            val checkSlot = obstacles.size / (tomb_config.size + urn_config.size)
            if (obstacles[i * checkSlot] == `object` && obstacles[i * checkSlot + 1] == 0) {
                found = true
                obstacles[i * checkSlot + 1] = 1
                displayTomb()
            }
        }
    }

    fun openDoor(`object`: Int): Boolean {
        for (i in 2 until tomb_config.size) {
            val checkSlot = obstacles.size / (tomb_config.size + urn_config.size)
            if (obstacles[i * checkSlot] == `object` && obstacles[i * checkSlot + 1] == 1) return true
        }
        return false
    }

    fun resetObstacles() {
        val client = c ?: return
        client.varbit(820, 0)
        client.varbit(821, 0)
        for (i in 0 until tomb_config.size + urn_config.size) {
            val checkSlot = obstacles.size / (tomb_config.size + urn_config.size)
            obstacles[i * checkSlot + 1] = 0
        }
    }

    fun displayUrns() {
        val client = c ?: return
        var config = 0
        for (i in tomb_config.size until tomb_config.size + urn_config.size) {
            val checkSlot = obstacles.size / (tomb_config.size + urn_config.size)
            val slot = i - tomb_config.size
            if (obstacles[(i * checkSlot) + 1] == 1) config = config or (1 shl urn_config[slot])
            else if (obstacles[(i * checkSlot) + 1] == 2) config = config or (1 shl (urn_config[slot] + 1))
        }
        client.varbit(820, config)
    }

    fun displayTomb() {
        val client = c ?: return
        var config = 0
        for (i in tomb_config.indices) {
            val checkSlot = obstacles.size / (tomb_config.size + urn_config.size)
            if (obstacles[(i * checkSlot) + 1] == 1) config = config or (1 shl tomb_config[i])
        }
        client.varbit(821, config)
    }

    fun hinderTeleport(): Boolean = ticks != -1

    fun getRoomNr(): Int = roomNr

    fun nextRoom() {
        val client = c ?: return
        if (roomNr + 1 == 8) return
        val level = 31 + (roomNr * 10)
        if (client.getLevel(Skill.THIEVING) < level) {
            client.send(SendMessage("You need level $level thieving to enter next room!"))
            return
        }
        client.transport(room[roomNr])
        resetObstacles()
        roomNr++
    }
}
