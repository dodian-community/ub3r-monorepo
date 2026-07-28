package net.dodian.uber.skills.runecrafting

import net.dodian.uber.game.api.content.ContentAttributeKey
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

data class RunecraftingAltarDef(
    val objectIds: IntArray,
    val runeId: Int,
    val requiredLevel: Int,
    val experiencePerEssence: Int,
) {
    val objectId: Int get() = objectIds.first()
}

/**
 * Plugin-owned reference implementation for altar rune-crafting.
 */
object RunecraftingModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.runecrafting", "Runecrafting")

    const val RUNE_ESSENCE_ID = 1436

    /** Last altar-craft timestamp for the plugin-owned pouch interaction guard. */
    val LAST_ALTAR_CRAFT_KEY: ContentAttributeKey<Long> = ContentAttributeKey("skill.runecrafting", "lastAltarCraftAtMillis")

    val altars: List<RunecraftingAltarDef> by lazy { loadAltars() }

    override val definition: SkillPluginDefinition = skillPlugin("Runecrafting", Skill.RUNECRAFTING) {
        altars.forEach { altar ->
            objectClick(PolicyPreset.GATHERING, option = 1, *altar.objectIds) { interaction ->
                craftRunes(interaction.player, altar)
            }
        }
    }

    override val contentManifest: ContentModuleManifest = definition.manifest(
        id = descriptor.id,
        owner = "gameplay",
        version = descriptor.version,
        maturity = ContentMaturity.STABLE,
    )

    private fun craftRunes(player: SkillPlayer, altar: RunecraftingAltarDef): Boolean {
        val essenceCount = player.inventory.amount(RUNE_ESSENCE_ID)
        if (essenceCount <= 0) {
            player.ui.message("You do not have any rune essence!")
            return false
        }
        if (player.skills.current(Skill.RUNECRAFTING) < altar.requiredLevel) {
            val runeName = player.inventory.itemName(altar.runeId).lowercase()
            player.ui.message("You must have ${altar.requiredLevel} runecrafting to craft $runeName")
            return false
        }

        // Legacy rolls once per essence with a level-scaled percentage: (level + 1) / 2.
        val bonusChancePercent = (player.skills.current(Skill.RUNECRAFTING) + 1) / 2
        val bonusRuneCount = (0 until essenceCount).count { player.random.chance(bonusChancePercent, 100) }
        val totalRunes = essenceCount + bonusRuneCount
        val totalExperience = altar.experiencePerEssence * essenceCount

        val committed = player.inventory.transaction {
            remove(RUNE_ESSENCE_ID, essenceCount)
            add(altar.runeId, totalRunes)
        }
        if (!committed) return false

        player.attributes.put(LAST_ALTAR_CRAFT_KEY, player.clock.nowMillis())
        player.actions.animate(791)
        player.skills.gainXp(totalExperience, Skill.RUNECRAFTING)
        player.actions.triggerRandomEvent(totalExperience)

        val runeName = player.inventory.itemName(altar.runeId).lowercase()
        player.ui.message("You craft $totalRunes $runeName(s)!")
        return true
    }

    private fun loadAltars(): List<RunecraftingAltarDef> =
        TomlRecordReader.readRecords("runecrafting/altars.toml", "altar").map { row ->
            val objectIds = if (row.contains("objectIds")) {
                row.getValue("objectIds").removeSurrounding("[", "]").split(",").map { it.trim().toInt() }.toIntArray()
            } else if (row.contains("objectId")) {
                intArrayOf(row.getValue("objectId").toInt())
            } else intArrayOf()
            RunecraftingAltarDef(
                objectIds = objectIds,
                runeId = row.getValue("runeId").toInt(),
                requiredLevel = row.getValue("requiredLevel").toInt(),
                experiencePerEssence = row.getValue("experiencePerEssence").toInt(),
            )
        }
}
