package net.dodian.uber.game.systems.ui.interfaces.bank

object BankComponents {
    const val INTERFACE_ID = 5292

    val depositInventoryButtons = intArrayOf(89223, 50004)
    val depositWornItemsButtons = intArrayOf(50007)
    val withdrawAsNoteButtons = intArrayOf(21011, 5387)
    val withdrawAsItemButtons = intArrayOf(21010, 5386)
    val searchButtons = intArrayOf(50010)

    val tabButtons: List<Int> = (0..9).map { 50070 + it * 4 }
}

