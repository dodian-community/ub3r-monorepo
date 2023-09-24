package net.dodian.uber.game.modelkt.inventory

import net.dodian.uber.game.modelkt.Item

data class Inventory(
    val capacity: Int,
    val firingEvents: Boolean = true,
    val items: MutableList<Item> = mutableListOf(),
    val mode: StackMode = StackMode.STACK_STACKABLE_ITEMS,
    val listeners: MutableList<InventoryListener> = mutableListOf()
) {
    val size: Int get() = items.size

    enum class StackMode {
        STACK_ALWAYS,
        STACK_NEVER,
        STACK_STACKABLE_ITEMS
    }
}