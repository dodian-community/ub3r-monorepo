package net.dodian.uber.game.skills.crafting.api

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.crafting.CraftingService
import net.dodian.uber.game.skills.crafting.GoldJewelryRequest
import net.dodian.uber.game.skills.crafting.GoldJewelryService
import net.dodian.uber.game.skills.crafting.TanningRequest
import net.dodian.uber.game.skills.crafting.TanningService

object CraftingPlugin {
    @JvmStatic
    fun open(client: Client, hideIndex: Int) = CraftingService.openLeatherMenu(client, hideIndex)

    @JvmStatic
    fun startLeather(client: Client, productIndex: Int, amount: Int) =
        CraftingService.startStandardLeatherCraft(client, productIndex, amount)

    @JvmStatic
    fun startHide(client: Client, productGroup: Int, amount: Int) = CraftingService.startHideCraft(client, productGroup, amount)

    @JvmStatic
    fun startSpinning(client: Client) = CraftingService.startSpinning(client)

    @JvmStatic
    fun startShafting(client: Client) = CraftingService.startShafting(client)

    @JvmStatic
    fun startTanning(client: Client, request: TanningRequest) = TanningService.start(client, request)

    @JvmStatic
    fun startGoldJewelry(client: Client, request: GoldJewelryRequest) = GoldJewelryService.start(client, request)

    @JvmStatic
    fun openGoldJewelry(client: Client) = GoldJewelryService.openInterface(client)
}
