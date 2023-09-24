package net.dodian.uber.game.action

import net.dodian.uber.game.modelkt.entity.Mob
import net.dodian.uber.game.scheduling.ScheduledTask

abstract class Action<T : Mob>(
    val mob: Mob,
    delay: Int,
    immediate: Boolean = false
) : ScheduledTask(delay, immediate) {
    private var stopping = false

    override fun stop() {
        super.stop()
        if (stopping) return

        stopping = true
        mob.stopAction()
    }
}