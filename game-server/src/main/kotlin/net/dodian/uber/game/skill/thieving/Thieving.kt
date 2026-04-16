package net.dodian.uber.game.skill.thieving

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.objects.ObjectContent
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
            player.canPreformAction = false
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

private class StallObjectContent : ObjectContent {
    override val objectIds: IntArray = ThievingObjectComponents.stallObjects

    override fun clickInteractionPolicy(
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): ContentObjectInteractionPolicy? {
        if (option != 2) {
            return null
        }
        return ContentInteraction.nearestBoundaryCardinalPolicy()
    }

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        Thieving.attempt(client, objectId, position)
        return true
    }
}

private class ChestObjectContent : ObjectContent {
    override val objectIds: IntArray = ThievingObjectComponents.chestObjects

    override fun clickInteractionPolicy(
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): ContentObjectInteractionPolicy? {
        if (option != 1 && option != 2) {
            return null
        }
        return ContentInteraction.nearestBoundaryCardinalPolicy()
    }

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId == 20873 || objectId == 20885 || objectId == 6847) {
            Thieving.attempt(client, objectId, position)
            return true
        }
        if (objectId == 375 && position.x == 2593 && position.y == 3108 && client.position.z == 1) {
            if (client.skillingEventState.isChestEventPendingMove) {
                return true
            }
            if (client.getLevel(Skill.THIEVING) < 70) {
                client.sendMessage("You must be level 70 thieving to open this chest")
                return true
            }
            if (client.freeSlots() < 1) {
                client.sendMessage("You need atleast one free inventory slot!")
                return true
            }
            if (!ContentInteraction.tryAcquireMs(client, ContentInteraction.YANILLE_CHEST, 1200L)) {
                return true
            }
            val emptyObj = WorldObject(378, position.x, position.y, client.position.z, 10, 2, objectId)
            if (!GlobalObject.addGlobalObject(emptyObj, 12000)) {
                return true
            }
            val roll = Math.random() * 100
            if (roll <= 0.3) {
                val items = intArrayOf(2577, 2579, 2631)
                val itemId = items[(Math.random() * items.size).toInt()]
                client.sendMessage("You have recieved a ${client.getItemName(itemId)}!")
                client.addItem(itemId, 1)
                ItemLog.playerGathering(client, itemId, 1, client.position.copy(), "Thieving")
                client.yell("[Server] - ${client.playerName} has just received from the Yanille chest a  ${client.getItemName(itemId)}")
            } else {
                val coins = 300 + Utils.random(1200)
                client.sendMessage("You find $coins coins inside the chest")
                client.addItem(995, coins)
                ItemLog.playerGathering(client, 995, coins, client.position.copy(), "Thieving")
            }
            if (client.equipment[Equipment.Slot.HEAD.id] == 2631) {
                ProgressionService.addXp(client, 300, Skill.THIEVING)
            }
            client.checkItemUpdate()
            val state = client.skillingEventState
            client.skillingEventState = state.withChestEventCount(state.chestEventCount + 1)
            client.stillgfx(444, position.y, position.x)
            SkillingRandomEventService.trigger(client, 900)
            return true
        }
        if (objectId == 375 && position.x == 2733 && position.y == 3374) {
            if (client.skillingEventState.isChestEventPendingMove) {
                return true
            }
            if (!client.premium) {
                client.resetPos()
                return true
            }
            if (client.getLevel(Skill.THIEVING) < 85) {
                client.sendMessage("You must be level 85 thieving to open this chest")
                return true
            }
            if (client.freeSlots() < 1) {
                client.sendMessage("You need atleast one free inventory slot!")
                return true
            }
            if (!ContentInteraction.tryAcquireMs(client, ContentInteraction.LEGENDS_CHEST, 1200L)) {
                return true
            }
            val emptyObj = WorldObject(378, position.x, position.y, position.z, 11, -1, objectId)
            if (!GlobalObject.addGlobalObject(emptyObj, 15000)) {
                return true
            }
            val roll = Math.random() * 100
            if (roll <= 0.3) {
                val items = intArrayOf(1050, 2581, 2631)
                val itemId = items[(Math.random() * items.size).toInt()]
                client.sendMessage("You have recieved a ${client.getItemName(itemId)}!")
                client.addItem(itemId, 1)
                ItemLog.playerGathering(client, itemId, 1, client.position.copy(), "Thieving")
                client.yell("[Server] - ${client.playerName} has just received from the Legends chest a  ${client.getItemName(itemId)}")
            } else {
                val coins = 500 + Utils.random(2000)
                client.sendMessage("You find $coins coins inside the chest")
                client.addItem(995, coins)
                ItemLog.playerGathering(client, 995, coins, client.position.copy(), "Thieving")
            }
            if (client.equipment[Equipment.Slot.HEAD.id] == 2631) {
                ProgressionService.addXp(client, 500, Skill.THIEVING)
            }
            client.checkItemUpdate()
            val state = client.skillingEventState
            client.skillingEventState = state.withChestEventCount(state.chestEventCount + 1)
            client.stillgfx(444, position.y, position.x)
            SkillingRandomEventService.trigger(client, 1500)
            return true
        }
        return false
    }

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        return when (objectId) {
            378 -> {
                client.sendMessage("This chest is empty!")
                true
            }
            20873, 20885, 11729, 11730, 11731, 11732, 11733, 11734 -> {
                Thieving.attempt(client, objectId, position)
                true
            }
            else -> false
        }
    }
}

