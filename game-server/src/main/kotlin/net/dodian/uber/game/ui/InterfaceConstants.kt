package net.dodian.uber.game.ui

/** Immutable legacy interface component registries shared by all players. */
object InterfaceConstants {
    @JvmField val QUEST_TEXT_COMPONENTS = intArrayOf(8145) + (8147..8195).toList().toIntArray() + (12174..12223).toList().toIntArray()
    @JvmField val SKILL_REWARD_BUTTONS = intArrayOf(2812, 2816, 2813, 2817, 2814, 2818, 2815, 2827, 2829, 2830, 2826, 2828, 2822, 2825, 2824, 2820, 2819, 2821, 47002, 54090, 2823)
    @JvmField val BONUS_NAMES = arrayOf("Stab", "Slash", "Crush", "Magic", "Range", "Stab", "Slash", "Crush", "Magic", "Range", "Melee Str", "Ranged Str", "Magic Dmg", "Prayer")
}
