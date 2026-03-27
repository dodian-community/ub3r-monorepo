package net.dodian.uber.game.skills.guide

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.netty.listener.out.ShowInterface

object SkillGuideService {
    private val baselineHidden = intArrayOf(15307, 15304, 15294, 8863, 8860, 8850, 8841, 8838, 8828)
    private val baselineShown = intArrayOf(8825, 8813)
    private val titleComponentIds = intArrayOf(8716, 8846, 8823, 8824, 8827, 8837, 8840, 8843, 8859, 8849)

    @JvmStatic
    fun open(client: Client, skillId: Int, child: Int) {
        val switchingSkill = client.currentSkill != skillId
        if (switchingSkill) {
            client.send(RemoveInterfaces())
        }
        if (client.isBusy) {
            client.send(SendMessage("You are currently too busy to open the skill menu!"))
            return
        }

        val definition = SkillGuideDefinitions.find(skillId) ?: return
        val skill = Skill.getSkill(skillId) ?: return

        clearInterface(client)
        resetBaselineVisibility(client, skillId)

        val skillName = skill.name.lowercase().replaceFirstChar { it.uppercase() }
        client.send(SendString(skillName, 8716))

        definition.layout.hideComponents.forEach { client.changeInterfaceStatus(it, false) }
        definition.layout.showComponents.forEach { client.changeInterfaceStatus(it, true) }
        definition.layout.extraStrings.forEach { (componentId, text) -> client.send(SendString(text, componentId)) }
        definition.tabLabels.forEach { label -> client.send(SendString(label.text, label.componentId)) }

        val page = definition.pageProvider(client, child) ?: SkillGuidePage()
        page.entries.forEachIndexed { index, entry ->
            client.send(SendString(entry.text, 8760 + index))
            entry.levelText?.let { client.send(SendString(it, 8720 + index)) }
        }

        val itemIds = page.entries.map { it.itemId }.toIntArray()
        val hasAmounts = page.entries.any { it.itemAmount != null }
        if (hasAmounts) {
            val amounts = page.entries.map { it.itemAmount ?: 0 }.toIntArray()
            client.setMenuItems(itemIds, amounts)
        } else {
            client.setMenuItems(itemIds)
        }

        client.sendQuestSomething(8717)
        if (switchingSkill) {
            client.send(ShowInterface(8714))
        }
        client.currentSkill = skillId
    }

    private fun clearInterface(client: Client) {
        titleComponentIds.forEach { componentId -> client.send(SendString("", componentId)) }
        for (componentId in 8720 until 8800) {
            client.send(SendString("", componentId))
        }
    }

    private fun resetBaselineVisibility(client: Client, skillId: Int) {
        if (skillId >= 23) {
            return
        }
        baselineHidden.forEach { componentId -> client.changeInterfaceStatus(componentId, false) }
        baselineShown.forEach { componentId -> client.changeInterfaceStatus(componentId, true) }
        client.send(SendString("", 8849))
    }
}
