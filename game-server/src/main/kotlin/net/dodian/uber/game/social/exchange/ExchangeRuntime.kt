package net.dodian.uber.game.social.exchange

import java.util.UUID
import net.dodian.uber.game.Server
import net.dodian.uber.game.api.plugin.social.ExchangeCommandResult
import net.dodian.uber.game.api.plugin.social.ExchangeKind
import net.dodian.uber.game.api.plugin.social.ExchangeParticipantId
import net.dodian.uber.game.api.plugin.social.ExchangeReservation
import net.dodian.uber.game.api.plugin.social.ExchangeStage
import net.dodian.uber.game.api.plugin.social.runtime.ExchangeSession
import net.dodian.uber.game.api.plugin.social.runtime.ExchangeSessionRegistry
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.GameItem

/**
 * Game-thread-owned bridge between engine players and the engine-independent
 * social exchange aggregate.
 *
 * Offered items remain in the live inventory. A reservation is therefore both
 * the offer and the authoritative guard preventing another subsystem from
 * spending the same quantity.
 */
object ExchangeRuntime {
    private val registry = ExchangeSessionRegistry()
    private val settlementBypass = ThreadLocal.withInitial { 0 }

    @JvmStatic
    fun participant(client: Client): ExchangeParticipantId =
        ExchangeParticipantId(client.dbId, client.slot, client.session_start)

    @JvmStatic
    fun create(kind: ExchangeKind, first: Client, second: Client): ExchangeSession {
        remove(first)
        remove(second)
        return registry.create(kind, participant(first), participant(second))
    }

    @JvmStatic
    fun session(client: Client): ExchangeSession? {
        val participant = participant(client)
        return registry.session(participant)?.takeIf {
            it.first == participant || it.second == participant
        }
    }

    @JvmStatic
    fun session(first: Client, second: Client, kind: ExchangeKind): ExchangeSession? {
        val session = session(first) ?: return null
        return session.takeIf {
            it.kind == kind && it.other(participant(first)) == participant(second)
        }
    }

    @JvmStatic
    fun remove(client: Client): Boolean {
        val session = session(client) ?: return false
        return registry.remove(session)
    }

    @JvmStatic
    fun remove(session: ExchangeSession): Boolean = registry.remove(session)

    @JvmStatic
    fun offers(client: Client): List<GameItem> {
        val session = session(client) ?: return emptyList()
        val reservations = session.reservationSnapshot(participant(client))
        val result = mutableListOf<GameItem>()
        reservations.forEach { reservation ->
            if (isStackable(reservation.itemId)) {
                val existing = result.firstOrNull { it.id == reservation.itemId }
                if (existing == null) {
                    result += GameItem(reservation.itemId, reservation.amount)
                } else {
                    existing.amount = Math.addExact(existing.amount, reservation.amount)
                }
            } else {
                repeat(reservation.amount) { result += GameItem(reservation.itemId, 1) }
            }
        }
        return result
    }

    @JvmStatic
    fun reserve(client: Client, slot: Int, itemId: Int, amount: Int): Boolean {
        if (amount <= 0 || slot !in client.playerItems.indices ||
            client.playerItems[slot] != itemId + 1
        ) return false
        val session = session(client) ?: return false
        if (session.stage != ExchangeStage.OFFER) return false
        val participant = participant(client)
        val current = session.reservationSnapshot(participant)
        val updated = current.toMutableList()
        var remaining = amount
        val candidateSlots =
            if (isStackable(itemId)) listOf(slot)
            else listOf(slot) + client.playerItems.indices.filter { it != slot && client.playerItems[it] == itemId + 1 }
        candidateSlots.forEach { candidate ->
            if (remaining == 0) return@forEach
            val reserved = current.asSequence()
                .filter { it.inventorySlot == candidate && it.itemId == itemId }
                .sumOf { it.amount }
            val available = (client.playerItemsN[candidate] - reserved).coerceAtLeast(0)
            val taken = minOf(available, remaining)
            if (taken == 0) return@forEach
            val existing = updated.indexOfFirst { it.inventorySlot == candidate && it.itemId == itemId }
            if (existing >= 0) {
                val old = updated[existing]
                updated[existing] = old.copy(amount = Math.addExact(old.amount, taken))
            } else {
                updated += ExchangeReservation(candidate, itemId, taken)
            }
            remaining -= taken
        }
        if (remaining != 0) return false
        return session.replaceReservations(participant, updated) is ExchangeCommandResult.Applied
    }

