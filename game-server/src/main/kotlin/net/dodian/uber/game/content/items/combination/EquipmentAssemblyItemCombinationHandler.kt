package net.dodian.uber.game.content.items.combination

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage

object EquipmentAssemblyItemCombinationHandler {
    private val slayerHelmItems = intArrayOf(4155, 4156, 4164, 4166, 4168, 4551, 6720, 8923, 11784, 8921)

    @JvmStatic
    fun handle(client: Client, itemUsed: Int, otherItem: Int): Boolean {
        val usedMatches = slayerHelmItems.any { itemUsed == it }
        val otherMatches = slayerHelmItems.any { otherItem == it }
        if (!usedMatches || !otherMatches) {
            return false
        }

        var hasAllItems = true
        for (index in 0 until slayerHelmItems.size - 2) {
            if (!client.playerHasItem(slayerHelmItems[index])) {
                hasAllItems = false
            }
        }
        if (!hasAllItems) {
            client.send(SendMessage("You need a enchanted gem, mirror shield, face mask, earmuffs, nosepeg, spiny helm,"))
            client.send(SendMessage("slayer gloves, witchwood icon and black mask or black mask (i)"))
            return true
        }

        if (!client.playerHasItem(slayerHelmItems[slayerHelmItems.size - 1]) &&
            !client.playerHasItem(slayerHelmItems[slayerHelmItems.size - 2])
        ) {
            return true
        }

        if (client.getSkillLevel(Skill.CRAFTING) < 70) {
            client.send(SendMessage("You need level 70 crafting to assemble these items together."))
            return true
        }

        val slayerHelm = if (client.playerHasItem(slayerHelmItems[slayerHelmItems.size - 2])) 11865 else 11864
        for (index in 0 until slayerHelmItems.size - 2) {
            client.deleteItem(slayerHelmItems[index], 1)
        }
        client.deleteItem(if (slayerHelm == 11865) slayerHelmItems[slayerHelmItems.size - 2] else slayerHelmItems[slayerHelmItems.size - 1], 1)
        client.addItem(slayerHelm, 1)
        client.send(SendMessage("You assemble the items together and made a ${client.GetItemName(slayerHelm).lowercase()}."))
        client.checkItemUpdate()
        return true
    }
}
