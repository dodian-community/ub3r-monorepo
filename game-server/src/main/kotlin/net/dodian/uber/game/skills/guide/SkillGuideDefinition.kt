package net.dodian.uber.game.skills.guide

import net.dodian.uber.game.model.entity.player.Client

data class SkillGuideDefinition(
    val skillId: Int,
    val tabLabels: List<SkillGuideTabLabel>,
    val layout: SkillGuideLayout = SkillGuideLayout(),
    val pageProvider: (Client, Int) -> SkillGuidePage?,
)

data class SkillGuideTabLabel(
    val componentId: Int,
    val text: String,
)
