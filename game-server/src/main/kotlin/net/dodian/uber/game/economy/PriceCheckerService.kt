package net.dodian.uber.game.economy

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.EconomyRuntimeState
import net.dodian.uber.game.model.entity.player.PendingInputState
import net.dodian.uber.game.model.item.GameItem
import net.dodian.uber.game.model.item.transaction.invAdd
import net.dodian.uber.game.model.item.transaction.invDelAt
import net.dodian.uber.game.netty.listener.out.ClearItemContainer
import net.dodian.uber.game.netty.listener.out.InventoryInterface
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.ResetItems
import net.dodian.uber.game.netty.listener.out.SendItemOnInterfaceSlot
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.netty.listener.out.SetVarbit
import net.dodian.uber.game.netty.listener.out.TradeItemsUpdate

/**
 * Owns the non-persistent price-checker container and its UI.  Inventory changes
 * use transaction helpers, so exchange reservations are enforced before an item
 * can enter this temporary container.
 */
object PriceCheckerService {
    private const val MAX_SLOTS = 28

    @JvmStatic fun state(client: Client): EconomyRuntimeState = client.contentRuntimeState.getEconomyRuntimeState()
    @JvmStatic fun isOpen(client: Client): Boolean = state(client).priceCheckerOpen
    @JvmStatic fun items(client: Client): List<GameItem> = state(client).priceCheckerItems
    @JvmStatic fun itemAt(client: Client, slot: Int): GameItem? = state(client).priceCheckerItems.getOrNull(slot)
    @JvmStatic fun isSearchPending(client: Client): Boolean = client.contentRuntimeState.getPendingInputState() == PendingInputState.PRICE_CHECKER_SEARCH
    @JvmStatic fun setSearchPending(client: Client, value: Boolean) {
        if (value) client.contentRuntimeState.setPendingInputState(PendingInputState.PRICE_CHECKER_SEARCH)
        else if (isSearchPending(client)) client.contentRuntimeState.clearPendingInputState()
    }

    @JvmStatic fun open(client: Client) {
        val state = state(client)
        // A stale state can only occur after an interrupted interface close; return it first.
        if (state.priceCheckerItems.isNotEmpty()) returnAll(client, state)
        state.priceCheckerOpen = true
        state.priceCheckerSearchedItem = null
        setSearchPending(client, false)
        state.priceCheckerMode = 0
        client.send(InventoryInterface(48500, 5063))
        refresh(client)
    }

    @JvmStatic @JvmOverloads fun close(client: Client, removeInterfaces: Boolean = true) {
        val state = state(client)
        if (!state.priceCheckerOpen && state.priceCheckerItems.isEmpty()) return
        returnAll(client, state)
        state.priceCheckerOpen = false
        state.priceCheckerSearchedItem = null
        setSearchPending(client, false)
        if (removeInterfaces) client.send(RemoveInterfaces())
    }

    @JvmStatic fun deposit(client: Client, itemId: Int, fromSlot: Int, requestedAmount: Int) {
        val state = state(client)
        if (!state.priceCheckerOpen || fromSlot !in client.playerItems.indices || client.playerItems[fromSlot] != itemId + 1) return
        if (!Server.itemManager.isTradable(itemId)) {
            client.send(SendMessage("This item is untradeable!"))
            return
        }
        var amount = minOf(requestedAmount, client.playerItemsN[fromSlot])
        if (amount <= 0) return
        val stackable = Server.itemManager.isStackable(itemId)
        val existing = state.priceCheckerItems.firstOrNull { it.id == itemId && stackable }
        if (existing == null && state.priceCheckerItems.size >= MAX_SLOTS) {
            client.send(SendMessage("Your price checker is full!"))
            return
        }

        if (stackable) {
            if (!client.invDelAt(itemId, fromSlot, amount)) return
            if (existing == null) state.priceCheckerItems += GameItem(itemId, amount) else existing.amount += amount
        } else {
            var moved = 0
            repeat(amount) {
                if (state.priceCheckerItems.size >= MAX_SLOTS) return@repeat
                val slot = client.playerItems.indices.firstOrNull { client.playerItems[it] == itemId + 1 } ?: return@repeat
                if (client.invDelAt(itemId, slot, 1)) {
                    state.priceCheckerItems += GameItem(itemId, 1)
                    moved++
                } else return@repeat
            }
            if (moved < amount && state.priceCheckerItems.size >= MAX_SLOTS) client.send(SendMessage("Your price checker is full!"))
        }
        refresh(client)
    }

