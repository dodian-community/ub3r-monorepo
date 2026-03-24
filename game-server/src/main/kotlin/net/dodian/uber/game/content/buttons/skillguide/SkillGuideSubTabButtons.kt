package net.dodian.uber.game.content.buttons.skillguide

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.skills.guide.SkillGuideService

object SkillGuideSubTabButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        8846, 8823, 8824, 8827,
        34142, 34119, 34120, 34123,
        34133, 34136, 34139, 34155,
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        when (buttonId) {
            8846, 34142 -> if (client.currentSkill < 2) SkillGuideService.open(client, Skill.ATTACK.id, 0) else SkillGuideService.open(client, client.currentSkill, 0)
            8823, 34119 -> if (client.currentSkill < 2) SkillGuideService.open(client, Skill.DEFENCE.id, 0) else SkillGuideService.open(client, client.currentSkill, 1)
            8824, 34120 -> if (client.currentSkill < 2) SkillGuideService.open(client, Skill.RANGED.id, 0) else SkillGuideService.open(client, client.currentSkill, 2)
            8827, 34123 -> if (client.currentSkill < 2) client.send(SendMessage("Coming soon!")) else SkillGuideService.open(client, client.currentSkill, 3)
            34133 -> SkillGuideService.open(client, client.currentSkill, 4)
            34136 -> SkillGuideService.open(client, client.currentSkill, 5)
            34139 -> SkillGuideService.open(client, client.currentSkill, 6)
            34155 -> SkillGuideService.open(client, client.currentSkill, 7)
            else -> return false
        }
        return true
    }
}
