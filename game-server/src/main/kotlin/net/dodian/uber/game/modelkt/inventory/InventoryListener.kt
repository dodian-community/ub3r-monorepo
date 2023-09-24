package net.dodian.uber.game.modelkt.inventory

import net.dodian.uber.game.modelkt.Item

interface InventoryListener {
    fun capacityExceeded(inventory: Inventory)
    fun itemsUpdated(inventory: Inventory)
    fun itemUpdated(inventory: Inventory, slot: Int, item: Item)
}