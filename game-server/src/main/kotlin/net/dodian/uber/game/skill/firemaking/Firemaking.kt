package net.dodian.uber.game.skill.firemaking

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.engine.systems.skills.ProgressionService
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.skillPlugin

object Firemaking {
    private const val TINDERBOX = 590

    @JvmStatic
    fun handleItemCombination(client: Client, itemUsed: Int, useWith: Int): Boolean {
        if (itemUsed != TINDERBOX && useWith != TINDERBOX) {
            return false
        }
        val log = FiremakingData.findLog(itemUsed, useWith) ?: return false
        if (client.getLevel(Skill.FIREMAKING) < log.level) {
            client.sendMessage("You need a firemaking level of ${log.level} to burn ${log.name}.")
            return true
        }
        client.deleteItem(log.itemId, 1)
        ProgressionService.addXp(client, log.xp, Skill.FIREMAKING)
        client.resetAction()
        return true
    }
}

object FiremakingSkillPlugin : SkillPlugin {
    private const val TINDERBOX = 590
    private val logIds = intArrayOf(1511, 1521, 1519, 1517, 1515, 1513)

    override val definition =
        skillPlugin(name = "Firemaking", skill = Skill.FIREMAKING) {
            for (logId in logIds) {
                itemOnItem(preset = PolicyPreset.PRODUCTION, leftItemId = TINDERBOX, rightItemId = logId) { client, itemUsed, otherItem ->
                    Firemaking.handleItemCombination(client, itemUsed, otherItem)
                }
            }
        }
}
