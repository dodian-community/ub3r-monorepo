package net.dodian.uber.game.content.buttons.skillguide

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill

object DefenceSkillGuideButtons : ButtonContent {
    override val buttonIds: IntArray = SkillGuideButtonIds.DEFENCE

    override fun onClick(client: Client, buttonId: Int): Boolean {
        client.showSkillMenu(Skill.DEFENCE.getId(), 0)
        return true
    }
}
