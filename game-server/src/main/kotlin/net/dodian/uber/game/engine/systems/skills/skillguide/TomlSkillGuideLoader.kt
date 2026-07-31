package net.dodian.uber.game.engine.systems.skills.skillguide

import com.fasterxml.jackson.dataformat.toml.TomlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.herblore.HerbloreModule
import net.dodian.uber.skills.smithing.SmithingFrameDefinitions
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(TomlSkillGuideLoader::class.java)

data class TomlSkillGuide(
    val skill: String = "",
    val tab: List<TomlTab> = emptyList(),
    val layout: TomlLayout = TomlLayout(),
    val page: List<TomlPage> = emptyList(),
)

data class TomlTab(
    val component: Int = -1,
    val label: String = "",
)

data class TomlLayout(
    val show: List<Int> = emptyList(),
    val hide: List<Int> = emptyList(),
)

data class TomlPage(
    val tab: Int = -1,
    val dynamic_source: String? = null,
    val entry: List<TomlEntry> = emptyList(),
)

data class TomlEntry(
    val name: String = "",
    val level: String? = null,
    val item: Int = -1,
    val amount: Int? = null,
)

object TomlSkillGuideLoader {
    private val mapper = TomlMapper().registerKotlinModule()

    @JvmStatic
    fun load(): Map<Int, SkillGuideDefinition> {
        val definitions = mutableMapOf<Int, SkillGuideDefinition>()

        for (skill in Skill.values()) {
            val resourceName = "skillguide/${skill.name.lowercase()}.toml"
            val stream = TomlSkillGuideLoader::class.java.classLoader.getResourceAsStream(resourceName)
            if (stream == null) {
                logger.warn("Skill guide resource not found: {}", resourceName)
                continue
            }

            try {
                stream.use { input ->
                    val toml = mapper.readValue(input, TomlSkillGuide::class.java)
                    val skillId = skill.id
                    val tabLabels = toml.tab.map { SkillGuideTabLabel(it.component, it.label) }
                    val layout = SkillGuideLayout(
                        showComponents = toml.layout.show.toIntArray(),
                        hideComponents = toml.layout.hide.toIntArray(),
                    )

                    // Map tabs to pages/providers
                    val pagesByTab = toml.page.associateBy { it.tab }

                    val pageProvider: (Client, Int) -> SkillGuidePage? = { client, child ->
                        val pageConfig = pagesByTab[child]
                        when {
                            pageConfig == null -> SkillGuidePage()
                            pageConfig.dynamic_source != null -> loadDynamicPage(client, pageConfig.dynamic_source)
                            else -> {
                                SkillGuidePage(
                                    entries = pageConfig.entry.map { entry ->
                                        SkillGuideEntry(
                                            text = entry.name,
                                            levelText = entry.level,
                                            itemId = entry.item,
                                            itemAmount = entry.amount
                                        )
                                    }
                                )
                            }
                        }
                    }

                    definitions[skillId] = SkillGuideDefinition(skillId, tabLabels, layout, pageProvider)
                }
            } catch (e: Exception) {
                logger.error("Failed to load skill guide resource {}: {}", resourceName, e.message, e)
            }
        }

        logger.info("Loaded {} skill guide definitions from classpath resources.", definitions.size)
        return definitions
    }

    private fun loadDynamicPage(client: Client, source: String): SkillGuidePage {
        return when (source) {
            "smithing_smelting" -> {
                val level = client.getLevel(Skill.SMITHING)
                val successRate = 50 + ((level + 1) / 4)
                SkillGuidePage(
                    entries = listOf(
                        SkillGuideEntry("Bronze bar", "1", 2349),
                        SkillGuideEntry("Iron bar ($successRate% success)", "15", 2351),
                        SkillGuideEntry("Steel bar (2 coal & 1 iron ore)", "30", 2353),
                        SkillGuideEntry("Gold bar", "40", 2357),
                        SkillGuideEntry("Mithril bar (3 coal & 1 mithril ore)", "55", 2359),
                        SkillGuideEntry("Adamantite bar (4 coal & 1 adamantite ore)", "70", 2361),
                        SkillGuideEntry("Runite bar (6 coal & 1 runite ore)", "85", 2363),
                    )
                )
            }
            "smithing_frame_0" -> loadSmithingFrame(client, 0)
            "smithing_frame_1" -> loadSmithingFrame(client, 1)
            "smithing_frame_2" -> loadSmithingFrame(client, 2)
            "smithing_frame_3" -> loadSmithingFrame(client, 3)
            "smithing_frame_4" -> loadSmithingFrame(client, 4)
            "smithing_frame_5" -> loadSmithingFrame(client, 5)
            "herblore_herbs" -> {
                SkillGuidePage(
                    entries = HerbloreModule.herbs.map { definition ->
                        val cleanName = client.getItemName(definition.cleanId)
                        val suffix = if (definition.premiumOnly) " @red@(Premium only)" else ""
                        SkillGuideEntry(
                            text = cleanName + suffix,
                            levelText = definition.level.toString(),
                            itemId = definition.grimyId,
                        )
                    }
                )
            }
            else -> {
                logger.warn("Unknown dynamic page source: {}", source)
                SkillGuidePage()
            }
        }
    }

    private fun loadSmithingFrame(client: Client, frameIndex: Int): SkillGuidePage {
        val frame = SmithingFrameDefinitions.smithingFrame.getOrNull(frameIndex) ?: return SkillGuidePage()
        return SkillGuidePage(
            entries = frame.map { definition ->
                SkillGuideEntry(
                    text = client.getItemName(definition.itemId),
                    levelText = definition.levelRequired.toString(),
                    itemId = definition.itemId,
                    itemAmount = definition.outputAmount,
                )
            }
        )
    }
}
