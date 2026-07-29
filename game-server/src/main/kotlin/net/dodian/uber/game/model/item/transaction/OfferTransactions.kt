package net.dodian.uber.game.model.item.transaction

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.GameItem
import net.dodian.uber.game.social.exchange.ExchangeRuntime

/** Atomic projection and publication for reservation-backed player exchanges. */
object OfferTransactions {
    data class InventoryImage(
        val itemIds: IntArray,
        val amounts: IntArray,
    ) {
        init {
            require(itemIds.size == amounts.size)
        }

        fun matches(client: Client): Boolean =
            itemIds.contentEquals(client.playerItems) && amounts.contentEquals(client.playerItemsN)

        fun publish(client: Client) {
            System.arraycopy(itemIds, 0, client.playerItems, 0, itemIds.size)
            System.arraycopy(amounts, 0, client.playerItemsN, 0, amounts.size)
        }
    }

    data class SettlementProjection(
        val firstBefore: InventoryImage,
        val secondBefore: InventoryImage,
        val firstAfter: InventoryImage,
        val secondAfter: InventoryImage,
    )

    /**
     * Applies a reservation-backed trade. Negotiation never moved the items,
     * so settlement removes each exact reserved quantity from its owner and
     * inserts the other participant's offer in one transaction.
     */
    @JvmStatic
    fun projectReservedTrade(first: Client, second: Client): SettlementProjection? {
        val session = ExchangeRuntime.session(first, second, net.dodian.uber.game.api.plugin.social.ExchangeKind.TRADE)
            ?: return null
        val firstReservations = session.reservationSnapshot(ExchangeRuntime.participant(first))
        val secondReservations = session.reservationSnapshot(ExchangeRuntime.participant(second))
        val firstInv = first.selectInv()
        val secondInv = second.selectInv()
        val projected = ExchangeRuntime.settlementMutation {
            project(firstInv, secondInv) {
                firstReservations.forEach { reservation ->
                    delete {
                        from = firstInv.transactionInv
                        obj = reservation.itemId
                        strictSlot = reservation.inventorySlot
                        strictCount = reservation.amount
                    }
                }
                secondReservations.forEach { reservation ->
                    delete {
                        from = secondInv.transactionInv
                        obj = reservation.itemId
                        strictSlot = reservation.inventorySlot
                        strictCount = reservation.amount
                    }
                }
                aggregate(secondReservations).forEach { (itemId, amount) ->
                    insert { into = firstInv.transactionInv; obj = itemId; strictCount = amount }
                }
                aggregate(firstReservations).forEach { (itemId, amount) ->
                    insert { into = secondInv.transactionInv; obj = itemId; strictCount = amount }
                }
            }
        }
        return projected?.let { (firstAfter, secondAfter) ->
            SettlementProjection(snapshot(first), snapshot(second), firstAfter, secondAfter)
        }
    }

    /**
     * Releases the winner's stake in place and transfers only the loser's
     * reserved quantities. Both live inventories change atomically.
     */
    @JvmStatic
    fun projectReservedDuelPayout(winner: Client, loser: Client): SettlementProjection? {
        val session = ExchangeRuntime.session(winner, loser, net.dodian.uber.game.api.plugin.social.ExchangeKind.DUEL)
            ?: return null
        val loserReservations = session.reservationSnapshot(ExchangeRuntime.participant(loser))
        val winnerInv = winner.selectInv()
        val loserInv = loser.selectInv()
        val projected = ExchangeRuntime.settlementMutation {
            project(winnerInv, loserInv) {
                loserReservations.forEach { reservation ->
                    delete {
                        from = loserInv.transactionInv
                        obj = reservation.itemId
                        strictSlot = reservation.inventorySlot
                        strictCount = reservation.amount
                    }
                }
                aggregate(loserReservations).forEach { (itemId, amount) ->
                    insert { into = winnerInv.transactionInv; obj = itemId; strictCount = amount }
                }
            }
        }
        return projected?.let { (winnerAfter, loserAfter) ->
            SettlementProjection(snapshot(winner), snapshot(loser), winnerAfter, loserAfter)
        }
    }

    private fun aggregate(
        reservations: List<net.dodian.uber.game.api.plugin.social.ExchangeReservation>,
    ): Map<Int, Int> {
        val result = linkedMapOf<Int, Int>()
        reservations.forEach {
            result[it.itemId] = Math.addExact(result[it.itemId] ?: 0, it.amount)
        }
        return result
    }

    private fun project(
        first: SelectInv,
        second: SelectInv,
        block: Transaction<GameItem?>.() -> Unit,
    ): Pair<InventoryImage, InventoryImage>? {
        val tx = buildTransaction()
        tx.register(first.transactionInv)
        tx.register(second.transactionInv)
        return try {
            tx.block()
            if (!first.canCommit() || !second.canCommit()) return null
            image(first) to image(second)
        } catch (_: TransactionCancellation) {
            null
        }
    }

    private fun image(selected: SelectInv): InventoryImage {
        val ids = IntArray(selected.transactionInv.image.size)
        val amounts = IntArray(selected.transactionInv.image.size)
        selected.transactionInv.image.forEachIndexed { slot, obj ->
            if (obj != null) {
                ids[slot] = obj.id + 1
                amounts[slot] = obj.count
            }
        }
        return InventoryImage(ids, amounts)
    }

    private fun snapshot(client: Client): InventoryImage =
        InventoryImage(client.playerItems.copyOf(), client.playerItemsN.copyOf())

    @JvmStatic
    fun publishProjection(first: Client, second: Client, projection: SettlementProjection): Boolean {
        if (!projection.firstBefore.matches(first) || !projection.secondBefore.matches(second)) return false
        return ExchangeRuntime.settlementMutation {
            projection.firstAfter.publish(first)
            projection.secondAfter.publish(second)
            first.markSaveDirty(net.dodian.uber.game.persistence.player.PlayerSaveSegment.INVENTORY.mask)
            second.markSaveDirty(net.dodian.uber.game.persistence.player.PlayerSaveSegment.INVENTORY.mask)
            first.checkItemUpdate()
            second.checkItemUpdate()
            true
        }
    }
}
