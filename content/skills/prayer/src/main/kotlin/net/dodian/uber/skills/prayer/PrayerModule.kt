package net.dodian.uber.skills.prayer

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillItemInteraction
import net.dodian.uber.game.api.plugin.skills.SkillItemOnObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginDefinition
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.skill.runtime.action.CycleSignal
import net.dodian.uber.game.skill.runtime.action.productionAction
import net.dodian.uber.skills.api.SkillModuleDescriptor
import net.dodian.uber.skills.runtime.TomlRecordReader

data class PrayerBone(val itemId: Int, val experience: Int)
data class PrayerAltar(val objectId: Int, val offeringEnabled: Boolean)

object PrayerModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.prayer", "Prayer")
    val bones: List<PrayerBone> by lazy { loadBones() }
    val altars: List<PrayerAltar> by lazy { loadAltars() }

    override val definition: SkillPluginDefinition = skillPlugin("Prayer", Skill.PRAYER) {
        objectClick(PolicyPreset.PRODUCTION, 1, *altars.map { it.objectId }.toIntArray()) { restorePrayer(it.player) }
        val offeringAltars = altars.filter { it.offeringEnabled }.map { it.objectId }.toIntArray()
        bones.forEach { bone ->
            itemClick(PolicyPreset.PRODUCTION, 1, bone.itemId) { bury(it, bone) }
            itemOnObject(PolicyPreset.PRODUCTION, *offeringAltars, itemIds = intArrayOf(bone.itemId)) { offer(it, bone) }
        }
    }

    override val contentManifest: ContentModuleManifest = definition.manifest(
        id = descriptor.id,
        owner = "gameplay",
        version = descriptor.version,
        maturity = ContentMaturity.STABLE,
    )

    private fun restorePrayer(player: net.dodian.uber.game.api.plugin.skills.SkillPlayer): Boolean {
        if (player.vitals.currentPrayer >= player.vitals.maximumPrayer) {
            player.ui.message("You are at maximum prayer points!")
        } else {
            player.vitals.setPrayer(player.vitals.maximumPrayer)
            player.ui.message("You restore your prayer points!")
        }
        return true
    }

    private fun bury(interaction: SkillItemInteraction, bone: PrayerBone): Boolean {
        val player = interaction.player
        if (!player.inventory.transaction { removeAt(interaction.itemSlot, bone.itemId) }) return false
        player.actions.animate(BURY_ANIMATION)
        player.skills.gainXp(bone.experience, Skill.PRAYER)
        player.actions.triggerRandomEvent(bone.experience)
        player.ui.message("You bury the ${player.inventory.itemName(bone.itemId).lowercase()}")
        return true
    }

    private fun offer(interaction: SkillItemOnObjectInteraction, bone: PrayerBone): Boolean {
        val player = interaction.player
        player.world.anchor(interaction.position)
        val action = productionAction("prayer.altar") {
            delay(3)
            onCycleSignal {
                if (!inventory.transaction { remove(bone.itemId) }) {
                    ui.message("You have run out of bones.")
                    return@onCycleSignal CycleSignal.stop()
                }
                actions.animate(ALTAR_ANIMATION)
                world.graphic(ALTAR_GFX)
                val multiplier = 2.0 + (skills.current(Skill.FIREMAKING) + 1).toDouble() / 100
                val experience = (bone.experience * multiplier).toInt()
                skills.gainXp(experience, Skill.PRAYER)
                actions.triggerRandomEvent(experience)
                ui.message("You sacrifice the ${inventory.itemName(bone.itemId).lowercase()} and your multiplier was $multiplier (${(multiplier * 100).toInt()}%)")
                CycleSignal.success()
            }
        }.start(player)
        return action != null
    }

    private fun loadBones(): List<PrayerBone> = TomlRecordReader.readRecords("prayer/bones.toml", "bone").mapIndexed { index, row ->
        PrayerBone(row.int("item_id", index), row.int("experience", index))
    }.also { rows -> require(rows.map { it.itemId }.distinct().size == rows.size) { "prayer/bones.toml contains duplicate item_id" } }

    private fun loadAltars(): List<PrayerAltar> = TomlRecordReader.readRecords("prayer/altars.toml", "altar").mapIndexed { index, row ->
        PrayerAltar(row.int("object_id", index), row["offering_enabled"]?.toBooleanStrictOrNull() ?: false)
    }.also { rows -> require(rows.map { it.objectId }.distinct().size == rows.size) { "prayer/altars.toml contains duplicate object_id" } }

    private fun Map<String, String>.int(field: String, index: Int): Int = get(field)?.toIntOrNull()?.takeIf { it >= 0 }
        ?: error("Invalid prayer TOML field $field at record $index")

    private const val BURY_ANIMATION = 827
    private const val ALTAR_ANIMATION = 3705
    private const val ALTAR_GFX = 624
}
