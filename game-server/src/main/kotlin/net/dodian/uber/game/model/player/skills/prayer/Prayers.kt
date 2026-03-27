package net.dodian.uber.game.model.player.skills.prayer

import net.dodian.uber.game.model.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.netty.listener.out.SendMessage
import kotlin.jvm.JvmName

class Prayers(player: Player) {
    private val prayerStatus = BooleanArray(Prayer.values().size)
    private val p: Player = player
    private val c: Client = player as Client
    @Suppress("unused")
    private val lastClicked: Long = System.currentTimeMillis()

    private var currentDrainRate: Double = 0.0

    @get:JvmName("getDrainRateValue")
    @set:JvmName("setDrainRateValue")
    var drainRate: Double
        get() = currentDrainRate
        set(value) {
            currentDrainRate = value
        }

    fun getCurrentDrainRate(): Double = currentDrainRate

    fun setCurrentDrainRate(value: Double) {
        currentDrainRate = value
    }

    fun togglePrayer(prayer: Prayer) {
        if (prayer.prayerLevel != -1 && Skills.getLevelForExperience(p.getExperience(Skill.PRAYER)) < prayer.prayerLevel) {
            c.send(SendMessage("You need a prayer level of at least ${prayer.prayerLevel} to use ${formatEnum(prayer).lowercase()}"))
            c.varbit(prayer.configId, 0)
            return
        }
        if (c.currentPrayer < 1) {
            c.send(SendMessage("You have no prayer points currently! Recharge at a nearby altar"))
            reset()
            return
        }
        if (c.duelFight || prayer.prayerLevel == -1 || c.deathStage > 0) {
            reset()
            return
        }
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
            checkExtraPrayers(prayer)
        }
    }

    fun set(prayer: Prayer, on: Boolean) {
        prayerStatus[prayer.ordinal] = on
    }

    fun getDrain(): Int {
        var drain = 0
        for (prayer in Prayer.values()) {
            if (isPrayerOn(prayer)) {
                drain += prayer.drainEffect
            }
        }
        return drain
    }

    fun getDrainRate(): Double {
        val drainResistance = 60.0 + (2 * c.playerBonus[8])
        val drain = getDrain()
        return if (drain == 0) 0.0 else drainResistance / drain
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

    fun ifCheck(): Boolean {
        for (prayer in Prayer.values()) {
            if (prayer.mask == 1 && prayerStatus[prayer.ordinal]) {
                return true
            }
        }
        return false
    }

    fun checkExtraPrayers(prayer: Prayer) {
        if (prayer.mask == -1) {
            return
        }
        val overheadPrayer = (prayer.mask and OVERHEAD_PRAYER) != 0
        val attackPrayer = (prayer.mask and ATTACK_PRAYER) != 0
        val strengthPrayer = (prayer.mask and STRENGTH_PRAYER) != 0
        val defencePrayer = (prayer.mask and DEFENCE_PRAYER) != 0
        val rangePrayer = (prayer.mask and RANGE_PRAYER) != 0
        val magicPrayer = (prayer.mask and MAGIC_PRAYER) != 0
        for (pray in Prayer.values()) {
            if (!isPrayerOn(pray) || pray == prayer || pray.mask == -1) {
                continue
            }
            if ((pray.mask and OVERHEAD_PRAYER) != 0 && overheadPrayer
                || (pray.mask and ATTACK_PRAYER) != 0 && attackPrayer
                || (pray.mask and STRENGTH_PRAYER) != 0 && strengthPrayer
                || (pray.mask and DEFENCE_PRAYER) != 0 && defencePrayer
                || (pray.mask and RANGE_PRAYER) != 0 && rangePrayer
                || (pray.mask and MAGIC_PRAYER) != 0 && magicPrayer
            ) {
                togglePrayer(pray)
            }
        }
    }

    enum class Prayer(
        val prayerLevel: Int,
        val drainEffect: Int,
        val configId: Int,
        val buttonId: Int,
        val mask: Int = -1,
        val headIcon: HeadIcon? = null,
    ) {
        THICK_SKIN(5, 3, 83, 21233, DEFENCE_PRAYER),
        BURST_OF_STRENGTH(5, 3, 84, 21234, STRENGTH_PRAYER or MAGIC_PRAYER or RANGE_PRAYER),
        CLARITY_OF_THOUGHT(5, 3, 85, 21235, ATTACK_PRAYER),
        SHARP_EYE(10, 5, 700, 77100, RANGE_PRAYER or STRENGTH_PRAYER or ATTACK_PRAYER),
        MYSTIC_WILL(10, 3, 701, 77102, MAGIC_PRAYER or STRENGTH_PRAYER or ATTACK_PRAYER),
        ROCK_SKIN(20, 6, 86, 21236, DEFENCE_PRAYER),
        SUPERHUMAN_STRENGTH(20, 6, 87, 21237, STRENGTH_PRAYER or MAGIC_PRAYER or RANGE_PRAYER),
        IMPROVED_REFLEXES(20, 6, 88, 21238, ATTACK_PRAYER),
        HAWK_EYE(25, 10, 702, 77104, RANGE_PRAYER or STRENGTH_PRAYER or ATTACK_PRAYER),
        MYSTIC_LORE(25, 10, 703, 77106, MAGIC_PRAYER or STRENGTH_PRAYER or ATTACK_PRAYER),
        RAPID_RESTORE(-1, 1, 89, 21239),
        RAPID_HEAL(-1, 1, 90, 21240),
        PROTECT_ITEM(-1, 1, 91, 21241),
        STEEL_SKIN(40, 12, 92, 21242, DEFENCE_PRAYER),
        ULTIMATE_STRENGTH(40, 12, 93, 21243, STRENGTH_PRAYER or MAGIC_PRAYER or RANGE_PRAYER),
        INCREDIBLE_REFLEXES(40, 12, 94, 21244, ATTACK_PRAYER),
        EAGLE_EYE(45, 18, 704, 77109, RANGE_PRAYER or STRENGTH_PRAYER or ATTACK_PRAYER),
        MYSTIC_MIGHT(45, 18, 705, 77111, MAGIC_PRAYER or STRENGTH_PRAYER or ATTACK_PRAYER),
        PROTECT_MAGIC(55, 15, 95, 21245, OVERHEAD_PRAYER, HeadIcon.PROTECT_MAGIC),
        PROTECT_RANGE(55, 15, 96, 21246, OVERHEAD_PRAYER, HeadIcon.PROTECT_MISSLES),
        PROTECT_MELEE(55, 15, 97, 21247, OVERHEAD_PRAYER, HeadIcon.PROTECT_MELEE),
        RETRIBUTION(-1, 15, 98, 2171, OVERHEAD_PRAYER, HeadIcon.RETRIBUTION),
        REDEMPTION(-1, 15, 99, 2172, OVERHEAD_PRAYER, HeadIcon.REDEMPTION),
        SMITE(-1, 15, 100, 2173, OVERHEAD_PRAYER, HeadIcon.SMITE),
        CHIVALRY(70, 24, 706, 77113, ATTACK_PRAYER or STRENGTH_PRAYER or DEFENCE_PRAYER or MAGIC_PRAYER or RANGE_PRAYER),
        PIETY(80, 24, 707, 77115, ATTACK_PRAYER or STRENGTH_PRAYER or DEFENCE_PRAYER or MAGIC_PRAYER or RANGE_PRAYER);

        companion object {
            private val BY_BUTTON = values().associateBy { it.buttonId }

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

        @JvmStatic
        fun formatEnum(`object`: Any): String {
            val s = `object`.toString().lowercase()
            return s.replaceFirstChar { it.uppercase() }.replace("_", " ")
        }
    }
}