private class PlunderObjectContent : ObjectContent {
    override val objectIds: IntArray = ThievingObjectComponents.plunderObjects

    override fun clickInteractionPolicy(
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): ContentObjectInteractionPolicy? {
        if (option != 1 && option != 2) {
            return null
        }
        return ContentInteraction.nearestBoundaryCardinalPolicy()
    }

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        return when {
            objectId in 26622..26625 -> {
                if (client.getLevel(Skill.THIEVING) < 21 || client.stunTimer > 0) {
                    client.send(
                        SendMessage(
                            if (client.getLevel(Skill.THIEVING) < 21) {
                                "You need level 21 thieving to enter."
                            } else {
                                "You are stunned!"
                            },
                        ),
                    )
                    return true
                }
                if (PyramidPlunder.isEntryDoor(position)) {
                    val chance = Misc.random(255)
                    if (chance <= (client.getLevel(Skill.THIEVING) * 2.5).toInt()) {
                        client.transport(Position(1934, 4450, 2))
                    } else {
                        client.dealDamage(null, Misc.random(3), Entity.hitType.STANDARD)
                        client.stunTimer = 4
                    }
                } else {
                    client.transport(Position(1968, 4420, 2))
                }
                true
            }
            objectId in 26618..26621 -> {
                if (PyramidPlunder.roomNumber(client) + 1 == 8) {
                    return true
                }
                if (PyramidPlunder.canOpenNextRoomDoor(client, objectId)) {
                    PyramidPlunder.advanceRoom(client)
                } else if (PyramidPlunder.openDoor(client, objectId)) {
                    client.sendMessage("This tomb door lead nowhere.")
                } else {
                    PyramidPlunder.toggleObstacle(client, objectId)
                }
                true
            }
            objectId == 20932 -> {
                client.transport(PyramidPlunder.endPosition())
                true
            }
            objectId == 20931 -> {
                DialogueService.setDialogueId(client, 20931)
                DialogueService.setDialogueSent(client, false)
                true
            }
            objectId == 26616 || objectId == 26626 -> {
                PyramidPlunder.toggleObstacle(client, objectId)
                true
            }
            objectId == 26580 || objectId in 26600..26613 -> {
                PyramidPlunder.toggleObstacle(client, objectId)
                true
            }
            objectId == 20275 -> {
                client.transport(Position(2799, 5160, 0))
                client.setFocus(2799, 5159)
                true
            }
            objectId == 20277 -> {
                client.transport(Position(3315, 2796, 0))
                client.setFocus(3315, 2797)
                true
            }
            else -> false
        }
    }

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId == 20931) {
            PyramidPlunder.reset(client)
            return true
        }
        return false
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

object ThievingSkillPlugin : SkillPlugin {
    override val definition =
        skillPlugin(name = "Thieving", skill = Skill.THIEVING) {
            val stallObjects = StallObjectContent()
            val chestObjects = ChestObjectContent()
            val plunderObjects = PlunderObjectContent()
            val firstClickObjects =
                (chestObjects.objectIds + plunderObjects.objectIds).distinct().toIntArray()
            val secondClickObjects =
                (stallObjects.objectIds + chestObjects.objectIds + plunderObjects.objectIds).distinct().toIntArray()

            objectClick(preset = PolicyPreset.GATHERING, option = 1, *firstClickObjects) { client, objectId, position, obj ->
                if (objectId in chestObjects.objectIds) {
                    chestObjects.onFirstClick(client, objectId, position, obj)
                } else {
                    plunderObjects.onFirstClick(client, objectId, position, obj)
                }
            }
            objectClick(preset = PolicyPreset.GATHERING, option = 2, *secondClickObjects) { client, objectId, position, obj ->
                when {
                    objectId in stallObjects.objectIds -> stallObjects.onSecondClick(client, objectId, position, obj)
                    objectId in chestObjects.objectIds -> chestObjects.onSecondClick(client, objectId, position, obj)
                    else -> plunderObjects.onSecondClick(client, objectId, position, obj)
                }
            }
        }
}
