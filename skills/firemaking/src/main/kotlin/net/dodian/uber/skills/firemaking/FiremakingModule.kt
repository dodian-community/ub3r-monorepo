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
import net.dodian.uber.skills.runtime.TomlRecordReader

data class FiremakingLogDef(
    val name: String,
    val itemId: Int,
    val requiredLevel: Int,
    val experience: Int,
    val durationTicks: Int,
)

/**
 * Plugin-owned reference implementation for firemaking: light a log with a tinderbox
 * (spawns a temporary fire object, drops ashes once it burns out), or add another log
 * to an already-burning fire for straight experience.
 */
object FiremakingModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.firemaking", "Firemaking")

    private const val TINDERBOX = 590
    private const val FIRE_OBJECT_ID = 5249
    private const val ASHES_ITEM_ID = 592
    private const val ANIM_LIGHT_FIRE = 733

    val logs: List<FiremakingLogDef> by lazy { loadLogs() }

    override val definition: SkillPluginDefinition = skillPlugin("Firemaking", Skill.FIREMAKING) {
        logs.forEach { log ->
            itemOnItem(PolicyPreset.PRODUCTION, TINDERBOX, log.itemId) { interaction ->
                lightFire(interaction.player, log)
            }
        }
        itemOnObject(PolicyPreset.PRODUCTION, FIRE_OBJECT_ID, itemIds = logs.map { it.itemId }.toIntArray()) { interaction ->
            addLogToFire(interaction.player, interaction.itemId)
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
        player.actions.animate(ANIM_LIGHT_FIRE)
        player.ui.message("You light the ${log.name}.")
        player.skills.gainXp(log.experience, Skill.FIREMAKING)
        player.actions.triggerRandomEvent(log.experience)
        player.world.spawnTemporaryObject(FIRE_OBJECT_ID, log.durationTicks) {
            player.world.dropItem(ASHES_ITEM_ID, 1)
            player.ui.message("The fire has burnt out.")
        }
        player.actions.stop()
        return true
    }

    private fun addLogToFire(player: SkillPlayer, itemId: Int): Boolean {
        val log = logs.firstOrNull { it.itemId == itemId } ?: return false
        if (player.skills.current(Skill.FIREMAKING) < log.requiredLevel) {
            player.ui.message("You need a firemaking level of ${log.requiredLevel} to burn ${log.name}.")
            return true
        }
        if (!player.inventory.remove(log.itemId, 1)) return false
        player.inventory.refresh()
        player.skills.gainXp(log.experience, Skill.FIREMAKING)
        player.ui.message("You add the ${log.name} to the fire.")
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
