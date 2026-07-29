package net.dodian.uber.game.engine.systems.inventory

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.persistence.player.PlayerSaveSegment
import java.util.Collections
import java.util.IdentityHashMap
import net.dodian.uber.game.social.exchange.ExchangeRuntime

/**
 * Staged, all-or-nothing mutation of one player's live inventory arrays.
 * No client-visible or persistent state changes until [commit] succeeds.
 */
class ClientInventoryTransaction internal constructor(
    private val client: Client,
) {
    private val itemIds = client.playerItems.copyOf()
    private val amounts = client.playerItemsN.copyOf()
    private var failed = false
    private var changed = false

    fun require(itemId: Int, amount: Int = 1): Boolean {
        if (!valid(itemId, amount) || amountOf(itemId) < amount) {
            failed = true
        }
        return !failed
    }

    fun remove(itemId: Int, amount: Int = 1): Boolean {
        if (!require(itemId, amount)) return false
        var remaining = amount
        for (slot in itemIds.indices) {
            if (itemIds[slot] != itemId + 1 || remaining == 0) continue
            val removed = minOf(
                (amounts[slot] - ExchangeRuntime.reservedAt(client, slot, itemId)).coerceAtLeast(0),
                remaining,
            )
            if (removed == 0) continue
            amounts[slot] -= removed
            remaining -= removed
            if (amounts[slot] == 0) itemIds[slot] = 0
        }
        changed = true
        return true
    }

    fun removeAt(slot: Int, itemId: Int, amount: Int = 1): Boolean {
        if (!valid(itemId, amount) || slot !in itemIds.indices || itemIds[slot] != itemId + 1 ||
            amounts[slot] < amount || !ExchangeRuntime.mayRemove(client, slot, itemId, amount)
        ) {
            failed = true
            return false
        }
        amounts[slot] -= amount
        if (amounts[slot] == 0) itemIds[slot] = 0
        changed = true
        return true
    }

    fun add(itemId: Int, amount: Int = 1): Boolean {
        if (!valid(itemId, amount) || failed) {
            failed = true
            return false
        }
        if (Server.itemManager.isStackable(itemId)) {
            val slot = itemIds.indexOfFirst { it == itemId + 1 }
            if (slot >= 0) {
                if (amounts[slot].toLong() + amount > client.maxItemAmount) {
                    failed = true
                    return false
                }
                amounts[slot] += amount
            } else {
                val empty = itemIds.indexOfFirst { it <= 0 }
                if (empty < 0 || amount > client.maxItemAmount) {
                    failed = true
                    return false
                }
                itemIds[empty] = itemId + 1
                amounts[empty] = amount
            }
        } else {
            val emptySlots = itemIds.indices.filter { itemIds[it] <= 0 }
            if (emptySlots.size < amount) {
                failed = true
                return false
            }
            emptySlots.take(amount).forEach { slot ->
                itemIds[slot] = itemId + 1
                amounts[slot] = 1
            }
        }
        changed = true
        return true
    }

    fun amountOf(itemId: Int): Int =
        itemIds.indices.sumOf { slot ->
            if (itemIds[slot] == itemId + 1) {
                (amounts[slot] - ExchangeRuntime.reservedAt(client, slot, itemId)).coerceAtLeast(0).toLong()
            } else 0L
        }
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()

    fun commit(): Boolean {
        if (failed || !ExchangeRuntime.mayMutateInventory(client)) return false
        if (!changed) return true
        System.arraycopy(itemIds, 0, client.playerItems, 0, itemIds.size)
        System.arraycopy(amounts, 0, client.playerItemsN, 0, amounts.size)
        client.markSaveDirty(PlayerSaveSegment.INVENTORY.mask)
        client.checkItemUpdate()
        return true
    }

    private fun valid(itemId: Int, amount: Int): Boolean = itemId >= 0 && amount > 0
}

private val activeTransactions = Collections.newSetFromMap(IdentityHashMap<Client, Boolean>())

fun Client.inventoryTransaction(block: ClientInventoryTransaction.() -> Unit): Boolean {
    check(activeTransactions.add(this)) { "Nested inventory transaction for ${playerName}" }
    return try {
        ClientInventoryTransaction(this).apply(block).commit()
    } finally {
        activeTransactions.remove(this)
    }
}
