package net.dodian.uber.game.model.item

import net.dodian.uber.game.Server

class GameItem(
    @JvmField val id: Int,
    @JvmField var amount: Int,
) {
    private val stackable: Boolean = Server.itemManager?.isStackable(id) ?: false

    fun getId(): Int = id

    fun getAmount(): Int = amount

    fun addAmount(amount: Int) {
        this.amount += amount
    }

    fun removeAmount(amount: Int) {
        this.amount -= amount
    }

    fun isStackable(): Boolean = stackable
}
