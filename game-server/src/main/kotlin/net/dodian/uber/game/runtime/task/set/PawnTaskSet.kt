package net.dodian.uber.game.runtime.task.set

import kotlin.coroutines.resume
import net.dodian.uber.game.runtime.task.GameTaskSet
import net.dodian.uber.game.runtime.task.TaskPriority

class PawnTaskSet<OWNER : Any>(
    owner: OWNER,
    private val isStandardBlocked: (OWNER) -> Boolean = { false },
) : GameTaskSet<OWNER>(owner) {
    override fun cycle() {
        cleanupFinished()
        while (true) {
            val task = queue.peekFirst() ?: return
            if (task.priority == TaskPriority.STANDARD && isStandardBlocked(owner)) {
                return
            }
            if (!task.invoked) {
                task.invoked = true
                task.coroutine.resume(Unit)
            }
            task.cycle()
            if (task.isFinished()) {
                queue.remove(task)
                continue
            }
            return
        }
    }
}
