package net.dodian.uber.game.model.entity.player

data class AgilitySessionState(val courseStage: Int, val updatedAtCycle: Long) {
    fun withCourseStage(stage: Int, cycle: Long) = copy(courseStage = stage, updatedAtCycle = cycle)
}
