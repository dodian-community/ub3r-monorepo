package net.dodian.uber.game.model.item

class Equipment : ItemContainer(SIZE) {
    fun getSlot(slot: Slot): GameItem = getSlot(slot.id)

    fun setSlot(slot: Slot, item: GameItem) {
        setSlot(slot.id, item)
    }

    enum class Slot(val id: Int) {
        HEAD(0),
        CAPE(1),
        NECK(2),
        WEAPON(3),
        CHEST(4),
        SHIELD(5),
        LEGS(7),
        BLESSING(8),
        HANDS(9),
        FEET(10),
        RING(12),
        ARROWS(13),
    }

    companion object {
        const val SIZE: Int = 14
    }
}
