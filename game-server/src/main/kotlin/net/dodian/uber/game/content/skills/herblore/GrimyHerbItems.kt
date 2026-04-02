package net.dodian.uber.game.content.skills.herblore

import net.dodian.uber.game.content.items.ItemContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.systems.skills.ProgressionService
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.content.skills.herblore.HerbloreData

object GrimyHerbItems : ItemContent {
    override val itemIds: IntArray = HerbloreData.herbDefinitions.map { it.grimyId }.toIntArray()

    override fun onFirstClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        val herb = HerbloreData.findHerbDefinitionByGrimy(itemId)
        if (herb != null) {
            val herbloreLevel = Skills.getLevelForExperience(client.getExperience(Skill.HERBLORE))
            val requiredLevel = herb.requiredLevel
            if (herbloreLevel < requiredLevel) {
                client.sendMessage("You need level $requiredLevel herblore to clean this herb.")
                return true
            }

            ProgressionService.addXp(client, herb.cleaningExperience, Skill.HERBLORE)
            client.deleteItem(itemId, itemSlot, 1)
            client.addItemSlot(herb.cleanId, 1, itemSlot)
            client.sendMessage("You clean the ${client.getItemName(itemId)}.")
            return true
        }
        return false
    }
}
