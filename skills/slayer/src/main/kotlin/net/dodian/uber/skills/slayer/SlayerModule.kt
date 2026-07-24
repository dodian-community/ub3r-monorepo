package net.dodian.uber.skills.slayer

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillNpcInteraction
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginDefinition
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillModuleDescriptor
import net.dodian.uber.skills.runtime.TomlRecordReader

data class SlayerTaskDef(
    val name: String,
    val slayerOnly: Boolean,
    val levelMin: Int,
    val levelMax: Int,
    val amountMin: Int,
    val amountMax: Int,
    val npcIds: List<Int>,
)

object SlayerModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.slayer", "Slayer")
    val tasks: List<SlayerTaskDef> by lazy { loadTasks() }

    private val masterNpcIds = intArrayOf(402, 403, 404)

    override val definition: SkillPluginDefinition = skillPlugin("Slayer", Skill.SLAYER) {
        npcClick(preset = PolicyPreset.GATHERING, option = 1, *masterNpcIds) { interaction ->
            talkToMaster(interaction)
        }
    }

    override val contentManifest: ContentModuleManifest = definition.manifest(
        id = descriptor.id,
        owner = "gameplay",
        version = descriptor.version,
        maturity = ContentMaturity.STABLE,
    )

    private fun talkToMaster(interaction: SkillNpcInteraction): Boolean {
        val player = interaction.player
        player.ui.message("Slayer masters can assign you tasks.")
        return true
    }

    private fun loadTasks(): List<SlayerTaskDef> =
        TomlRecordReader.readRecords("slayer/tasks.toml", "task").map { row ->
            val npcsStr = row.getValue("npc_ids").removeSurrounding("[", "]").split(",")
            SlayerTaskDef(
                name = row.getValue("name"),
                slayerOnly = row.getValue("slayer_only").toBoolean(),
                levelMin = row.getValue("level_min").toInt(),
                levelMax = row.getValue("level_max").toInt(),
                amountMin = row.getValue("amount_min").toInt(),
                amountMax = row.getValue("amount_max").toInt(),
                npcIds = npcsStr.map { it.trim().toInt() },
            )
        }
}
