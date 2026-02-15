package net.dodian.uber.game.content.objects.impl.thieving

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.`object`.GlobalObject
import net.dodian.uber.game.model.`object`.Object as GameObject
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.thieving.Thieving
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.security.ItemLog
import net.dodian.utilities.Utils

object ChestObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(375, 378, 20873, 11729, 11730, 11731, 11732, 11733, 11734)

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId == 20873) {
            Thieving.attemptSteal(client, objectId, position)
            return true
        }
        if (objectId == 375 && position.x == 2593 && position.y == 3108 && client.position.z == 1) {
            if (client.chestEventOccur) {
                return true
            }
            if (client.getLevel(Skill.THIEVING) < 70) {
                client.send(SendMessage("You must be level 70 thieving to open this chest"))
                return true
            }
            if (client.freeSlots() < 1) {
                client.send(SendMessage("You need atleast one free inventory slot!"))
                return true
            }
            if (System.currentTimeMillis() - client.lastAction < 1200) {
                client.lastAction = System.currentTimeMillis()
                return true
            }
            val emptyObj = GameObject(378, position.x, position.y, client.position.z, 10, 2, objectId)
            if (!GlobalObject.addGlobalObject(emptyObj, 12000)) {
                return true
            }
            client.lastAction = System.currentTimeMillis()
            val roll = Math.random() * 100
            if (roll <= 0.3) {
                val items = intArrayOf(2577, 2579, 2631)
                val itemId = items[(Math.random() * items.size).toInt()]
                client.send(SendMessage("You have recieved a ${client.GetItemName(itemId)}!"))
                client.addItem(itemId, 1)
                ItemLog.playerGathering(client, itemId, 1, client.position.copy(), "Thieving")
                client.yell("[Server] - ${client.playerName} has just received from the Yanille chest a  ${client.GetItemName(itemId)}")
            } else {
                val coins = 300 + Utils.random(1200)
                client.send(SendMessage("You find $coins coins inside the chest"))
                client.addItem(995, coins)
                ItemLog.playerGathering(client, 995, coins, client.position.copy(), "Thieving")
            }
            if (client.equipment[Equipment.Slot.HEAD.id] == 2631) {
                client.giveExperience(300, Skill.THIEVING)
            }
            client.checkItemUpdate()
            client.chestEvent++
            client.stillgfx(444, position.y, position.x)
            client.triggerRandom(900)
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
                client.send(SendMessage("You must be level 85 thieving to open this chest"))
                return true
            }
            if (client.freeSlots() < 1) {
                client.send(SendMessage("You need atleast one free inventory slot!"))
                return true
            }
            if (System.currentTimeMillis() - client.lastAction < 1200) {
                client.lastAction = System.currentTimeMillis()
                return true
            }
            val emptyObj = GameObject(378, position.x, position.y, position.z, 11, -1, objectId)
            if (!GlobalObject.addGlobalObject(emptyObj, 15000)) {
                return true
            }
            client.lastAction = System.currentTimeMillis()
            val roll = Math.random() * 100
            if (roll <= 0.3) {
                val items = intArrayOf(1050, 2581, 2631)
                val itemId = items[(Math.random() * items.size).toInt()]
                client.send(SendMessage("You have recieved a ${client.GetItemName(itemId)}!"))
                client.addItem(itemId, 1)
                ItemLog.playerGathering(client, itemId, 1, client.position.copy(), "Thieving")
                client.yell("[Server] - ${client.playerName} has just received from the Legends chest a  ${client.GetItemName(itemId)}")
            } else {
                val coins = 500 + Utils.random(2000)
                client.send(SendMessage("You find $coins coins inside the chest"))
                client.addItem(995, coins)
                ItemLog.playerGathering(client, 995, coins, client.position.copy(), "Thieving")
            }
            if (client.equipment[Equipment.Slot.HEAD.id] == 2631) {
                client.giveExperience(500, Skill.THIEVING)
            }
            client.checkItemUpdate()
            client.chestEvent++
            client.stillgfx(444, position.y, position.x)
            client.triggerRandom(1500)
            return true
        }
        return false
    }

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        return when (objectId) {
            378 -> {
                client.send(SendMessage("This chest is empty!"))
                true
            }
            20873, 11729, 11730, 11731, 11732, 11733, 11734 -> {
                Thieving.attemptSteal(client, objectId, position)
                true
            }
            else -> false
        }
    }
}
