package net.dodian.uber.game.model.item

import java.util.ArrayList

open class ItemContainer(size: Int) {
    private val items = ArrayList<GameItem>(size)

    init {
        for (index in 0 until size) {
            items.add(GameItem(-1, 0))
        }
    }

    protected fun getSlot(slot: Int): GameItem = items[slot]

    protected fun setSlot(slot: Int, item: GameItem) {
        items[slot] = item
    }
}
