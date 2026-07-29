package net.dodian.uber.game.ui

import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding
import net.dodian.uber.game.netty.listener.out.SendEnterName
import net.dodian.uber.game.economy.PriceCheckerService

object PriceCheckerInterface : InterfaceButtonContent {
    private val buttonAddAll = intArrayOf(48505)
    private val buttonWithdrawAll = intArrayOf(48578)
    private val buttonSearch = intArrayOf(48508)
    private val buttonPriceValue = intArrayOf(48584)
    private val buttonPriceAlch = intArrayOf(48585)

    override val bindings =
        listOf(
            buttonBinding(-1, 0, "price_checker.add_all", buttonAddAll) { client, _ ->
                if (PriceCheckerService.isOpen(client)) {
                    PriceCheckerService.depositAll(client)
                }
                true
            },
            buttonBinding(-1, 1, "price_checker.withdraw_all", buttonWithdrawAll) { client, _ ->
                if (PriceCheckerService.isOpen(client)) {
                    PriceCheckerService.withdrawAll(client)
                }
                true
            },
            buttonBinding(-1, 2, "price_checker.search", buttonSearch) { client, _ ->
                if (PriceCheckerService.isOpen(client)) {
                    PriceCheckerService.setSearchPending(client, true)
                    client.send(SendEnterName("Search for item:"))
                }
                true
            },
            buttonBinding(-1, 3, "price_checker.mode_value", buttonPriceValue) { client, _ ->
                if (PriceCheckerService.isOpen(client)) {
                    PriceCheckerService.setMode(client, 0)
                }
                true
            },
            buttonBinding(-1, 4, "price_checker.mode_alch", buttonPriceAlch) { client, _ ->
                if (PriceCheckerService.isOpen(client)) {
                    PriceCheckerService.setMode(client, 1)
                }
                true
            }
        )
}
