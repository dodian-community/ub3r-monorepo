package net.dodian.uber.game.economy

import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.config.FeatureStateService
import net.dodian.uber.game.engine.systems.inventory.EconomyTransaction
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.audit.ShopLog
import net.dodian.uber.game.shop.ShopManager
import net.dodian.uber.game.shop.ShopRulesService

/**
 * Owns the authoritative shop mutation path. Inventory changes are staged as
 * one economy transaction; shop stock and audit records are changed only after
 * that transaction commits.
 */
object ShopTransactionService {
    @JvmStatic
    fun sell(client: Client, itemId: Int, slot: Int, requestedAmount: Int) {
        if (slot !in client.playerItems.indices || client.playerItems[slot] - 1 != itemId || client.playerItemsN[slot] < 1) return
        if (!FeatureStateService.shopping.get() || client.tradeLocked) {
            client.send(SendMessage(if (client.tradeLocked) "You are trade locked!" else "Currently selling stuff to the store has been disabled!"))
            return
        }

        val unnoted = client.getUnnotedItem(itemId).takeIf { it > 0 } ?: itemId
        val price = ShopRulesService.sellPrice(unnoted)
        if (price < 0 || !Server.itemManager.isTradable(unnoted) || !ShopRulesService.canSellItemToShop(client.MyShopID, unnoted)) {
            client.send(SendMessage("You cannot sell ${client.getItemName(unnoted).lowercase()} in this store."))
            return
        }

        val shopSlot = findSellSlot(client.MyShopID, unnoted) ?: run {
            client.send(SendMessage("Can't sell more items to the store!"))
            return
        }
        val currency = ShopRulesService.currencyItemId(client.MyShopID)
        var available = 0
        var currentCurrency = 0
        EconomyTransaction.run {
            available = inventory(client).amountOf(itemId)
            currentCurrency = inventory(client).amountOf(currency)
        }
        val amount = capSellAmountForCurrency(
            minOf(requestedAmount.coerceAtLeast(0), available, Int.MAX_VALUE - ShopManager.ShopItemsN[client.MyShopID][shopSlot]),
            price,
            currentCurrency,
        )
        if (amount <= 0) {
            client.send(SendMessage("Could not sell anything!"))
            return
        }

        val total = amount.toLong() * price
        if (total > Int.MAX_VALUE || !EconomyTransaction.run {
                val inventory = inventory(client)
                inventory.remove(itemId, amount)
                if (total > 0) inventory.add(currency, total.toInt())
            }) {
            client.send(SendMessage("Could not sell anything!"))
            return
        }

        ShopManager.ShopItems[client.MyShopID][shopSlot] = unnoted + 1
        ShopManager.ShopItemsN[client.MyShopID][shopSlot] += amount
        ShopLog.recordSell(client, client.MyShopID, shopSlot, unnoted, amount, currency, total)
        refreshViewers(client)
    }

    @JvmStatic
    fun buy(client: Client, itemId: Int, slot: Int, requestedAmount: Int) {
        if (slot !in 0 until ShopManager.MaxShopItems || requestedAmount <= 0 || itemId != ShopManager.ShopItems[client.MyShopID][slot] - 1) return
        if (client.canUse(itemId)) {
            client.send(SendMessage("You must be a premium member to buy this item"))
            client.send(SendMessage("Visit Dodian.net to subscribe"))
            return
        }

        val price = ShopRulesService.buyPrice(client.MyShopID, itemId, slot)
        val currency = ShopRulesService.currencyItemId(client.MyShopID)
        var availableCurrency = 0
        EconomyTransaction.run { availableCurrency = inventory(client).amountOf(currency) }
        val amount = capBuyAmountForCurrency(
            minOf(requestedAmount, ShopManager.ShopItemsN[client.MyShopID][slot]),
            price,
            availableCurrency,
        )
        if (amount <= 0) {
            client.send(SendMessage("You don't have enough ${client.getItemName(currency).lowercase()}"))
            return
        }

        val total = amount.toLong() * price
        if (total > Int.MAX_VALUE || !EconomyTransaction.run {
                val inventory = inventory(client)
                if (total > 0) inventory.remove(currency, total.toInt())
                inventory.add(itemId, amount)
            }) {
            client.send(SendMessage("Not enough space in your inventory."))
            return
        }

        ShopManager.ShopItemsN[client.MyShopID][slot] -= amount
        if (slot + 1 > ShopManager.ShopItemsStandard[client.MyShopID] && ShopManager.ShopItemsN[client.MyShopID][slot] <= 0) {
            ShopManager.resetAnItem(client.MyShopID, slot)
        }
        ShopLog.recordBuy(client, client.MyShopID, slot, itemId, amount, currency, total)
        refreshViewers(client)
    }

    @JvmStatic
    fun buyPrice(client: Client, slot: Int): Int {
        if (slot !in 0 until ShopManager.MaxShopItems) return 0
        return ShopRulesService.buyPrice(client.MyShopID, ShopManager.ShopItems[client.MyShopID][slot] - 1, slot)
    }

    private fun findSellSlot(shopId: Int, itemId: Int): Int? = (0 until ShopManager.MaxShopItems).firstOrNull {
        ShopManager.ShopItems[shopId][it] <= 0 || ShopManager.ShopItems[shopId][it] - 1 == itemId
    }

    @JvmStatic
    fun capSellAmountForCurrency(requestedAmount: Int, unitPrice: Int, currentCurrency: Int): Int {
        if (requestedAmount <= 0 || unitPrice <= 0) return requestedAmount
        return minOf(requestedAmount, ((Int.MAX_VALUE.toLong() - currentCurrency.coerceAtLeast(0)) / unitPrice).toInt())
    }

    @JvmStatic
    fun capBuyAmountForCurrency(requestedAmount: Int, unitPrice: Int, availableCurrency: Int): Int {
        if (requestedAmount <= 0 || unitPrice <= 0) return requestedAmount
        return minOf(requestedAmount, (availableCurrency.coerceAtLeast(0).toLong() / unitPrice).toInt())
    }

    private fun refreshViewers(owner: Client) {
        PlayerRegistry.players
            .mapNotNull { eligibleViewer(it, owner.MyShopID, owner.slot) }
            .forEach(Client::checkItemUpdate)
        owner.checkItemUpdate()
    }

    @JvmStatic
    fun eligibleViewer(candidate: Player?, shopId: Int, ownerSlot: Int): Client? {
        val viewer = candidate as? Client ?: return null
        return if (viewer.slot != ownerSlot && viewer.isShopping && viewer.MyShopID == shopId) viewer else null
    }
}
