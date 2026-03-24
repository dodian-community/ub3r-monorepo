package net.dodian.uber.game.content.interfaces.magic

object MagicComponents {
    const val NORMAL_INTERFACE_ID = 1151
    const val ANCIENT_INTERFACE_ID = 12855

    val spellbookToggleButtons = intArrayOf(74212, 49047, 49046, 23024)

    data class TeleportBinding(
        val componentId: Int,
        val componentKey: String,
        val rawButtonIds: IntArray,
        val x: Int,
        val xRand: Int,
        val y: Int,
        val yRand: Int,
        val premium: Boolean,
    )

    val teleports =
        listOf(
            TeleportBinding(0, "magic.teleport.yanille", intArrayOf(21741, 75010, 84237), 2604, 6, 3101, 3, false),
            TeleportBinding(1, "magic.teleport.seers", intArrayOf(13035, 4143, 50235), 2722, 6, 3484, 2, false),
            TeleportBinding(2, "magic.teleport.ardougne", intArrayOf(13045, 4146, 50245), 2660, 4, 3306, 4, false),
            TeleportBinding(3, "magic.teleport.catherby", intArrayOf(13053, 4150, 50253), 2802, 4, 3432, 3, false),
            TeleportBinding(4, "magic.teleport.legends_guild", intArrayOf(13061, 6004, 51005), 2726, 5, 3346, 2, false),
            TeleportBinding(5, "magic.teleport.taverly", intArrayOf(13069, 6005, 51013), 2893, 4, 3454, 3, false),
            TeleportBinding(6, "magic.teleport.fishing_guild", intArrayOf(13079, 29031, 51023), 2596, 3, 3406, 4, true),
            TeleportBinding(7, "magic.teleport.gnome_village", intArrayOf(13087, 72038, 51031), 2472, 6, 3436, 3, false),
            TeleportBinding(8, "magic.teleport.edgeville", intArrayOf(13095, 4140, 51039), 3085, 4, 3488, 4, false),
        )
}

