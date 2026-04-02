package net.dodian.uber.game.content.items

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.systems.skills.ProgressionService
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.systems.api.content.ContentActions
import net.dodian.uber.game.systems.api.content.ContentProductionRequest
import net.dodian.uber.game.content.skills.crafting.CraftingData
import net.dodian.uber.game.content.skills.crafting.GoldJewelryService
import net.dodian.uber.game.content.skills.crafting.Crafting

object CraftingItemCombinationHandler {
    @JvmStatic
    fun handleCrystalKey(client: Client, itemUsed: Int, otherItem: Int, itemUsedSlot: Int, usedWithSlot: Int): Boolean {
        if ((itemUsed == 2383 && otherItem == 2382) || (itemUsed == 2382 && otherItem == 2383)) {
            if (client.getSkillLevel(Skill.CRAFTING) >= 60) {
                client.deleteItem(itemUsed, itemUsedSlot, 1)
                client.deleteItem(otherItem, usedWithSlot, 1)
                client.addItem(989, 1)
                client.sendMessage("You have crafted the crystal key!  I wonder what it's for?")
            } else {
                client.sendMessage("You need 60 crafting to make the crystal key")
            }
            return true
        }
        return false
    }

    @JvmStatic
    fun handle(client: Client, itemUsed: Int, otherItem: Int, itemUsedSlot: Int, usedWithSlot: Int): Boolean {
        if ((itemUsed == 1733 || otherItem == 1733) && (itemUsed == 1741 || otherItem == 1741)) {
            client.openInterface(2311)
            return true
        }
        for ((index, hide) in CraftingData.hideDefinitions.withIndex()) {
            if ((itemUsed == 1733 || otherItem == 1733) && (itemUsed == hide.itemId || otherItem == hide.itemId)) {
                Crafting.open(client, index)
                client.cIndex = index
                return true
            }
        }
        if (itemUsed == 1755 || otherItem == 1755) {
            val gem = if (itemUsed == 1755) otherItem else itemUsed
            val definition = CraftingData.findGemDefinition(gem)
            if (definition != null) {
                if (definition.requiredLevel > client.getLevel(Skill.CRAFTING)) {
                    client.sendMessage("You need a crafting level of ${definition.requiredLevel} to cut this.")
                    return true
                }
                ContentActions.queueProductionSelection(
                    client,
                    ContentProductionRequest(
                        skillId = Skill.CRAFTING.id,
                        productId = definition.cutId,
                        amountPerCycle = 1,
                        primaryItemId = gem,
                        secondaryItemId = -1,
                        experiencePerUnit = definition.experience * 5,
                        animationId = definition.animationId,
                        tickDelay = 3,
                        completionMessage = "You cut the ${client.getItemName(definition.cutId)}",
                    ),
                )
                return true
            }
        }
        if (itemUsed == 1391 || otherItem == 1391) {
            val orb = if (itemUsed == 1391) otherItem else itemUsed
            val definition = CraftingData.findOrbDefinition(orb)
            if (definition != null) {
                if (definition.requiredLevel > client.getLevel(Skill.CRAFTING)) {
                    client.sendMessage("You need a crafting level of ${definition.requiredLevel} to make this.")
                    return true
                }
                ContentActions.queueProductionSelection(
                    client,
                    ContentProductionRequest(
                        skillId = Skill.CRAFTING.id,
                        productId = definition.staffId,
                        amountPerCycle = 1,
                        primaryItemId = orb,
                        secondaryItemId = 1391,
                        experiencePerUnit = definition.experience,
                        animationId = -1,
                        tickDelay = 3,
                        completionMessage = "You put the ${client.getItemName(orb).lowercase()} onto the battlestaff and made a ${client.getItemName(definition.staffId).lowercase()}.",
                    ),
                )
                return true
            }
        }
        if ((itemUsed == 1785 && otherItem == 1775) || (itemUsed == 1775 && otherItem == 1785)) {
            val jump = "\n\n\n"
            client.sendInterfaceModel(11465, 160, 229)
            client.sendString(jump + "Vial", 11474)
            client.sendInterfaceModel(11466, 180, 1980)
            client.sendString(jump + "Empty cup", 12396)
            client.sendInterfaceModel(11467, 150, 6667)
            client.sendString(jump + "Fishbowl", 12400)
            client.sendInterfaceModel(11468, 150, 567)
            client.sendString(jump + "Orb", 12404)
            client.sendInterfaceModel(11469, 190, -1)
            client.sendString(jump, 12408)
            client.sendInterfaceModel(11470, 190, -1)
            client.sendString(jump, 12412)
            client.sendInterfaceModel(6199, 190, -1)
            client.sendString(jump, 6203)
            client.openInterface(11462)
            return true
        }
        if ((itemUsed == 6667 && otherItem == 1755) || (itemUsed == 1755 && otherItem == 6667)) {
            val slot = if (itemUsed == 6667) itemUsedSlot else usedWithSlot
            client.deleteItem(6667, slot, 1)
            client.addItemSlot(7534, 1, slot)
            client.checkItemUpdate()
            ProgressionService.addXp(client, 60, Skill.CRAFTING)
            client.sendMessage("You chisel the fishbowl into a helmet.")
            return true
        }
        if (itemUsed == 1759 || otherItem == 1759) {
            val amulet = if (itemUsed == 1759) otherItem else itemUsed
            val strung = GoldJewelryService.findStrungAmulet(amulet)
            if (strung < 0) {
                client.sendMessage("You cannot string this item with wool!")
                return true
            }
            ContentActions.queueProductionSelection(
                client,
                ContentProductionRequest(
                    skillId = Skill.CRAFTING.id,
                    productId = strung,
                    amountPerCycle = 1,
                    primaryItemId = amulet,
                    secondaryItemId = 1759,
                    experiencePerUnit = 60,
                    animationId = -1,
                    tickDelay = 2,
                    completionMessage = "You put the wool onto the ${client.getItemName(strung).lowercase()}.",
                ),
            )
            return true
        }
        return false
    }
}
