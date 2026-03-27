package net.dodian.uber.game.content.skills.guide

data class SkillGuidePage(
    val entries: List<SkillGuideEntry> = emptyList(),
)

data class SkillGuideEntry(
    val text: String,
    val levelText: String? = null,
    val itemId: Int = -1,
    val itemAmount: Int? = null,
)
