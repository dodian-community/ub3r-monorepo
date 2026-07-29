package net.dodian.uber.game.engine.systems.inventory

import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.loop.GameThreadContext
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.persistence.player.PlayerSaveSegment
import net.dodian.uber.game.social.exchange.ExchangeRuntime

/**
 * Stages changes to one or more player-owned item containers.  A container is
 * never written until every requested operation has been validated.  This is
 * deliberately game-thread owned; callers must not retain a transaction over
 * a tick or expose it to asynchronous work.
 */
class EconomyTransaction private constructor() {
    private val arrays = linkedMapOf<ArrayKey, ArrayContainer>()
    private var failed = false

    fun inventory(client: Client): Items = array(client, Container.INVENTORY)
    fun bank(client: Client): Items = array(client, Container.BANK)
    fun equipment(client: Client): Items = array(client, Container.EQUIPMENT)

    fun failed(): Boolean = failed

    fun commit(): Boolean {
        GameThreadContext.validateGameThread("economy.transaction.commit")
        if (failed || arrays.values.any { it.failed || !it.mayCommit() }) return false
        arrays.values.forEach { it.commit() }
        return true
    }

    private fun array(client: Client, kind: Container): ArrayContainer {
        val key = ArrayKey(client, kind)
        return arrays.getOrPut(key) { ArrayContainer(client, kind) }
    }

    private data class ArrayKey(val client: Client, val kind: Container)
    private enum class Container { INVENTORY, BANK, EQUIPMENT }

    /** Item ids supplied to this API are always unencoded game ids. */
    interface Items {
        fun amountOf(itemId: Int): Int
        fun require(itemId: Int, amount: Int = 1): Boolean
        fun remove(itemId: Int, amount: Int = 1): Boolean
        fun removeAt(slot: Int, itemId: Int, amount: Int = 1): Boolean
        fun add(itemId: Int, amount: Int = 1): Boolean
        fun setAt(slot: Int, itemId: Int, amount: Int = 1): Boolean
        fun replaceAt(slot: Int, removeItemId: Int, addItemId: Int, amount: Int = 1): Boolean
    }