    @JvmStatic fun withdraw(client: Client, itemId: Int, fromSlot: Int, requestedAmount: Int) {
        val state = state(client)
        if (!state.priceCheckerOpen) return
        val item = state.priceCheckerItems.getOrNull(fromSlot) ?: return
        if (item.id != itemId) return
        val amount = minOf(requestedAmount, item.amount)
        if (amount <= 0) return
        if (!Server.itemManager.isStackable(itemId) && client.freeSlots() < amount) {
            client.send(SendMessage("Not enough space in your inventory!"))
            return
        }
        if (!client.invAdd(itemId, amount)) {
            client.send(SendMessage("Not enough space in your inventory!"))
            return
        }
        removeFromState(state, fromSlot, itemId, amount)
        refresh(client)
    }

    @JvmStatic fun withdrawAll(client: Client) {
        val state = state(client)
        if (!state.priceCheckerOpen) return
        returnAll(client, state)
        refresh(client)
    }

    @JvmStatic fun depositAll(client: Client) {
        if (!isOpen(client)) return
        client.playerItems.indices.toList().forEach { slot ->
            val itemId = client.playerItems[slot] - 1
            if (itemId >= 0 && client.playerItemsN[slot] > 0) deposit(client, itemId, slot, client.playerItemsN[slot])
        }
    }

    @JvmStatic fun setMode(client: Client, mode: Int) { state(client).priceCheckerMode = if (mode == 1) 1 else 0; refresh(client) }

    @JvmStatic fun search(client: Client, name: String) {
        val state = state(client)
        if (!state.priceCheckerOpen) return
        val needle = name.trim().lowercase().replace("'", "")
        val found = (0 until 20_000).firstOrNull { id ->
            val itemName = Server.itemManager.getName(id)
            !itemName.isNullOrEmpty() && !Server.itemManager.isNote(id) && itemName.lowercase().replace("'", "").contains(needle)
        }
        state.priceCheckerSearchedItem = found?.let { GameItem(it, 1) }
        if (found == null) client.send(SendMessage("No items found matching: $name"))
        refresh(client)
    }

    @JvmStatic fun refresh(client: Client) {
        val state = state(client)
        if (!state.priceCheckerOpen) return
        repeat(MAX_SLOTS) { index ->
            val item = state.priceCheckerItems.getOrNull(index)
            val text = item?.let {
                val price = price(it.id, state.priceCheckerMode).toLong()
                if (Server.itemManager.isStackable(it.id)) "%,d x %,d\\n= %,d".format(it.amount, price, price * it.amount) else "%,d".format(price)
            } ?: ""
            client.send(SendString(text, 48550 + index))
        }
        state.priceCheckerSearchedItem?.let { item ->
            client.send(SendString("<col=ffb000>${Server.itemManager.getName(item.id)}:", 48582))
            client.send(SendString("%,d".format(price(item.id, state.priceCheckerMode)), 48583))
            client.send(SendItemOnInterfaceSlot(48581, item.id, item.amount, 0))
        } ?: run {
            client.send(SendString("\\nSelect item to search", 48582)); client.send(SendString("", 48583)); client.send(ClearItemContainer(48581, 1))
        }
        val total = state.priceCheckerItems.sumOf { price(it.id, state.priceCheckerMode).toLong() * it.amount }
        client.send(SendString("%,d".format(total), 48513))
        client.send(TradeItemsUpdate(48542, state.priceCheckerItems))
        client.send(ResetItems(5064)); client.send(SetVarbit(237, state.priceCheckerMode))
    }

    @JvmStatic fun price(itemId: Int, mode: Int): Int = if (mode == 1) Server.itemManager.getAlchemy(itemId) else Server.itemManager.getShopSellValue(itemId)

    private fun returnAll(client: Client, state: EconomyRuntimeState) {
        val pending = state.priceCheckerItems.toList()
        pending.forEach { item ->
            if (client.invAdd(item.id, item.amount)) state.priceCheckerItems.remove(item)
            else client.send(SendMessage("Not enough space in your inventory!"))
        }
    }

    private fun removeFromState(state: EconomyRuntimeState, slot: Int, itemId: Int, amount: Int) {
        val item = state.priceCheckerItems.getOrNull(slot) ?: return
        if (item.id != itemId) return
        if (amount >= item.amount) state.priceCheckerItems.removeAt(slot) else item.amount -= amount
    }
}
