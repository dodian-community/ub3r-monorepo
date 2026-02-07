package net.dodian.uber.game.content.items.herblore

import net.dodian.uber.game.content.items.ItemContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.utilities.Utils

object GrimyHerbItems : ItemContent {
    override val itemIds: IntArray = intArrayOf(
        199, 203, 207, 209, 213, 215, 217, 219, 3049, 3051,
    )

    override fun onFirstClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        for (index in Utils.grimy_herbs.indices) {
            if (Utils.grimy_herbs[index] != itemId) {
                continue
            }

            val herbloreLevel = Skills.getLevelForExperience(client.getExperience(Skill.HERBLORE))
            val requiredLevel = Utils.grimy_herbs_lvl[index]
            if (herbloreLevel < requiredLevel) {
                client.send(SendMessage("You need level $requiredLevel herblore to clean this herb."))
                return true
            }

            client.giveExperience(Utils.grimy_herbs_xp[index], Skill.HERBLORE)
            client.deleteItem(itemId, itemSlot, 1)
            val cleanedId = if (itemId == 3051 || itemId == 3049) itemId - 51 else itemId + 50
            client.addItemSlot(cleanedId, 1, itemSlot)
            client.send(SendMessage("You clean the ${client.GetItemName(itemId)}."))
            return true
        }
        return false
    }
}
