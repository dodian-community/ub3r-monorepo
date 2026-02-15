package net.dodian.uber.game.content.objects.impl.smithing

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage

object AnvilObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(2097, 2783)

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId != 2097) {
            return false
        }
        var type = -1
        val possibleBars = intArrayOf(2349, 2351, 2353, 2359, 2361, 2363)
        for (possibleBar in possibleBars) {
            if (client.contains(possibleBar)) {
                type = possibleBar
            }
        }
        if (type != -1 && client.CheckSmithing(type) != -1) {
            client.skillX = position.x
            client.setSkillY(position.y)
            client.OpenSmithingFrame(client.CheckSmithing(type))
        } else if (type == -1) {
            client.send(SendMessage("You do not have any bars to smith!"))
        }
        return true
    }

    override fun onUseItem(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        itemId: Int,
        itemSlot: Int,
        interfaceId: Int,
    ): Boolean {
        if (objectId != 2097 && objectId != 2783) {
            return false
        }

        if (objectId == 2097 && (itemId == 1540 || itemId == 11286)) {
            if (!client.playerHasItem(2347)) {
                client.send(SendMessage("You need a hammer!"))
            } else if (itemId == 1540 && !client.playerHasItem(11286)) {
                client.send(SendMessage("You need a draconic visage!"))
            } else if (itemId == 11286 && !client.playerHasItem(1540)) {
                client.send(SendMessage("You need a anti-dragon shield!"))
            } else if (client.getLevel(Skill.SMITHING) < 90) {
                client.send(SendMessage("You need level 90 smithing to do this!"))
            } else {
                client.deleteItem(itemId, itemSlot, 1)
                client.deleteItem(if (itemId == 1540) 11286 else 1540, 1)
                client.addItemSlot(11284, 1, itemSlot)
                client.checkItemUpdate()
                client.giveExperience(15000, Skill.SMITHING)
                client.send(SendMessage("Your smithing craft made a Dragonfire shield out of the visage."))
            }
            return true
        }

        val type = client.CheckSmithing(itemId)
        if (type != -1) {
            client.skillX = position.x
            client.setSkillY(position.y)
            client.OpenSmithingFrame(type)
            return true
        }
        return false
    }
}
