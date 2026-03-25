package net.dodian.uber.game.skills.guide

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.guide.SkillGuideBookService
import net.dodian.uber.game.skills.guide.SkillGuideService

object SkillGuidePlugin {
    @JvmStatic
    fun open(client: Client, skillId: Int, tabIndex: Int) = SkillGuideService.open(client, skillId, tabIndex)

    @JvmStatic
    fun openBook(client: Client) = SkillGuideBookService.open(client)
}
