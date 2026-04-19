package net.dodian.uber.game.model.player.skills.prayer

enum class HeadIcon(private val headIconId: Int) {
    NONE(-1),
    PROTECT_MELEE(0),
    PROTECT_MISSLES(1),
    PROTECT_MAGIC(2),
    RETRIBUTION(3),
    SMITE(4),
    REDEMPTION(5);

    fun asInt(): Int = headIconId
}
