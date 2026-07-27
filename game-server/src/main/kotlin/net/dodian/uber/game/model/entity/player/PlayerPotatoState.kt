package net.dodian.uber.game.model.entity.player

data class PlayerPotatoState(
    val flowType: Int,
    val targetSlot: Int,
    val targetIdentifier: Int,
    val stage: Int,
) {
    val isActive: Boolean get() = stage == 1
}
