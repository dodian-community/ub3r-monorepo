package net.dodian.uber.game.model.player.casino

import net.dodian.uber.game.Server

class Spin(
    private val symbols: Array<Symbol>,
) {
    private var winnings: Int = 0

    init {
        symbols[0].setColor("red")
        symbols[1].setColor("red")
        symbols[2].setColor("red")

        if (symbols[0].getId() == symbols[1].getId() && symbols[1].getId() == symbols[2].getId()) {
            symbols[0].setColor("gre")
            symbols[1].setColor("gre")
            symbols[2].setColor("gre")
            winnings =
                when (symbols[0].getId()) {
                    8 -> {
                        val total = Server.slots.slotsJackpot + Server.slots.peteBalance
                        if (total >= Int.MAX_VALUE) Int.MAX_VALUE else total
                    }

                    7 -> 25_000
                    else -> 3_500
                }
        } else if (symbols[0].getId() == symbols[1].getId() || symbols[1].getId() == symbols[2].getId() || symbols[0].getId() == symbols[2].getId()) {
            var id = -1
            if (symbols[0].getId() == symbols[1].getId()) {
                id = symbols[0].getId()
                symbols[0].setColor("gre")
                symbols[1].setColor("gre")
            } else if (symbols[0].getId() == symbols[2].getId()) {
                id = symbols[0].getId()
                symbols[0].setColor("gre")
                symbols[2].setColor("gre")
            } else if (symbols[1].getId() == symbols[2].getId()) {
                id = symbols[1].getId()
                symbols[1].setColor("gre")
                symbols[2].setColor("gre")
            }
            winnings =
                when (id) {
                    8 -> 30_000
                    7 -> 11_000
                    -1 -> 0
                    else -> 1_500
                }
        } else {
            var found = false
            for (symbol in symbols) {
                if (symbol.getId() == 8 || symbol.getId() == 7) {
                    symbol.setColor("gre")
                    winnings = 1_500
                    found = true
                    break
                }
            }
            if (!found) {
                winnings = 0
            }
        }
    }

    fun getSymbols(): Array<Symbol> = symbols

    fun getWinnings(): Int = winnings
}
