package net.dodian.uber.game.content.interfaces.crafting

object CraftingInterfaceComponents {
    const val LEATHER_INTERFACE_ID = 8880

    data class ButtonGroup(
        val componentId: Int,
        val componentKey: String,
        val rawButtonIds: IntArray,
        val amountByButton: Map<Int, Int>,
    )

    val hideCraftGroup =
        ButtonGroup(
            componentId = 0,
            componentKey = "crafting.leather.hide_set",
            rawButtonIds = intArrayOf(34185, 34184, 34183, 34182, 34189, 34188, 34187, 34186, 34193, 34192, 34191, 34190),
            amountByButton = mapOf(34185 to 1, 34184 to 5, 34183 to 10, 34182 to 27, 34189 to 1, 34188 to 5, 34187 to 10, 34186 to 27, 34193 to 1, 34192 to 5, 34191 to 10, 34190 to 27),
        )

    val standardCraftGroup =
        ButtonGroup(
            componentId = 1,
            componentKey = "crafting.leather.standard_set",
            rawButtonIds = intArrayOf(33187, 33186, 33185, 33190, 33189, 33188, 33193, 33192, 33191, 33196, 33195, 33194, 33199, 33198, 33197, 33202, 33201, 33200, 33205, 33204, 33203),
            amountByButton = mapOf(33187 to 1, 33186 to 5, 33185 to 10, 33190 to 1, 33189 to 5, 33188 to 10, 33193 to 1, 33192 to 5, 33191 to 10, 33196 to 1, 33195 to 5, 33194 to 10, 33199 to 1, 33198 to 5, 33197 to 10, 33202 to 1, 33201 to 5, 33200 to 10, 33205 to 1, 33204 to 5, 33203 to 10),
        )
}

