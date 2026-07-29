package net.dodian.uber.game.model.entity.player

/** Persisted rune-pouch contents and immutable pouch definitions. */
class RunePouchState {
    private val amounts = IntArray(SLOT_COUNT)

    fun amount(slot: Int): Int = amounts[slot]
    fun setAmount(slot: Int, amount: Int) { amounts[slot] = amount }
    fun addAmount(slot: Int, amount: Int) { amounts[slot] += amount }
    fun levelRequirement(slot: Int): Int = LEVEL_REQUIREMENTS[slot]
    fun capacity(slot: Int): Int = CAPACITIES[slot]
    fun size(): Int = SLOT_COUNT
    fun saveValue(): String = amounts.joinToString(":")

    companion object {
        private const val SLOT_COUNT = 4
        private val LEVEL_REQUIREMENTS = intArrayOf(1, 20, 40, 60)
        private val CAPACITIES = intArrayOf(4, 7, 10, 13)
    }
}
