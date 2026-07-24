package net.dodian.uber.skills.thieving

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillNpcInteraction
import net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginDefinition
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillModuleDescriptor
import net.dodian.uber.skills.runtime.TomlRecordReader

data class ThievingTargetDef(
    val name: String,
    val targetType: String,
    val id: Int,
    val level: Int,
    val experience: Int,
    val lootItem: Int,
    val lootAmount: Int,
)

object ThievingModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.thieving", "Thieving")
    val targets: List<ThievingTargetDef> by lazy { loadTargets() }

    private val npcIds by lazy { targets.filter { it.targetType == "npc" }.map { it.id }.toIntArray() }
    private val stallIds by lazy { targets.filter { it.targetType == "stall" }.map { it.id }.toIntArray() }

    override val definition: SkillPluginDefinition = skillPlugin("Thieving", Skill.THIEVING) {
        if (npcIds.isNotEmpty()) {
            npcClick(preset = PolicyPreset.GATHERING, option = 2, *npcIds) { interaction ->
                pickpocketNpc(interaction)
            }
        }
        if (stallIds.isNotEmpty()) {
            objectClick(preset = PolicyPreset.GATHERING, option = 1, *stallIds) { interaction ->
                stealStall(interaction)
            }
        }
    }

    override val contentManifest: ContentModuleManifest = definition.manifest(
        id = descriptor.id,
        owner = "gameplay",
        version = descriptor.version,
        maturity = ContentMaturity.STABLE,
    )

    private fun pickpocketNpc(interaction: SkillNpcInteraction): Boolean {
        val player = interaction.player
        val target = targets.firstOrNull { it.id == interaction.npc.id } ?: return false
        if (player.skills.current(Skill.THIEVING) < target.level) {
            player.ui.message("You need level ${target.level} thieving to pickpocket this target.")
            return true
        }
        player.actions.animate(881)
        player.inventory.transaction { add(target.lootItem, target.lootAmount) }
        player.skills.gainXp(target.experience, Skill.THIEVING)
        player.actions.triggerRandomEvent(target.experience)
        player.ui.message("You pickpocket the ${target.name}.")
        return true
    }

    private fun stealStall(interaction: SkillObjectInteraction): Boolean {
        val player = interaction.player
        val target = targets.firstOrNull { it.id == interaction.target.id } ?: return false
        if (player.skills.current(Skill.THIEVING) < target.level) {
            player.ui.message("You need level ${target.level} thieving to steal from this stall.")
            return true
        }
        player.actions.animate(832)
        player.inventory.transaction { add(target.lootItem, target.lootAmount) }
        player.skills.gainXp(target.experience, Skill.THIEVING)
        player.actions.triggerRandomEvent(target.experience)
        player.ui.message("You steal from the ${target.name}.")
        return true
    }

    private fun loadTargets(): List<ThievingTargetDef> =
        TomlRecordReader.readRecords("thieving/tables.toml", "target").map { row ->
            ThievingTargetDef(
                name = row.getValue("name"),
                targetType = row.getValue("target_type"),
                id = row.getValue("id").toInt(),
                level = row.getValue("level").toInt(),
                experience = row.getValue("experience").toInt(),
                lootItem = row.getValue("loot_item").toInt(),
                lootAmount = row.getValue("loot_amount").toInt(),
            )
        }
}
