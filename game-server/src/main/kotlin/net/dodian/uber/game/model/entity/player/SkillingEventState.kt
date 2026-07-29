package net.dodian.uber.game.model.entity.player

data class SkillingEventState(
    val isRandomEventOpen: Boolean,
    val randomSkillId: Int,
    val chestEventCount: Int,
    val isChestEventPendingMove: Boolean,
    val isSecondaryRandomEventPending: Boolean = false,
) {
    fun withRandomEventOpen(value: Boolean) = copy(isRandomEventOpen = value)
    fun withRandomSkillId(value: Int) = copy(randomSkillId = value)
    fun withChestEventCount(value: Int) = copy(chestEventCount = value)
    fun withChestEventPendingMove(value: Boolean) = copy(isChestEventPendingMove = value)
    fun withSecondaryRandomEventPending(value: Boolean) = copy(isSecondaryRandomEventPending = value)
}
