package net.dodian.uber.game.model.player.casino

class Symbol(
    private val id: Int,
    private var symbol: String,
    private val triggers: IntArray,
) {
    private var color: String = ""

    fun check(stop: Int): Boolean {
        for (i in triggers) {
            if (i == stop) {
                return true
            }
        }
        return false
    }

    fun getSymbol(): String = symbol

    fun getId(): Int = id

    fun setSymbol(symbol: String) {
        this.symbol = symbol
    }

    fun setColor(color: String) {
        this.color = "@$color@"
    }

    fun getColor(): String = color

    fun output(): String = color + symbol
}
