package net.dodian.uber.game.sync.block

data class InteractingMobBlock(
    val index: Int
) : SynchronizationBlock {

    companion object {
        const val RESET_INDEX = 65_535
        fun createFrom(index: Int) = InteractingMobBlock(index)
    }
}