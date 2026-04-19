package net.dodian.uber.game.model.player.skills

import java.util.stream.Stream
import kotlin.jvm.JvmName

enum class Skill(
    private val skillId: Int,
    private val skillName: String,
    private val levelComponentId: Int,
    private val currentComponentId: Int,
    private val enabledFlag: Boolean = true,
) {
    ATTACK(0, "attack", 24138, 24137),
    DEFENCE(1, "defence", 24170, 24169),
    STRENGTH(2, "strength", 24154, 24153),
    HITPOINTS(3, "hitpoints", 24140, 24139),
    RANGED(4, "ranged", 24186, 24185),
    PRAYER(5, "prayer", 24202, 24201),
    MAGIC(6, "magic", 24218, 24217),
    COOKING(7, "cooking", 24190, 24189),
    WOODCUTTING(8, "woodcutting", 24222, 24221),
    FLETCHING(9, "fletching", 24220, 24219),
    FISHING(10, "fishing", 24174, 23173),
    FIREMAKING(11, "firemaking", 24206, 24205),
    CRAFTING(12, "crafting", 24204, 24203),
    SMITHING(13, "smithing", 24158, 24157),
    MINING(14, "mining", 24142, 24141),
    HERBLORE(15, "herblore", 24172, 24171),
    AGILITY(16, "agility", 24156, 24155),
    THIEVING(17, "thieving", 24188, 24187),
    SLAYER(18, "slayer", 24367, 24366),
    FARMING(19, "farming", 24372, 24371),
    RUNECRAFTING(20, "runecrafting", 24362, 24361);

    @get:JvmName("getSkillIdValue")
    val id: Int
        get() = skillId

    @get:JvmName("getLevelComponentValue")
    val levelComponent: Int
        get() = levelComponentId

    @get:JvmName("getCurrentComponentValue")
    val currentComponent: Int
        get() = currentComponentId

    @get:JvmName("getEnabledValue")
    val enabled: Boolean
        get() = enabledFlag

    fun getId(): Int = skillId

    fun getName(): String = skillName

    fun isEnabled(): Boolean = enabledFlag

    fun getCurrentComponent(): Int = currentComponentId

    fun getLevelComponent(): Int = levelComponentId

    companion object {
        private val VALUES = values()

        @JvmStatic
        fun getSkill(id: Int): Skill? = VALUES.firstOrNull { it.skillId == id }

        @JvmStatic
        fun enabledSkills(): Stream<Skill> = Stream.of(*VALUES).filter { it.enabledFlag }

        @JvmStatic
        fun disabledSkills(): Stream<Skill> = Stream.of(*VALUES).filter { !it.enabledFlag }
    }
}
