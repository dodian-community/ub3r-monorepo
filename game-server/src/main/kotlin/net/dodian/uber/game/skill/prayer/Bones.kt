package net.dodian.uber.game.skill.prayer

import kotlin.jvm.JvmName

enum class Bones(
    private val boneItemId: Int,
    private val boneExperience: Int,
) {
    BONES(526, 45),
    BAT_BONES(530, 85),
    BIG_BONES(532, 150),
    ZOGRE_BONES(4812, 265),
    JOGRE_BONES(3125, 395),
    RAURG_BONES(4832, 585),
    DRAGON_BONES(536, 735),
    DAGANNOTH_BONES(6729, 1050),
    OURG_BONES(4834, 1200);

    @get:JvmName("getBoneItemIdValue")
    val itemId: Int
        get() = boneItemId

    @get:JvmName("getBoneExperienceValue")
    val experience: Int
        get() = boneExperience

    fun getItemId(): Int = boneItemId

    fun getExperience(): Int = boneExperience

    companion object {
        @JvmStatic
        fun getBone(itemId: Int): Bones? = values().firstOrNull { it.boneItemId == itemId }
    }
}

