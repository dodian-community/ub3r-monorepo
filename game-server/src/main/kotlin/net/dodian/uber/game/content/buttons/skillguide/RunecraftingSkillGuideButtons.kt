package net.dodian.uber.game.content.buttons.skillguide

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill

object RunecraftingSkillGuideButtons : ButtonContent {
    override val buttonIds: IntArray = SkillGuideButtonIds.RUNECRAFTING

    override fun onClick(client: Client, buttonId: Int): Boolean {
        client.showSkillMenu(Skill.RUNECRAFTING.getId(), 0)
        return true
    }
}
