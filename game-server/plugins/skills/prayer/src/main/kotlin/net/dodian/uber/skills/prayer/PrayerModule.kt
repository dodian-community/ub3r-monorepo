package net.dodian.uber.skills.prayer

import net.dodian.uber.game.api.content.ContentAttributeKey
import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillItemInteraction
import net.dodian.uber.game.api.plugin.skills.SkillItemOnObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.game.api.plugin.skills.SkillPrayer
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginDefinition
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.skill.runtime.action.CycleSignal
import net.dodian.uber.game.skill.runtime.action.productionAction
import net.dodian.uber.skills.api.SkillModuleDescriptor
import net.dodian.uber.game.api.plugin.runtime.TomlRecordReader

data class PrayerBone(val itemId: Int, val experience: Int)
data class PrayerAltar(val objectId: Int, val offeringEnabled: Boolean)

/**
 * Level/drain/mutual-exclusion data for one prayer, ported from the legacy
 * `PrayerManager.Prayer` enum (`engine/systems/skills/prayer/PrayerManager.kt`) - that enum
 * still owns save-format/protocol fields (configId, buttonId, VALUES/forButton) needed for
 * persistence and varbit writes, but the gameplay data and toggle/drain decision-making now
 * live here.
 */
data class PrayerDef(
    val prayer: SkillPrayer,
    val level: Int,
    val drainEffect: Int,
    /** Legacy client varbit id, needed to clear the button highlight on a refused toggle
     * (mirrors `SkillPrayer.buttonId`, which is the same kind of protocol-level plugin data). */
    val configId: Int,
    val overhead: Boolean,
    val attack: Boolean,
    val strength: Boolean,
    val defence: Boolean,
    val range: Boolean,
    val magic: Boolean,
) {
    /** Whether [this] and [other] share a mutual-exclusion category (matches the legacy
     * bitmask cascade - activating one deactivates the other). */
    fun conflictsWith(other: PrayerDef): Boolean =
        (overhead && other.overhead) || (attack && other.attack) || (strength && other.strength) ||
            (defence && other.defence) || (range && other.range) || (magic && other.magic)
}

object PrayerModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.prayer", "Prayer")
    val bones: List<PrayerBone> by lazy { loadBones() }
    val altars: List<PrayerAltar> by lazy { loadAltars() }
    val prayers: List<PrayerDef> by lazy { loadPrayers() }
    private val prayersByType: Map<SkillPrayer, PrayerDef> by lazy { prayers.associateBy { it.prayer } }

    private val drainRateKey = ContentAttributeKey<Double>("skill.prayer", "drainRate")
    private const val DRAIN_STEP = 0.6

    override val definition: SkillPluginDefinition = skillPlugin("Prayer", Skill.PRAYER) {
        SkillPrayer.entries.forEach { prayer ->
            button(PolicyPreset.DIALOGUE, PRAYER_INTERFACE_ID, null, prayer.buttonId) {
                togglePrayer(it.player, prayer)
                true
            }
        }
        objectClick(PolicyPreset.PRODUCTION, 1, *altars.map { it.objectId }.toIntArray()) { restorePrayer(it.player) }
        val offeringAltars = altars.filter { it.offeringEnabled }.map { it.objectId }.toIntArray()
        bones.forEach { bone ->
            itemClick(PolicyPreset.PRODUCTION, 1, bone.itemId) { bury(it, bone) }
            itemOnObject(PolicyPreset.PRODUCTION, *offeringAltars, itemIds = intArrayOf(bone.itemId)) { offer(it, bone) }
        }
    }

    /**
     * Level/points/duel/death validation and the mutual-exclusion cascade, ported 1:1 from
     * legacy `PrayerManager.togglePrayer`'s guard clauses and `checkExtraPrayers` - including
     * the level==-1 duel/death branch resetting ALL active prayers rather than just refusing
     * the one click. That's preserved deliberately (confirmed intentional-as-is, not "fixed"
     * during this port) even though it only affects the six no-level-requirement prayers
     * (Protect Item, Smite, Retribution, Redemption, Rapid Restore, Rapid Heal).
     */
    private fun togglePrayer(player: SkillPlayer, prayer: SkillPrayer) {
        val data = prayersByType.getValue(prayer)
        if (data.level != -1 && player.skills.current(Skill.PRAYER) < data.level) {
            player.ui.message("You need a prayer level of at least ${data.level} to use ${prayer.name.lowercase().replace('_', ' ')}")
            player.ui.varbit(data.configId, 0)
            return
        }
        if (player.vitals.currentPrayer < 1) {
            player.ui.message("You have no prayer points currently! Recharge at a nearby altar")
            resetAll(player)
            return
        }
        if (player.vitals.inDuel || data.level == -1 || player.vitals.isDead) {
            resetAll(player)
            return
        }
        if (player.vitals.isPrayerActive(prayer)) {
            player.vitals.togglePrayer(prayer)
            return
        }
        player.vitals.togglePrayer(prayer)
        prayers.filter { it.prayer != prayer && data.conflictsWith(it) && player.vitals.isPrayerActive(it.prayer) }
            .forEach { player.vitals.deactivatePrayer(it.prayer) }
    }

    private fun resetAll(player: SkillPlayer) {
        SkillPrayer.entries.forEach { player.vitals.deactivatePrayer(it) }
    }

    /** Prayer-point drain stepping, ported from legacy `PlayerLifecycleTickService.handlePrayerDrain`
     * + `PrayerManager.getDrain`/`getDrainRate` - called once per player per real tick. */
    fun tickDrain(player: SkillPlayer) {
        val activePrayers = prayers.filter { player.vitals.isPrayerActive(it.prayer) }
        val totalDrainEffect = activePrayers.sumOf { it.drainEffect }
        if (totalDrainEffect <= 0) {
            player.attributes.remove(drainRateKey)
            return
        }
        val drainResistance = 60.0 + (2 * player.vitals.prayerBonus)
        val targetDrainRate = drainResistance / totalDrainEffect
        val next = (player.attributes.get(drainRateKey) ?: 0.0) + DRAIN_STEP
        if (next < targetDrainRate) {
            player.attributes.put(drainRateKey, next)
            return
        }
        player.attributes.put(drainRateKey, 0.0)
        val remaining = player.vitals.currentPrayer - 1
        player.vitals.setPrayer(remaining)
        if (remaining <= 0) {
            resetAll(player)
            player.ui.message("Your prayer has ran out! Please recharge at a nearby altar!")
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
                world.graphic(ALTAR_GFX, interaction.target.position)
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

    private fun loadPrayers(): List<PrayerDef> = TomlRecordReader.readRecords("prayer/prayers.toml", "prayer").mapIndexed { index, row ->
        val name = row["name"] ?: error("Invalid prayer TOML field name at record $index")
        val prayer = SkillPrayer.entries.firstOrNull { it.name == name }
            ?: error("prayer/prayers.toml record $index has unknown prayer name '$name'")
        PrayerDef(
            prayer = prayer,
            level = row.signedInt("level", index),
            drainEffect = row.int("drain_effect", index),
            configId = row.int("config_id", index),
            overhead = row["overhead"]?.toBooleanStrictOrNull() ?: false,
            attack = row["attack"]?.toBooleanStrictOrNull() ?: false,
            strength = row["strength"]?.toBooleanStrictOrNull() ?: false,
            defence = row["defence"]?.toBooleanStrictOrNull() ?: false,
            range = row["range"]?.toBooleanStrictOrNull() ?: false,
            magic = row["magic"]?.toBooleanStrictOrNull() ?: false,
        )
    }.also { rows ->
        require(rows.map { it.prayer }.distinct().size == rows.size) { "prayer/prayers.toml contains a duplicate prayer name" }
        require(SkillPrayer.entries.all { entry -> rows.any { it.prayer == entry } }) { "prayer/prayers.toml is missing an entry for one or more SkillPrayer values" }
    }

    private fun Map<String, String>.int(field: String, index: Int): Int = get(field)?.toIntOrNull()?.takeIf { it >= 0 }
        ?: error("Invalid prayer TOML field $field at record $index")

    /** Like [int] but allows -1 (used by `level` to mean "no level requirement"). */
    private fun Map<String, String>.signedInt(field: String, index: Int): Int = get(field)?.toIntOrNull()?.takeIf { it >= -1 }
        ?: error("Invalid prayer TOML field $field at record $index")

    private const val BURY_ANIMATION = 827
    private const val ALTAR_ANIMATION = 3705
    private const val ALTAR_GFX = 624
    private const val PRAYER_INTERFACE_ID = 5608
}
