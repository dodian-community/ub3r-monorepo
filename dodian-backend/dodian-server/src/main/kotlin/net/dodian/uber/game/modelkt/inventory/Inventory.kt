package net.dodian.uber.game.modelkt.inventory

import net.dodian.uber.game.modelkt.Item

const val CAPACITY_EQUIPMENT = 14
const val CAPACITY_BANK = 352
const val CAPACITY_INVENTORY = 28

data class Inventory(
    val capacity: Int,
    val mode: StackMode = StackMode.STACK_STACKABLE_ITEMS,
    val firingEvents: Boolean = true,
    val items: MutableList<Item> = mutableListOf(),
    val listeners: MutableList<InventoryListener> = mutableListOf()
) {
    val size: Int get() = items.size

    operator fun get(slot: Int): Item? {
        checkBounds(slot)

        if (slot >= items.size)
            return null

        return items[slot]
    }

    fun checkBounds(vararg slots: Int) {
        if (slots.any { it >= capacity })
            error("A slot is out of bounds. (slots=$slots, capacity=$capacity)")
    }

    enum class StackMode {
        STACK_ALWAYS,
        STACK_NEVER,
        STACK_STACKABLE_ITEMS
    }
}