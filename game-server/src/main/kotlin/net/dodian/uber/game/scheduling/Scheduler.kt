package net.dodian.uber.game.scheduling

import net.dodian.utilities.CollectionUtil
import java.util.*

class Scheduler(
    val pending: Queue<ScheduledTask> = ArrayDeque(),
    val active: MutableList<ScheduledTask> = mutableListOf()
) {

    fun schedule(task: ScheduledTask) = pending.add(task)

    fun pulse() {
        CollectionUtil.pollAll(pending, active::add)

        active.forEach { task ->
            task.pulse()

            if (!task.isRunning)
                active.remove(task)
        }
    }
}