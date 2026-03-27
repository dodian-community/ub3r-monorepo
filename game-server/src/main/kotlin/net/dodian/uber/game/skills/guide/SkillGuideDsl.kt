package net.dodian.uber.game.skills.guide

class SkillGuideBuilder(
    private val skillId: Int,
) {
    private val tabLabels = mutableListOf<SkillGuideTabLabel>()
    private var layout = SkillGuideLayout()
    private var pageProvider: (net.dodian.uber.game.model.entity.player.Client, Int) -> SkillGuidePage? = { _, _ -> null }

    fun labels(vararg labels: Pair<Int, String>) {
        tabLabels += labels.map { SkillGuideTabLabel(it.first, it.second) }
    }

    fun layout(
        showComponents: IntArray = intArrayOf(),
        hideComponents: IntArray = intArrayOf(),
        extraStrings: Map<Int, String> = emptyMap(),
    ) {
        layout = SkillGuideLayout(showComponents, hideComponents, extraStrings)
    }

    fun pages(provider: (net.dodian.uber.game.model.entity.player.Client, Int) -> SkillGuidePage?) {
        pageProvider = provider
    }

    fun build(): SkillGuideDefinition = SkillGuideDefinition(skillId, tabLabels.toList(), layout, pageProvider)
}

fun skillGuide(skillId: Int, build: SkillGuideBuilder.() -> Unit): SkillGuideDefinition =
    SkillGuideBuilder(skillId).apply(build).build()