    @JvmStatic
    fun withdraw(client: Client, offerSlot: Int, itemId: Int, amount: Int): Boolean {
        if (amount <= 0) return false
        val session = session(client) ?: return false
        if (session.stage != ExchangeStage.OFFER) return false
        val participant = participant(client)
        val current = session.reservationSnapshot(participant).toMutableList()
        val displayed = displaySlices(current)
        val selected = displayed.getOrNull(offerSlot) ?: return false
        if (selected.itemId != itemId) return false

        var remaining = minOf(amount, selected.amount)
        for (index in current.indices.reversed()) {
            if (remaining == 0) break
            val reservation = current[index]
            if (reservation.itemId != itemId) continue
            val released = minOf(reservation.amount, remaining)
            remaining -= released
            if (released == reservation.amount) current.removeAt(index)
            else current[index] = reservation.copy(amount = reservation.amount - released)
        }
        if (remaining != 0) return false
        return session.replaceReservations(participant, current) is ExchangeCommandResult.Applied
    }

    @JvmStatic
    fun reservedAt(client: Client, slot: Int, itemId: Int): Int =
        session(client)?.reservationSnapshot(participant(client))
            ?.asSequence()
            ?.filter { it.inventorySlot == slot && it.itemId == itemId }
            ?.sumOf { it.amount.toLong() }
            ?.coerceAtMost(Int.MAX_VALUE.toLong())
            ?.toInt()
            ?: 0

    @JvmStatic
    fun hasReservationAt(client: Client, slot: Int): Boolean =
        session(client)?.reservationSnapshot(participant(client))?.any { it.inventorySlot == slot } == true

    @JvmStatic
    fun availableAt(client: Client, slot: Int, itemId: Int): Int {
        if (slot !in client.playerItems.indices || client.playerItems[slot] != itemId + 1) return 0
        return (client.playerItemsN[slot] - reservedAt(client, slot, itemId)).coerceAtLeast(0)
    }

    @JvmStatic
    fun mayRemove(client: Client, slot: Int, itemId: Int, amount: Int): Boolean {
        if (settlementBypass.get() > 0) return true
        val session = session(client)
        if (session != null && !ordinaryInventoryMutationAllowed(session)) return false
        return amount > 0 && availableAt(client, slot, itemId) >= amount
    }

    @JvmStatic
    fun mayMutateInventory(client: Client): Boolean {
        if (settlementBypass.get() > 0) return true
        return session(client)?.let(::ordinaryInventoryMutationAllowed) ?: true
    }

    fun <T> settlementMutation(block: () -> T): T {
        settlementBypass.set(settlementBypass.get() + 1)
        return try {
            block()
        } finally {
            settlementBypass.set((settlementBypass.get() - 1).coerceAtLeast(0))
        }
    }

    @JvmStatic
    fun adjustedInventory(client: Client): List<GameItem> =
        client.playerItems.indices.map { slot ->
            val itemId = client.playerItems[slot] - 1
            if (itemId < 0) GameItem(-1, 0)
            else GameItem(itemId, availableAt(client, slot, itemId))
        }

    @JvmStatic
    fun adjustedItemIds(client: Client): IntArray =
        IntArray(client.playerItems.size) { slot ->
            val itemId = client.playerItems[slot] - 1
            if (itemId >= 0 && availableAt(client, slot, itemId) > 0) itemId else -1
        }

    @JvmStatic
    fun adjustedItemAmounts(client: Client): IntArray =
        IntArray(client.playerItems.size) { slot ->
            val itemId = client.playerItems[slot] - 1
            if (itemId >= 0) availableAt(client, slot, itemId) else 0
        }

    private data class DisplaySlice(val itemId: Int, val amount: Int)

    private fun displaySlices(reservations: List<ExchangeReservation>): List<DisplaySlice> {
        val result = mutableListOf<DisplaySlice>()
        reservations.forEach { reservation ->
            if (isStackable(reservation.itemId)) {
                val index = result.indexOfFirst { it.itemId == reservation.itemId }
                if (index < 0) result += DisplaySlice(reservation.itemId, reservation.amount)
                else result[index] = result[index].copy(amount = Math.addExact(result[index].amount, reservation.amount))
            } else {
                repeat(reservation.amount) { result += DisplaySlice(reservation.itemId, 1) }
            }
        }
        return result
    }

    private fun isStackable(itemId: Int): Boolean = Server.itemManager?.isStackable(itemId) ?: false

    private fun ordinaryInventoryMutationAllowed(
        session: ExchangeSession,
    ): Boolean = when (session.stage) {
        ExchangeStage.CANCELLED, ExchangeStage.COMPLETE -> true
        ExchangeStage.ACTIVE -> session.kind == ExchangeKind.DUEL
        ExchangeStage.REQUESTED, ExchangeStage.OFFER, ExchangeStage.CONFIRM, ExchangeStage.SETTLING -> false
    }
}
