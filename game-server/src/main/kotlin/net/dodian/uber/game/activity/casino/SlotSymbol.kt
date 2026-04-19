package net.dodian.uber.game.activity.casino

class SlotSymbol(
    private val slotId: Int,
    private var displayText: String,
    private val triggerStops: IntArray,
) {
    private var colorPrefix: String = ""

    fun matchesStop(stop: Int): Boolean {
        for (trigger in triggerStops) {
            if (trigger == stop) {
                return true
            }
        }
        return false
    }

    fun highlight(color: String) {
        colorPrefix = "@$color@"
    }

    fun formattedOutput(): String = colorPrefix + displayText

    fun getId(): Int = slotId

    fun getSymbol(): String = displayText

    fun setSymbol(symbol: String) {
        this.displayText = symbol
    }

    fun setColor(color: String) {
        highlight(color)
    }

    fun getColor(): String = colorPrefix

    fun check(stop: Int): Boolean = matchesStop(stop)

    fun output(): String = formattedOutput()
}

