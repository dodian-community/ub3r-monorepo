package net.dodian.uber.game.modelkt.inventory

import net.dodian.uber.game.modelkt.Item

abstract class InventoryAdapter : InventoryListener {
    override fun capacityExceeded(inventory: Inventory) {}
    override fun itemsUpdated(inventory: Inventory) {}
    override fun itemUpdated(inventory: Inventory, slot: Int, item: Item) {}
}