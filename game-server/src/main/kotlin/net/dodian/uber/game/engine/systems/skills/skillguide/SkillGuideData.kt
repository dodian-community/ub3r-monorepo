package net.dodian.uber.game.engine.systems.skills.skillguide

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

class SkillGuideBuilder(
    private val skillId: Int,
) {
    private val tabLabels = mutableListOf<SkillGuideTabLabel>()
    private var layout = SkillGuideLayout()
    private var pageProvider: (Client, Int) -> SkillGuidePage? = { _, _ -> null }

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

    fun pages(provider: (Client, Int) -> SkillGuidePage?) {
        pageProvider = provider
    }

    fun build(): SkillGuideDefinition = SkillGuideDefinition(skillId, tabLabels.toList(), layout, pageProvider)
}

fun skillGuide(skillId: Int, build: SkillGuideBuilder.() -> Unit): SkillGuideDefinition =
    SkillGuideBuilder(skillId).apply(build).build()

object SkillGuideData {
    private var definitions: Map<Int, SkillGuideDefinition> = TomlSkillGuideLoader.load()

    @JvmStatic
    fun find(skillId: Int): SkillGuideDefinition? = definitions[skillId]

    @JvmStatic
    fun reload() {
        definitions = TomlSkillGuideLoader.load()
    }
}
