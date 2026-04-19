package net.dodian.uber.game.skill.runtime.action

interface SkillSessionState {
    val active: Boolean
    val startedCycle: Long
    val stopReason: ActionStopReason?
    val targetRef: String?
}
