package net.dodian.uber.game.systems.ui.interfaces.travel

object TravelComponents {
    const val INTERFACE_ID = 802

    data class TravelOption(
        val componentId: Int,
        val componentKey: String,
        val rawButtonIds: IntArray,
    )

    val options =
        listOf(
            TravelOption(0, "travel.tree", intArrayOf(3056)),
            TravelOption(1, "travel.big_tree", intArrayOf(3057)),
            TravelOption(2, "travel.mountain", intArrayOf(3058)),
            TravelOption(3, "travel.castle", intArrayOf(3059)),
            TravelOption(4, "travel.tent", intArrayOf(3060)),
            TravelOption(5, "travel.totem", intArrayOf(48054)),
        )
}

