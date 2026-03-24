package net.dodian.uber.game.content.items.combination

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.runtime.action.ProductionActionService
import net.dodian.uber.game.runtime.action.ProductionMode
import net.dodian.uber.game.runtime.action.ProductionRequest
import net.dodian.uber.game.skills.crafting.CraftingDefinitions

object CraftingItemCombinationHandler {
    @JvmStatic
    fun handleCrystalKey(client: Client, itemUsed: Int, otherItem: Int, itemUsedSlot: Int, usedWithSlot: Int): Boolean {
        if ((itemUsed == 2383 && otherItem == 2382) || (itemUsed == 2382 && otherItem == 2383)) {
            if (client.getSkillLevel(Skill.CRAFTING) >= 60) {
                client.deleteItem(itemUsed, itemUsedSlot, 1)
                client.deleteItem(otherItem, usedWithSlot, 1)
                client.addItem(989, 1)
                client.send(SendMessage("You have crafted the crystal key!  I wonder what it's for?"))
            } else {
                client.send(SendMessage("You need 60 crafting to make the crystal key"))
            }
            return true
        }
        return false
    }

    @JvmStatic
    fun handle(client: Client, itemUsed: Int, otherItem: Int, itemUsedSlot: Int, usedWithSlot: Int): Boolean {
        if ((itemUsed == 1733 || otherItem == 1733) && (itemUsed == 1741 || otherItem == 1741)) {
            client.showInterface(2311)
            return true
        }
        for ((index, hide) in CraftingDefinitions.hideDefinitions.withIndex()) {
            if ((itemUsed == 1733 || otherItem == 1733) && (itemUsed == hide.itemId || otherItem == hide.itemId)) {
                net.dodian.uber.game.skills.crafting.CraftingService.openLeatherMenu(client, index)
                client.cIndex = index
                return true
            }
        }
        if (itemUsed == 1755 || otherItem == 1755) {
            val gem = if (itemUsed == 1755) otherItem else itemUsed
            val definition = CraftingDefinitions.findGemDefinition(gem)
            if (definition != null) {
                if (definition.requiredLevel > client.getLevel(Skill.CRAFTING)) {
                    client.send(SendMessage("You need a crafting level of ${definition.requiredLevel} to cut this."))
                    return true
                }
                ProductionActionService.queueSelection(
                    client,
                    ProductionRequest(
                        skillId = Skill.CRAFTING.id,
                        productId = definition.cutId,
                        amountPerCycle = 1,
                        primaryItemId = gem,
                        secondaryItemId = -1,
                        experiencePerUnit = definition.experience * 5,
                        animationId = definition.animationId,
                        tickDelay = 3,
                        completionMessage = "You cut the ${client.GetItemName(definition.cutId)}",
                    ),
                )
                return true
            }
        }
        if (itemUsed == 1391 || otherItem == 1391) {
            val orb = if (itemUsed == 1391) otherItem else itemUsed
            val definition = CraftingDefinitions.findOrbDefinition(orb)
            if (definition != null) {
                if (definition.requiredLevel > client.getLevel(Skill.CRAFTING)) {
                    client.send(SendMessage("You need a crafting level of ${definition.requiredLevel} to make this."))
                    return true
                }
                ProductionActionService.queueSelection(
                    client,
                    ProductionRequest(
                        skillId = Skill.CRAFTING.id,
                        productId = definition.staffId,
                        amountPerCycle = 1,
                        primaryItemId = orb,
                        secondaryItemId = 1391,
                        experiencePerUnit = definition.experience,
                        animationId = -1,
                        tickDelay = 3,
                        completionMessage = "You put the ${client.GetItemName(orb).lowercase()} onto the battlestaff and made a ${client.GetItemName(definition.staffId).lowercase()}.",
                    ),
                )
                return true
            }
        }
        if ((itemUsed == 1785 && otherItem == 1775) || (itemUsed == 1775 && otherItem == 1785)) {
            val jump = "\n\n\n"
            client.sendFrame246(11465, 160, 229)
            client.send(SendString(jump + "Vial", 11474))
            client.sendFrame246(11466, 180, 1980)
            client.send(SendString(jump + "Empty cup", 12396))
            client.sendFrame246(11467, 150, 6667)
            client.send(SendString(jump + "Fishbowl", 12400))
            client.sendFrame246(11468, 150, 567)
            client.send(SendString(jump + "Orb", 12404))
            client.sendFrame246(11469, 190, -1)
            client.send(SendString(jump, 12408))
            client.sendFrame246(11470, 190, -1)
            client.send(SendString(jump, 12412))
            client.sendFrame246(6199, 190, -1)
            client.send(SendString(jump, 6203))
            client.showInterface(11462)
            return true
        }
        if ((itemUsed == 6667 && otherItem == 1755) || (itemUsed == 1755 && otherItem == 6667)) {
            val slot = if (itemUsed == 6667) itemUsedSlot else usedWithSlot
            client.deleteItem(6667, slot, 1)
            client.addItemSlot(7534, 1, slot)
            client.checkItemUpdate()
            client.giveExperience(60, Skill.CRAFTING)
            client.send(SendMessage("You chisel the fishbowl into a helmet."))
            return true
        }
        if (itemUsed == 1759 || otherItem == 1759) {
            val amulet = if (itemUsed == 1759) otherItem else itemUsed
            val strung = client.findStrungAmulet(amulet)
            if (strung < 0) {
                client.send(SendMessage("You cannot string this item with wool!"))
                return true
            }
            ProductionActionService.queueSelection(
                client,
                ProductionRequest(
                    skillId = Skill.CRAFTING.id,
                    productId = strung,
                    amountPerCycle = 1,
                    primaryItemId = amulet,
                    secondaryItemId = 1759,
                    experiencePerUnit = 60,
                    animationId = -1,
                    tickDelay = 2,
                    completionMessage = "You put the wool onto the ${client.GetItemName(strung).lowercase()}.",
                ),
            )
            return true
        }
        return false
    }
}
