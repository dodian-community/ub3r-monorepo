package net.dodian.uber.game.modelkt

import org.apollo.cache.def.ItemDefinition

data class Item(
    val id: Int,
    val amount: Int = 1,
    val definition: ItemDefinition = ItemDefinition.lookup(id)
) {
    override fun toString() = "Item(id=$id, amount=$amount)"
}