package net.dodian.uber.game.content.items.combination

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.content.skills.core.progression.SkillProgressionService
import net.dodian.uber.game.netty.listener.out.SendMessage

object FiremakingItemCombinationHandler {
    private data class FiremakingLog(val itemId: Int, val level: Int, val xp: Int, val name: String)

    private val logs =
        arrayOf(
            FiremakingLog(1511, 1, 160, "logs"),
            FiremakingLog(1521, 15, 240, "oak logs"),
            FiremakingLog(1519, 30, 360, "willow logs"),
            FiremakingLog(1517, 45, 540, "maple logs"),
            FiremakingLog(1515, 60, 812, "yew logs"),
            FiremakingLog(1513, 75, 1216, "magic logs"),
        )

    @JvmStatic
    fun handle(client: Client, itemUsed: Int, useWith: Int): Boolean {
        if (itemUsed != 590 && useWith != 590) {
            return false
        }
        val log = logs.firstOrNull { itemUsed == it.itemId || useWith == it.itemId } ?: return false
        if (client.getLevel(Skill.FIREMAKING) < log.level) {
            client.send(SendMessage("You need a firemaking level of ${log.level} to burn ${log.name}."))
            return true
        }
        client.deleteItem(log.itemId, 1)
        SkillProgressionService.gainXp(client, log.xp, Skill.FIREMAKING)
        client.resetAction()
        return true
    }
}
