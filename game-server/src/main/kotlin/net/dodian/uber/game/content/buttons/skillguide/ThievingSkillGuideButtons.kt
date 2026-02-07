package net.dodian.uber.game.content.buttons.skillguide

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill

object ThievingSkillGuideButtons : ButtonContent {
    override val buttonIds: IntArray = SkillGuideButtonIds.THIEVING

    override fun onClick(client: Client, buttonId: Int): Boolean {
        client.showSkillMenu(Skill.THIEVING.getId(), 0)
        return true
    }
}
