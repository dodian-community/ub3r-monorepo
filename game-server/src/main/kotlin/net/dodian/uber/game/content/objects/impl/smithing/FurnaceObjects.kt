package net.dodian.uber.game.content.objects.impl.smithing

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.utilities.Utils

object FurnaceObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(2150, 2151, 2152, 2153, 3994, 11666, 16469, 29662)

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId != 3994 && objectId != 11666 && objectId != 16469 && objectId != 29662) {
            return false
        }
        for (i in Utils.smelt_frame.indices) {
            client.sendFrame246(Utils.smelt_frame[i], 150, Utils.smelt_bars[i][0])
        }
        client.sendFrame164(2400)
        return true
    }

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId == 3994 || objectId == 11666 || objectId == 16469 || objectId == 29662) {
            client.showItemsGold()
            client.showInterface(4161)
            return true
        }
        return false
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
        if (objectId != 3994 && objectId != 11666 && objectId != 16469 && objectId != 29662) {
            return false
        }
        if (itemId == 1783 || itemId == 1781) {
            client.send(RemoveInterfaces())
            if (!client.playerHasItem(1783) || !client.playerHasItem(1781)) {
                client.send(SendMessage("You need one bucket of sand and one soda ash"))
                return true
            }
            client.setSkillAction(Skill.CRAFTING.id, 1775, 1, 1783, 1781, 80, 899, 3)
            client.skillMessage = "You smelt soda ash with the sand and made molten glass."
            return true
        }

        if (itemId == 2357) {
            client.showItemsGold()
            client.showInterface(4161)
            return true
        }

        for (fi in Utils.smelt_frame.indices) {
            client.sendFrame246(Utils.smelt_frame[fi], 150, Utils.smelt_bars[fi][0])
        }
        client.sendFrame164(2400)
        return true
    }

    override fun onMagic(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        spellId: Int,
    ): Boolean {
        return when {
            objectId == 2151 && spellId == 1179 -> chargeOrb(client, 55, 571, 725, "You charge the orb with the power of water.")
            objectId == 2150 && spellId == 1182 -> chargeOrb(client, 60, 575, 800, "You charge the orb with the power of earth.")
            objectId == 2153 && spellId == 1184 -> chargeOrb(client, 65, 569, 875, "You charge the orb with the power of fire.")
            objectId == 2152 && spellId == 1186 -> chargeOrb(client, 70, 573, 950, "You charge the orb with the power of air.")
            else -> false
        }
    }

    private fun chargeOrb(client: Client, levelReq: Int, resultItem: Int, exp: Int, message: String): Boolean {
        if (client.getLevel(Skill.MAGIC) < levelReq) {
            client.send(SendMessage("You need level $levelReq magic in order to cast this spell!"))
            return true
        }
        if (!client.playerHasItem(567) || !client.playerHasItem(564, 3)) {
            client.send(SendMessage("You need one unpowered orb and 3 cosmic runes to cast on this obelisk."))
            return true
        }
        client.setSkillAction(Skill.MAGIC.id, resultItem, 1, 567, 564, exp, 726, 5)
        client.skillMessage = message
        return true
    }
}
