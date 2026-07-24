package net.dodian.uber.game.skill.thieving

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.objects.GlobalObject
import net.dodian.uber.game.model.objects.WorldObject
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.audit.ItemLog
import net.dodian.uber.game.api.content.ContentInteraction
import net.dodian.uber.game.api.content.ContentObjectInteractionPolicy
import net.dodian.uber.game.api.content.ContentTiming
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.engine.systems.skills.ProgressionService
import net.dodian.uber.game.skill.runtime.action.SkillingRandomEventService
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.util.Misc
import net.dodian.utilities.Utils

object Thieving {
    const val PICKPOCKET_EMOTE: Int = 881
    const val STALL_THIEVING_EMOTE: Int = 832
    const val EMPTY_STALL_ID: Int = 634

    @JvmStatic
    fun attempt(player: Client, entityId: Int, position: Position) {
        val data = ThievingDefinition.forId(entityId) ?: return
        if (player.skillingEventState.isChestEventPendingMove) return
        val failChance = 0
        val face =
            if ((position.x == 2658 && position.y == 3297) || (position.x == 2663 && position.y == 3296)) 0
            else if ((position.x == 2655 && position.y == 3311) || (position.x == 2656 && position.y == 3302)) 1
            else if ((position.x == 2662 && position.y == 3314) || (position.x == 2657 && position.y == 3314)) 2
            else if ((position.x == 2667 && position.y == 3303) || (position.x == 2667 && position.y == 3310)) 3
            else -1
        if (face == -1 && data.type == ThievingType.STALL_THIEVING) {
            player.sendMessage("Not added object!")
            return
        }
        val emptyObject = WorldObject(EMPTY_STALL_ID, position.x, position.y, position.z, 10, face, data.entityId)
        if (player.getLevel(Skill.THIEVING) < data.requiredLevel) {
            player.sendMessage("You need a thieving level of ${data.requiredLevel} to steal from ${data.name.lowercase().replace('_', ' ')}s.")
            return
        }
        if (!ContentInteraction.tryAcquireMs(player, ContentInteraction.THIEVING_GENERIC, 2000L)) {
            return
        }
        if (data.type == ThievingType.PICKPOCKETING || data.type == ThievingType.OTHER) {
            player.setFocus(position.x, position.y)
            player.performAnimation(PICKPOCKET_EMOTE, 0)
            player.sendMessage("You attempt to steal from the ${data.name.lowercase().replace('_', ' ')}...")
        } else {
            if (GlobalObject.hasGlobalObject(emptyObject)) return
            player.performAnimation(STALL_THIEVING_EMOTE, 0)
        }

        ContentTiming.runLaterMs(600) {
            if (player.disconnected) return@runLaterMs
            if (failChance > 75) {
                player.sendMessage("You fail to thieve from the ${data.name.lowercase().replace('_', ' ')}")
                return@runLaterMs
            }
            if (!player.hasSpace()) {
                player.sendMessage("You don't have enough inventory space!")
                return@runLaterMs
            }
            ProgressionService.addXp(player, data.receivedExperience, Skill.THIEVING)
            if (data.item.size > 1) {
                val rollChance = (Math.random() * 100).toInt()
                for (i in data.item.indices) {
                    if (rollChance < data.itemChance[i]) {
                        val id = data.item[i]
                        val amount = data.itemAmount[i].value
                        player.addItem(id, amount)
                        ItemLog.playerGathering(player, id, amount, player.position.copy(), "Thieving")
                        player.sendMessage("You receive ${article(player.getItemName(id))} ${player.getItemName(id).lowercase()}")
                        break
                    }
                }
            } else {
                val id = data.item[0]
                val amount = data.itemAmount[0].value
                player.addItem(id, amount)
                ItemLog.playerGathering(player, id, amount, player.position.copy(), "Thieving")
                player.sendMessage("You receive ${article(player.getItemName(id))} ${player.getItemName(id).lowercase()}")
            }
            if (data.type == ThievingType.STALL_THIEVING) {
                val stallObject = WorldObject(EMPTY_STALL_ID, position.x, position.y, position.z, 10, face, data.entityId)
                GlobalObject.addGlobalObject(stallObject, data.respawnTime * 1000)
            }
            player.checkItemUpdate()
            SkillingRandomEventService.trigger(player, data.receivedExperience)
            val state = player.skillingEventState
            player.skillingEventState = state.withChestEventCount(state.chestEventCount + 1)
        }
    }

    private fun article(itemName: String): String =
        when {
            (itemName.startsWith("a") || itemName.startsWith("e") || itemName.startsWith("i") || itemName.startsWith("o") || itemName.startsWith("u")) && !itemName.endsWith("s") -> "an"
            itemName.endsWith("s") -> "some"
            else -> "a"
        }
}


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
    fun endPosition(): Position = globalState.end

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
