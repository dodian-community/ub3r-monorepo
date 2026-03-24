package net.dodian.uber.game.content.buttons.skillguide

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.skills.guide.SkillGuideService

object HerbloreSkillGuideButtons : ButtonContent {
    override val buttonIds: IntArray = SkillGuideButtonIds.HERBLORE

    override fun onClick(client: Client, buttonId: Int): Boolean {
        SkillGuideService.open(client, Skill.HERBLORE.id, 0)
        return true
    }
}
