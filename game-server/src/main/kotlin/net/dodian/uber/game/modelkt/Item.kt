package net.dodian.uber.game.modelkt

import net.dodian.uber.cache.definition.ItemDefinition

data class Item(
    val id: Int,
    val amount: Int = 1,
    val definition: ItemDefinition = ItemDefinition.byId(id)
) {
    override fun toString() = "Item(id=$id, amount=$amount)"
}