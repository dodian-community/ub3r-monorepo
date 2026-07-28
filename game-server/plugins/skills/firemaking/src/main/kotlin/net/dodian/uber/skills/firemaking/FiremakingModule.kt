package net.dodian.uber.skills.firemaking

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginDefinition
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillModuleDescriptor
import net.dodian.uber.game.api.plugin.runtime.TomlRecordReader

data class FiremakingLogDef(
    val name: String,
    val itemId: Int,
    val requiredLevel: Int,
    val experience: Int,
    val durationTicks: Int,
)

/**
 * Baseline firemaking only consumes a log with a tinderbox and grants experience.
 */
object FiremakingModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.firemaking", "Firemaking")

    private const val TINDERBOX = 590

    val logs: List<FiremakingLogDef> by lazy { loadLogs() }

    override val definition: SkillPluginDefinition = skillPlugin("Firemaking", Skill.FIREMAKING) {
        logs.forEach { log ->
            itemOnItem(PolicyPreset.PRODUCTION, TINDERBOX, log.itemId) { interaction ->
                lightFire(interaction.player, log)
            }
        }
    }

    override val contentManifest: ContentModuleManifest = definition.manifest(
        id = descriptor.id,
        owner = "gameplay",
        version = descriptor.version,
        maturity = ContentMaturity.STABLE,
    )

    private fun lightFire(player: SkillPlayer, log: FiremakingLogDef): Boolean {
        if (player.skills.current(Skill.FIREMAKING) < log.requiredLevel) {
            player.ui.message("You need a firemaking level of ${log.requiredLevel} to burn ${log.name}.")
            return true
        }
        if (!player.inventory.remove(log.itemId, 1)) return false
        player.inventory.refresh()
        player.skills.gainXp(log.experience, Skill.FIREMAKING)
        player.actions.stop()
        return true
    }

    private fun loadLogs(): List<FiremakingLogDef> =
        TomlRecordReader.readRecords("firemaking/logs.toml", "log").map { row ->
            FiremakingLogDef(
                name = row.getValue("name"),
                itemId = row.getValue("itemId").toInt(),
                requiredLevel = row.getValue("requiredLevel").toInt(),
                experience = row.getValue("experience").toInt(),
                durationTicks = row.getValue("durationTicks").toInt(),
            )
        }
}
