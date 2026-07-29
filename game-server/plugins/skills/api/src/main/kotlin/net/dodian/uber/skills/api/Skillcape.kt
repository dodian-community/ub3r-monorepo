package net.dodian.uber.skills.api

import net.dodian.uber.game.model.player.skills.Skill
import kotlin.random.Random
import kotlin.jvm.JvmName

/** Immutable skillcape metadata shared by equipment, drops, and emotes. */
enum class Skillcape(
    private val untrimmedIdRaw: Int,
    private val trimmedIdRaw: Int,
    private val emoteRaw: Int,
    private val gfxRaw: Int,
    private val skillRaw: Skill,
) {
    ATTACK_CAPE(9747, 9748, 4959, 823, Skill.ATTACK),
    STRENGTH_CAPE(9750, 9751, 4981, 828, Skill.STRENGTH),
    DEFENCE_CAPE(9753, 9754, 4961, 824, Skill.DEFENCE),
    RANGING_CAPE(9756, 9757, 4973, 832, Skill.RANGED),
    PRAYER_CAPE(9759, 9760, 4979, 829, Skill.PRAYER),
    MAGIC_CAPE(9762, 9763, 4939, 813, Skill.MAGIC),
    RUNECRAFT_CAPE(9765, 9766, 4947, 817, Skill.RUNECRAFTING),
    HITPOINTS_CAPE(9768, 9769, 4971, 833, Skill.HITPOINTS),
    AGILITY_CAPE(9771, 9772, 4977, 830, Skill.AGILITY),
    HERBLORE_CAPE(9774, 9775, 4969, 835, Skill.HERBLORE),
    THIEVING_CAPE(9777, 9778, 4965, 826, Skill.THIEVING),
    CRAFTING_CAPE(9780, 9781, 4949, 818, Skill.CRAFTING),
    FLETCHING_CAPE(9783, 9784, 4937, 812, Skill.FLETCHING),
    SLAYER_CAPE(9786, 9787, 4967, 827, Skill.SLAYER),
    MINING_CAPE(9792, 9793, 4941, 814, Skill.MINING),
    SMITHING_CAPE(9795, 9796, 4943, 815, Skill.SMITHING),
    FISHING_CAPE(9798, 9799, 4951, 819, Skill.FISHING),
    COOKING_CAPE(9801, 9802, 4955, 821, Skill.COOKING),
    FIREMAKING_CAPE(9804, 9805, 4975, 831, Skill.FIREMAKING),
    WOODCUTTING_CAPE(9807, 9808, 4957, 822, Skill.WOODCUTTING),
    ;

    fun getUntrimmedId(): Int = untrimmedIdRaw
    fun getTrimmedId(): Int = trimmedIdRaw
    fun getEmote(): Int = emoteRaw
    fun getGfx(): Int = gfxRaw
    fun getSkill(): Skill = skillRaw

    @get:JvmName("getUntrimmedIdValue")
    val untrimmedId: Int get() = untrimmedIdRaw
    @get:JvmName("getTrimmedIdValue")
    val trimmedId: Int get() = trimmedIdRaw
    @get:JvmName("getEmoteValue")
    val emote: Int get() = emoteRaw
    @get:JvmName("getGfxValue")
    val gfx: Int get() = gfxRaw

    companion object {
        @JvmStatic fun getSkillCape(itemId: Int): Skillcape? = entries.firstOrNull { it.trimmedIdRaw == itemId || it.untrimmedIdRaw == itemId }
        @JvmStatic fun getRandomCape(): Skillcape = entries[Random.nextInt(entries.size)]
        @JvmStatic fun isTrimmed(itemId: Int): Boolean = entries.any { it.trimmedIdRaw == itemId }
    }
}
