package net.dodian.uber.game.engine.systems.skills.prayer

import net.dodian.uber.game.model.entity.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player

/**
 * Persisted prayer on/off state plus the low-level raw-flip primitive. Level/points/duel/death
 * validation, the mutual-exclusion cascade, and drain-rate computation used to live here too -
 * they've moved to `plugins/skills/prayer/.../PrayerModule.kt` (`togglePrayer`/`tickDrain`),
 * which is the plugin-facing `SkillVitals.togglePrayer`/`deactivatePrayer` caller. This class
 * now only owns what's genuinely persistence/protocol-shaped: the save-format `Prayer.VALUES`/
 * `forButton` lookup (`PlayerSaveEnvelope`/`PlayerSaveSnapshot`/`AccountLoginMapper` replay saved
 * button clicks through it on login) and the raw on/off + varbit + head-icon flip.
 */
class PrayerManager(player: Player) {
    private val prayerStatus = BooleanArray(Prayer.values().size)
    private val p: Player = player
    private val c: Client = player as Client

    /** Raw flip only - no validation, no cascade. See the class doc for where those moved. */
    fun togglePrayer(prayer: Prayer) {
        if (isPrayerOn(prayer)) {
            set(prayer, false)
            c.varbit(prayer.configId, 0)
            if (!ifCheck()) {
                p.headIcon = HeadIcon.NONE.asInt()
                p.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
            }
        } else {
            set(prayer, true)
            prayer.headIcon?.let {
                p.headIcon = it.asInt()
                p.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
            }
        }
    }

    fun set(prayer: Prayer, on: Boolean) {
        prayerStatus[prayer.ordinal] = on
    }

    fun reset() {
        for (prayer in Prayer.values()) {
            set(prayer, false)
            c.varbit(prayer.configId, 0)
        }
        p.headIcon = HeadIcon.NONE.asInt()
        p.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
    }

    fun isPrayerOn(prayer: Prayer): Boolean = prayerStatus[prayer.ordinal]

    /** Whether any overhead-category prayer is still active - used to decide whether to clear
     * the head icon when deactivating one. */
    private fun ifCheck(): Boolean {
        for (prayer in Prayer.values()) {
            if (prayer.mask == 1 && prayerStatus[prayer.ordinal]) {
                return true
            }
        }
        return false
    }

    /** Save-format enum: `configId`/`buttonId` are the legacy client varbit/button ids needed
     * to write varbits and replay saved button clicks on login; `mask`/`headIcon` are used only
     * by [ifCheck] (whether an overhead prayer remains active) and the raw flip's head-icon
     * side effect above - the actual level/drainEffect/cascade *decisions* now live in
     * `PrayerModule.PrayerDef` (`prayer/prayers.toml`), not here. */
    enum class Prayer(
        val configId: Int,
        val buttonId: Int,
        val mask: Int = -1,
        val headIcon: HeadIcon? = null,
    ) {
        THICK_SKIN(83, 5609, DEFENCE_PRAYER),
        BURST_OF_STRENGTH(84, 5610, STRENGTH_PRAYER or MAGIC_PRAYER or RANGE_PRAYER),
        CLARITY_OF_THOUGHT(85, 5611, ATTACK_PRAYER),
        SHARP_EYE(700, 19812, RANGE_PRAYER or STRENGTH_PRAYER or ATTACK_PRAYER),
        MYSTIC_WILL(701, 19814, MAGIC_PRAYER or STRENGTH_PRAYER or ATTACK_PRAYER),
        ROCK_SKIN(86, 5612, DEFENCE_PRAYER),
        SUPERHUMAN_STRENGTH(87, 5613, STRENGTH_PRAYER or MAGIC_PRAYER or RANGE_PRAYER),
        IMPROVED_REFLEXES(88, 5614, ATTACK_PRAYER),
        HAWK_EYE(702, 19816, RANGE_PRAYER or STRENGTH_PRAYER or ATTACK_PRAYER),
        MYSTIC_LORE(703, 19818, MAGIC_PRAYER or STRENGTH_PRAYER or ATTACK_PRAYER),
        RAPID_RESTORE(89, 5615),
        RAPID_HEAL(90, 5616),
        PROTECT_ITEM(91, 5617),
        STEEL_SKIN(92, 5618, DEFENCE_PRAYER),
        ULTIMATE_STRENGTH(93, 5619, STRENGTH_PRAYER or MAGIC_PRAYER or RANGE_PRAYER),
        INCREDIBLE_REFLEXES(94, 5620, ATTACK_PRAYER),
        EAGLE_EYE(704, 19821, RANGE_PRAYER or STRENGTH_PRAYER or ATTACK_PRAYER),
        MYSTIC_MIGHT(705, 19823, MAGIC_PRAYER or STRENGTH_PRAYER or ATTACK_PRAYER),
        PROTECT_MAGIC(95, 5621, OVERHEAD_PRAYER, HeadIcon.PROTECT_MAGIC),
        PROTECT_RANGE(96, 5622, OVERHEAD_PRAYER, HeadIcon.PROTECT_MISSLES),
        PROTECT_MELEE(97, 5623, OVERHEAD_PRAYER, HeadIcon.PROTECT_MELEE),
        RETRIBUTION(98, 683, OVERHEAD_PRAYER, HeadIcon.RETRIBUTION),
        REDEMPTION(99, 684, OVERHEAD_PRAYER, HeadIcon.REDEMPTION),
        SMITE(100, 685, OVERHEAD_PRAYER, HeadIcon.SMITE),
        CHIVALRY(706, 19825, ATTACK_PRAYER or STRENGTH_PRAYER or DEFENCE_PRAYER or MAGIC_PRAYER or RANGE_PRAYER),
        PIETY(707, 19827, ATTACK_PRAYER or STRENGTH_PRAYER or DEFENCE_PRAYER or MAGIC_PRAYER or RANGE_PRAYER);

        companion object {
            @JvmField
            val VALUES = values()
            private val BY_BUTTON = VALUES.associateBy { it.buttonId }

            @JvmStatic
            fun forButton(button: Int): Prayer? = BY_BUTTON[button]
        }
    }

    companion object {
        private const val OVERHEAD_PRAYER = 1
        private const val ATTACK_PRAYER = 2
        private const val STRENGTH_PRAYER = 4
        private const val RANGE_PRAYER = 8
        private const val MAGIC_PRAYER = 16
        private const val DEFENCE_PRAYER = 32
    }
}