    private class ArrayContainer(
        private val client: Client,
        private val kind: Container,
    ) : Items {
        private val ids: IntArray
        private val amounts: IntArray
        private var changed = false
        var failed = false
            private set

        init {
            val sourceIds = when (kind) {
                Container.INVENTORY -> client.playerItems
                Container.BANK -> client.bankItems
                Container.EQUIPMENT -> client.equipment
            }
            val sourceAmounts = when (kind) {
                Container.INVENTORY -> client.playerItemsN
                Container.BANK -> client.bankItemsN
                Container.EQUIPMENT -> client.equipmentN
            }
            ids = sourceIds.copyOf()
            amounts = sourceAmounts.copyOf()
        }

        override fun amountOf(itemId: Int): Int = ids.indices.sumOf { slot ->
            if (decode(ids[slot]) == itemId) {
                val reserved = if (kind == Container.INVENTORY) ExchangeRuntime.reservedAt(client, slot, itemId) else 0
                (amounts[slot] - reserved).coerceAtLeast(0).toLong()
            } else 0L
        }.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

        override fun require(itemId: Int, amount: Int): Boolean =
            valid(itemId, amount) && amountOf(itemId) >= amount || fail()

        override fun remove(itemId: Int, amount: Int): Boolean {
            if (!require(itemId, amount)) return false
            var remaining = amount
            for (slot in ids.indices) {
                if (decode(ids[slot]) != itemId || remaining == 0) continue
                val available = if (kind == Container.INVENTORY) {
                    (amounts[slot] - ExchangeRuntime.reservedAt(client, slot, itemId)).coerceAtLeast(0)
                } else amounts[slot]
                val removed = minOf(available, remaining)
                if (removed == 0) continue
                amounts[slot] -= removed
                remaining -= removed
                if (amounts[slot] == 0) ids[slot] = 0
            }
            changed = true
            return true
        }

        override fun removeAt(slot: Int, itemId: Int, amount: Int): Boolean {
            return removeAt(slot, itemId, amount, retainIdOnZero = false)
        }

        fun removeAt(slot: Int, itemId: Int, amount: Int, retainIdOnZero: Boolean): Boolean {
            if (!valid(itemId, amount) || slot !in ids.indices || decode(ids[slot]) != itemId || amounts[slot] < amount) return fail()
            if (kind == Container.INVENTORY && !ExchangeRuntime.mayRemove(client, slot, itemId, amount)) return fail()
            amounts[slot] -= amount
            if (amounts[slot] == 0 && !(retainIdOnZero && kind == Container.BANK)) ids[slot] = 0
            changed = true
            return true
        }

        override fun add(itemId: Int, amount: Int): Boolean {
            if (!valid(itemId, amount) || failed) return fail()
            // Banks represent every item type as a single quantity-bearing
            // entry, while inventory/equipment preserve non-stackable slots.
            val stackable = kind == Container.BANK || Server.itemManager.isStackable(itemId)
            if (stackable) {
                val existing = ids.indexOfFirst { decode(it) == itemId }
                val slot = if (existing >= 0) existing else ids.indexOfFirst { it <= 0 }
                if (slot < 0 || amounts[slot].toLong() + amount > client.maxItemAmount) return fail()
                ids[slot] = encode(itemId)
                amounts[slot] += amount
            } else {
                val empty = ids.indices.filter { ids[it] <= 0 }
                if (empty.size < amount) return fail()
                empty.take(amount).forEach { slot ->
                    ids[slot] = encode(itemId)
                    amounts[slot] = 1
                }
            }
            changed = true
            return true
        }

        override fun setAt(slot: Int, itemId: Int, amount: Int): Boolean {
            if (kind != Container.EQUIPMENT || !valid(itemId, amount) || slot !in ids.indices) return fail()
            ids[slot] = itemId
            amounts[slot] = amount
            changed = true
            return true
        }

        override fun replaceAt(slot: Int, removeItemId: Int, addItemId: Int, amount: Int): Boolean {
            if (!removeAt(slot, removeItemId, amount)) return false
            return add(addItemId, amount)
        }

        fun commit() {
            if (!changed) return
            if (kind == Container.EQUIPMENT) {
                client.replaceEquipmentState(ids, amounts)
                return
            }
            val targetIds = when (kind) {
                Container.INVENTORY -> client.playerItems
                Container.BANK -> client.bankItems
                Container.EQUIPMENT -> error("Equipment is committed through Client.replaceEquipmentState")
            }
            val targetAmounts = when (kind) {
                Container.INVENTORY -> client.playerItemsN
                Container.BANK -> client.bankItemsN
                Container.EQUIPMENT -> error("Equipment is committed through Client.replaceEquipmentState")
            }
            System.arraycopy(ids, 0, targetIds, 0, ids.size)
            System.arraycopy(amounts, 0, targetAmounts, 0, amounts.size)
            when (kind) {
                Container.INVENTORY -> {
                    client.markSaveDirty(PlayerSaveSegment.INVENTORY.mask)
                    client.checkItemUpdate()
                }
                Container.BANK -> client.markSaveDirty(PlayerSaveSegment.BANK.mask)
                Container.EQUIPMENT -> error("Equipment is committed through Client.replaceEquipmentState")
            }
        }

        fun mayCommit(): Boolean = kind != Container.INVENTORY || ExchangeRuntime.mayMutateInventory(client)

        private fun valid(itemId: Int, amount: Int) = itemId >= 0 && amount > 0
        private fun fail(): Boolean = false.also { failed = true }
        private fun encode(itemId: Int) = if (kind == Container.EQUIPMENT) itemId else itemId + 1
        private fun decode(raw: Int) = if (raw <= 0) -1 else if (kind == Container.EQUIPMENT) raw else raw - 1
    }

    companion object {
        @JvmStatic
        fun run(block: EconomyTransaction.() -> Unit): Boolean = EconomyTransaction().apply(block).commit()

        @JvmStatic
        fun addToInventory(client: Client, itemId: Int, amount: Int): Boolean = run {
            inventory(client).add(itemId, amount)
        }

        @JvmStatic
        fun removeFromInventory(client: Client, itemId: Int, slot: Int, amount: Int): Boolean = run {
            inventory(client).removeAt(slot, itemId, amount)
        }

        @JvmStatic
        fun replaceInventorySlot(client: Client, slot: Int, itemId: Int, amount: Int): Boolean = run {
            val inventory = inventory(client)
            val current = client.playerItems.getOrNull(slot)?.minus(1) ?: -1
            if (current >= 0) inventory.removeAt(slot, current, client.playerItemsN[slot])
            inventory.add(itemId, amount)
        }
    }
}
