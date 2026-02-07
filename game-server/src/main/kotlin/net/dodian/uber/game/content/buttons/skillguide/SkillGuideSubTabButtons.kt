package net.dodian.uber.game.content.buttons.skillguide

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage

object SkillGuideSubTabButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        8846, 8823, 8824, 8827,
        34142, 34119, 34120, 34123,
        34133, 34136, 34139, 34155,
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        when (buttonId) {
            8846, 34142 -> if (client.currentSkill < 2) client.showSkillMenu(Skill.ATTACK.getId(), 0) else client.showSkillMenu(client.currentSkill, 0)
            8823, 34119 -> if (client.currentSkill < 2) client.showSkillMenu(Skill.DEFENCE.getId(), 0) else client.showSkillMenu(client.currentSkill, 1)
            8824, 34120 -> if (client.currentSkill < 2) client.showSkillMenu(Skill.RANGED.getId(), 0) else client.showSkillMenu(client.currentSkill, 2)
            8827, 34123 -> if (client.currentSkill < 2) client.send(SendMessage("Coming soon!")) else client.showSkillMenu(client.currentSkill, 3)
            34133 -> client.showSkillMenu(client.currentSkill, 4)
            34136 -> client.showSkillMenu(client.currentSkill, 5)
            34139 -> client.showSkillMenu(client.currentSkill, 6)
            34155 -> client.showSkillMenu(client.currentSkill, 7)
            else -> return false
        }
        return true
    }
}
