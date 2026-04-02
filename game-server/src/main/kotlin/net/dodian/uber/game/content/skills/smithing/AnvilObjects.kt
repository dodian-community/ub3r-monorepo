package net.dodian.uber.game.content.skills.smithing

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.content.skills.smithing.SmithingInterface
import net.dodian.uber.game.systems.skills.ProgressionService
import net.dodian.uber.game.content.skills.smithing.Smithing
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage

object AnvilObjects : ObjectContent {
    override val objectIds: IntArray = SmithingObjectComponents.anvilObjects

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId != 2097) {
            return false
        }
        val barId = SmithingInterface.firstBarInInventory(client)
        if (barId != -1) {
            client.setInteractionAnchor(position.x, position.y, position.z)
            Smithing.openSmithing(client, barId, position.x, position.y)
        } else {
            client.sendMessage("You do not have any bars to smith!")
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
                client.sendMessage("You need a hammer!")
            } else if (itemId == 1540 && !client.playerHasItem(11286)) {
                client.sendMessage("You need a draconic visage!")
            } else if (itemId == 11286 && !client.playerHasItem(1540)) {
                client.sendMessage("You need a anti-dragon shield!")
            } else if (client.getLevel(Skill.SMITHING) < 90) {
                client.sendMessage("You need level 90 smithing to do this!")
            } else {
                client.deleteItem(itemId, itemSlot, 1)
                client.deleteItem(if (itemId == 1540) 11286 else 1540, 1)
                client.addItemSlot(11284, 1, itemSlot)
                client.checkItemUpdate()
                ProgressionService.addXp(client, 15000, Skill.SMITHING)
                client.sendMessage("Your smithing craft made a Dragonfire shield out of the visage.")
            }
            return true
        }
        if (SmithingInterface.resolveTierId(itemId) != -1) {
            client.setInteractionAnchor(position.x, position.y, position.z)
            Smithing.openSmithing(client, itemId, position.x, position.y)
            return true
        }
        return false
    }
}
