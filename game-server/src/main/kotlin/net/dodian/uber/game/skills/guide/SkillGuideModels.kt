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

data class SkillGuideLayout(
    val showComponents: IntArray = intArrayOf(),
    val hideComponents: IntArray = intArrayOf(),
    val extraStrings: Map<Int, String> = emptyMap(),
)

data class SkillGuidePage(
    val entries: List<SkillGuideEntry> = emptyList(),
)

data class SkillGuideEntry(
    val text: String,
    val levelText: String? = null,
    val itemId: Int = -1,
    val itemAmount: Int? = null,
)
