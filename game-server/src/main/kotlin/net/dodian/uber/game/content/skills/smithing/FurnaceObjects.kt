package net.dodian.uber.game.content.skills.smithing

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.content.skills.smithing.SmithingInterface
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.systems.api.content.ContentActions
import net.dodian.uber.game.systems.api.content.ContentProductionMode
import net.dodian.uber.game.systems.api.content.ContentProductionRequest
import net.dodian.uber.game.content.skills.crafting.Crafting
import net.dodian.uber.game.content.skills.smithing.Smithing
import net.dodian.utilities.Utils

object FurnaceObjects : ObjectContent {
    override val objectIds: IntArray = SmithingObjectComponents.furnaceObjects

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId !in SmithingObjectComponents.smeltingInterfaceFurnaces) {
            return false
        }
        Smithing.openSmelting(client)
        return true
    }

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId in SmithingObjectComponents.smeltingInterfaceFurnaces) {
            Smithing.openSmelting(client)
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
        if (objectId !in SmithingObjectComponents.smeltingInterfaceFurnaces) {
            return false
        }
        if (itemId == 1783 || itemId == 1781) {
            client.send(RemoveInterfaces())
            if (!client.playerHasItem(1783) || !client.playerHasItem(1781)) {
                client.sendMessage("You need one bucket of sand and one soda ash")
                return true
            }
            ContentActions.queueProductionSelection(
                client,
                ContentProductionRequest(
                    skillId = Skill.CRAFTING.id,
                    productId = 1775,
                    amountPerCycle = 1,
                    primaryItemId = 1783,
                    secondaryItemId = 1781,
                    experiencePerUnit = 80,
                    animationId = 899,
                    tickDelay = 3,
                    completionMessage = "You smelt soda ash with the sand and made molten glass.",
                    mode = ContentProductionMode.MOLTEN_GLASS,
                ),
            )
            return true
        }

        if (itemId == 2357) {
            Crafting.openGoldJewelry(client)
            return true
        }
        SmithingInterface.selectPendingRecipeFromOre(client, itemId)
        Smithing.openSmelting(client)
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
            client.sendMessage("You need level $levelReq magic in order to cast this spell!")
            return true
        }
        if (!client.playerHasItem(567) || !client.playerHasItem(564, 3)) {
            client.sendMessage("You need one unpowered orb and 3 cosmic runes to cast on this obelisk.")
            return true
        }
        ContentActions.queueProductionSelection(
            client,
            ContentProductionRequest(
                skillId = Skill.MAGIC.id,
                productId = resultItem,
                amountPerCycle = 1,
                primaryItemId = 567,
                secondaryItemId = 564,
                experiencePerUnit = exp,
                animationId = 726,
                tickDelay = 5,
                completionMessage = message,
                mode = ContentProductionMode.CHARGED_ORB,
            ),
        )
        return true
    }
}
