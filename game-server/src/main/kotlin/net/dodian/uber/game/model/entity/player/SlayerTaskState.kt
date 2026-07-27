package net.dodian.uber.game.model.entity.player

data class SlayerTaskState(
    val masterNpcId: Int,
    val taskOrdinal: Int,
    val assignmentAmount: Int,
    val remainingAmount: Int,
    val streak: Int,
    val points: Int,
    val blockedTaskOrdinal: Int,
) {
    fun withRemainingAmount(value: Int) = copy(remainingAmount = value)

    fun withAssignment(masterId: Int, ordinal: Int, assignment: Int, remaining: Int) =
        copy(masterNpcId = masterId, taskOrdinal = ordinal, assignmentAmount = assignment, remainingAmount = remaining)

    fun withMasterNpcId(value: Int) = copy(masterNpcId = value)
    fun withTaskOrdinal(value: Int) = copy(taskOrdinal = value)
    fun withStreak(value: Int) = copy(streak = value)
    fun withPoints(value: Int) = copy(points = value)
}
