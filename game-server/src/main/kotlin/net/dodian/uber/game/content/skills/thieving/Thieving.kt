package net.dodian.uber.game.content.skills.thieving

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.engine.event.GameEventScheduler
import net.dodian.uber.game.systems.ui.dialogue.DialogueService
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.`object`.GlobalObject
import net.dodian.uber.game.model.`object`.Object as GameObject
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.audit.ItemLog
import net.dodian.uber.game.systems.api.content.ContentInteraction
import net.dodian.uber.game.systems.api.content.ContentObjectInteractionPolicy
import net.dodian.uber.game.systems.interaction.PlayerTickThrottleService
import net.dodian.uber.game.systems.skills.ProgressionService
import net.dodian.uber.game.systems.skills.SkillingRandomEventService
import net.dodian.utilities.Misc
import net.dodian.utilities.Utils

object Thieving {
    const val PICKPOCKET_EMOTE: Int = 881
    const val STALL_THIEVING_EMOTE: Int = 832
    const val EMPTY_STALL_ID: Int = 634

    @JvmStatic
    fun attempt(player: Client, entityId: Int, position: Position) {
        val data = ThievingDefinition.forId(entityId) ?: return
        if (player.chestEventOccur) return
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
        val emptyObject = GameObject(EMPTY_STALL_ID, position.x, position.y, position.z, 10, face, data.entityId)
        if (player.getLevel(Skill.THIEVING) < data.requiredLevel) {
            player.sendMessage("You need a thieving level of ${data.requiredLevel} to steal from ${data.name.lowercase().replace('_', ' ')}s.")
            return
        }
        if (!PlayerTickThrottleService.tryAcquireMs(player, PlayerTickThrottleService.THIEVING_GENERIC, 2000L)) {
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

        GameEventScheduler.runLaterMs(600) {
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
                val stallObject = GameObject(EMPTY_STALL_ID, position.x, position.y, position.z, 10, face, data.entityId)
                GlobalObject.addGlobalObject(stallObject, data.respawnTime * 1000)
            }
            player.checkItemUpdate()
            SkillingRandomEventService.trigger(player, data.receivedExperience)
            player.chestEvent++
        }
    }

    private fun article(itemName: String): String =
        when {
            (itemName.startsWith("a") || itemName.startsWith("e") || itemName.startsWith("i") || itemName.startsWith("o") || itemName.startsWith("u")) && !itemName.endsWith("s") -> "an"
            itemName.endsWith("s") -> "some"
            else -> "a"
        }
}

object StallObjects : ObjectContent {
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

object ChestObjects : ObjectContent {
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
        if (objectId == 20873 || objectId == 6847) {
            Thieving.attempt(client, objectId, position)
            return true
        }
        if (objectId == 375 && position.x == 2593 && position.y == 3108 && client.position.z == 1) {
            if (client.chestEventOccur) {
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
            val emptyObj = GameObject(378, position.x, position.y, client.position.z, 10, 2, objectId)
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
            client.chestEvent++
            client.stillgfx(444, position.y, position.x)
            SkillingRandomEventService.trigger(client, 900)
            return true
        }
        if (objectId == 375 && position.x == 2733 && position.y == 3374) {
            if (client.chestEventOccur) {
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
            val emptyObj = GameObject(378, position.x, position.y, position.z, 11, -1, objectId)
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
            client.chestEvent++
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
            20873, 11729, 11730, 11731, 11732, 11733, 11734 -> {
                Thieving.attempt(client, objectId, position)
                true
            }
            else -> false
        }
    }
}

object PlunderObjects : ObjectContent {
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
                client.transport(PyramidPlunder.endPosition(client))
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
