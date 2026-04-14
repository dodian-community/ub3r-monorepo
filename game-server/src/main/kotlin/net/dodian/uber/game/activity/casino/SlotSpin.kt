package net.dodian.uber.game.activity.casino

import net.dodian.uber.game.Server

class SlotSpin(
    private val reelSymbols: Array<SlotSymbol>,
) {
    private var payout: Int = 0

    init {
        reelSymbols[0].highlight("red")
        reelSymbols[1].highlight("red")
        reelSymbols[2].highlight("red")

        if (reelSymbols[0].getId() == reelSymbols[1].getId() && reelSymbols[1].getId() == reelSymbols[2].getId()) {
            reelSymbols[0].highlight("gre")
            reelSymbols[1].highlight("gre")
            reelSymbols[2].highlight("gre")
            payout =
                when (reelSymbols[0].getId()) {
                    8 -> {
                        val total = Server.slots.jackpotPool + Server.slots.houseBalance
                        if (total >= Int.MAX_VALUE) Int.MAX_VALUE else total
                    }

                    7 -> 25_000
                    else -> 3_500
                }
        } else if (reelSymbols[0].getId() == reelSymbols[1].getId() || reelSymbols[1].getId() == reelSymbols[2].getId() || reelSymbols[0].getId() == reelSymbols[2].getId()) {
            var matchingId = -1
            if (reelSymbols[0].getId() == reelSymbols[1].getId()) {
                matchingId = reelSymbols[0].getId()
                reelSymbols[0].highlight("gre")
                reelSymbols[1].highlight("gre")
            } else if (reelSymbols[0].getId() == reelSymbols[2].getId()) {
                matchingId = reelSymbols[0].getId()
                reelSymbols[0].highlight("gre")
                reelSymbols[2].highlight("gre")
            } else if (reelSymbols[1].getId() == reelSymbols[2].getId()) {
                matchingId = reelSymbols[1].getId()
                reelSymbols[1].highlight("gre")
                reelSymbols[2].highlight("gre")
            }
            payout =
                when (matchingId) {
                    8 -> 30_000
                    7 -> 11_000
                    -1 -> 0
                    else -> 1_500
                }
        } else {
            var foundHighSymbol = false
            for (symbol in reelSymbols) {
                if (symbol.getId() == 8 || symbol.getId() == 7) {
                    symbol.highlight("gre")
                    payout = 1_500
                    foundHighSymbol = true
                    break
                }
            }
            if (!foundHighSymbol) {
                payout = 0
            }
        }
    }

    fun getSymbols(): Array<SlotSymbol> = reelSymbols

    fun getWinnings(): Int = payout
}

