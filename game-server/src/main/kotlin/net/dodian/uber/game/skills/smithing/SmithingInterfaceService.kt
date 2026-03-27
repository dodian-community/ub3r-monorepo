package net.dodian.uber.game.skills.smithing

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SetSmithing

object SmithingInterfaceService {
    private val frameIds = intArrayOf(1119, 1120, 1121, 1122, 1123)
    private val possibleBars = intArrayOf(2349, 2351, 2353, 2359, 2361, 2363)

    @JvmStatic
    fun resolveTierId(barId: Int): Int = SmithingDefinitions.findSmithingTierByBar(barId)?.typeId ?: -1

    @JvmStatic
    fun openForBar(client: Client, barId: Int, anvilX: Int, anvilY: Int): Boolean {
        if (!client.IsItemInBag(2347)) {
            client.send(SendMessage("You need a ${client.GetItemName(2347)} to hammer bars."))
            return false
        }
        val tier = SmithingDefinitions.findSmithingTierByBar(barId)
        if (tier == null) {
            client.send(SendMessage("You cannot smith this item."))
            return false
        }
        client.setActiveSmithingSelection(ActiveSmithingSelection(tier.typeId, tier.barId, anvilX, anvilY))
        sendSmithingUi(client, tier)
        return true
    }

    @JvmStatic
    fun canSmithProduct(itemId: Int): Boolean = SmithingDefinitions.findTierForProduct(itemId) != null

    @JvmStatic
    fun startFromInterfaceItem(client: Client, itemId: Int, amount: Int): Boolean {
        val selection = client.getActiveSmithingSelection()
        if (selection == null) {
            client.send(SendMessage("Illigal Smithing !"))
            return true
        }
        val tier = SmithingDefinitions.findSmithingTierByTypeId(selection.tierId)
        val product = tier?.products?.firstOrNull { it.itemId == itemId }
        if (tier == null || product == null) {
            client.send(SendMessage("Illigal Smithing !"))
            return true
        }
        val request = SmithingRequest(
            tierId = tier.typeId,
            product = product,
            amount = amount.coerceAtLeast(1),
            barId = tier.barId,
            anvilX = selection.anvilX,
            anvilY = selection.anvilY,
        )
        client.send(net.dodian.uber.game.netty.listener.out.RemoveInterfaces())
        net.dodian.uber.game.runtime.action.SmithingActionService.startSmithing(client, request)
        return true
    }

    private fun sendSmithingUi(client: Client, tier: SmithingTier) {
        clearSpecialLines(client)
        val barNamePrefix = "${tier.displayName} "
        tier.products.take(22).forEach { product ->
            val color = if (client.AreXItemsInBag(tier.barId, product.barsRequired)) "@gre@" else "@red@"
            val barWord = if (product.barsRequired > 1) "bars" else "bar"
            client.send(net.dodian.uber.game.netty.listener.out.SendString("$color${product.barsRequired}$barWord", product.barCountLineId))
            val itemName = client.GetItemName(product.itemId)
            val stripped = if (itemName.startsWith(barNamePrefix)) itemName.removePrefix(barNamePrefix) else itemName
            val nameColor = if (client.getLevel(Skill.SMITHING) >= product.levelRequired) "@whi@" else "@bla@"
            client.send(net.dodian.uber.game.netty.listener.out.SendString(nameColor + stripped, product.itemNameLineId))
        }
        frameIds.forEach { frameId ->
            val items = SmithingDefinitions.displayItemsForFrame(tier, frameId)
            client.send(SetSmithing(frameId, items.map { intArrayOf(it.itemId, it.amount) }.toTypedArray()))
        }
        val special = tier.products.getOrNull(22)
        if (special != null && tier.typeId <= 3) {
            val specialName = client.GetItemName(special.itemId).removePrefix("${tier.displayName} ")
            val specialColor = if (client.getLevel(Skill.SMITHING) >= special.levelRequired) "@whi@" else "@bla@"
            client.send(net.dodian.uber.game.netty.listener.out.SendString(specialColor + specialName, 11461))
        }
        client.showInterface(994)
    }

    private fun clearSpecialLines(client: Client) {
        client.send(net.dodian.uber.game.netty.listener.out.SendString("", 1132))
        client.send(net.dodian.uber.game.netty.listener.out.SendString("", 1096))
        client.send(net.dodian.uber.game.netty.listener.out.SendString("", 11459))
        client.send(net.dodian.uber.game.netty.listener.out.SendString("", 11461))
        client.send(net.dodian.uber.game.netty.listener.out.SendString("", 1135))
        client.send(net.dodian.uber.game.netty.listener.out.SendString("", 1134))
    }

    @JvmStatic
    fun firstBarInInventory(client: Client): Int = possibleBars.firstOrNull(client::contains) ?: -1

    @JvmStatic
    fun resetRuntimeState(client: Client) {
        client.clearActiveSmithingSelection()
        client.IsAnvil = false
    }
}
