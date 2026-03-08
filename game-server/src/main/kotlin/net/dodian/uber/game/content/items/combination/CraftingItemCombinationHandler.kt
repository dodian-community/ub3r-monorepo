package net.dodian.uber.game.content.items.combination

import net.dodian.uber.game.Constants
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.utilities.Utils

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
        for (index in Constants.leathers.indices) {
            if ((itemUsed == 1733 || otherItem == 1733) && (itemUsed == Constants.leathers[index] || otherItem == Constants.leathers[index])) {
                client.craftMenu(index)
                client.cIndex = index
                return true
            }
        }
        if (itemUsed == 1755 || otherItem == 1755) {
            val gem = if (itemUsed == 1755) otherItem else itemUsed
            var slot = -1
            for (index in Utils.uncutGems.indices) {
                if (Utils.uncutGems[index] == gem) {
                    slot = index
                }
            }
            if (slot >= 0) {
                if (Utils.gemReq[slot] > client.getLevel(Skill.CRAFTING)) {
                    client.send(SendMessage("You need a crafting level of ${Utils.gemReq[slot]} to cut this."))
                    return true
                }
                client.setSkillAction(Skill.CRAFTING.id, Utils.cutGems[slot], 1, gem, -1, Utils.gemExp[slot] * 5, Utils.gemEmote[slot], 3)
                client.skillMessage = "You cut the ${client.GetItemName(Utils.cutGems[slot])}"
                return true
            }
        }
        if (itemUsed == 1391 || otherItem == 1391) {
            val orb = if (itemUsed == 1391) otherItem else itemUsed
            var slot = -1
            for (index in Utils.orbs.indices) {
                if (Utils.orbs[index] == orb) {
                    slot = index
                }
            }
            if (slot >= 0) {
                if (Utils.orbLevel[slot] > client.getLevel(Skill.CRAFTING)) {
                    client.send(SendMessage("You need a crafting level of ${Utils.orbLevel[slot]} to make this."))
                    return true
                }
                client.setSkillAction(Skill.CRAFTING.id, Utils.staves[slot], 1, orb, 1391, Utils.orbXp[slot], -1, 3)
                client.skillMessage = "You put the ${client.GetItemName(orb).lowercase()} onto the battlestaff and made a ${client.GetItemName(Utils.staves[slot]).lowercase()}."
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
            client.setSkillAction(Skill.CRAFTING.id, strung, 1, amulet, 1759, 60, -1, 2)
            client.skillMessage = "You put the wool onto the ${client.GetItemName(strung).lowercase()}."
            return true
        }
        return false
    }
}
