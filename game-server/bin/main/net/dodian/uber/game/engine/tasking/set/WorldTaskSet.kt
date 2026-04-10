package net.dodian.uber.game.engine.tasking.set

import kotlin.coroutines.resume
import net.dodian.uber.game.engine.tasking.GameTaskSet

class WorldTaskSet : GameTaskSet<Any>(WORLD_OWNER) {
    override fun cycle() {
        cleanupFinished()
        val iterator = queue.iterator()
        while (iterator.hasNext()) {
            val task = iterator.next()
            if (!task.invoked) {
                task.invoked = true
                task.coroutine.resume(Unit)
            }
            task.cycle()
            if (task.isFinished()) {
                iterator.remove()
            }
        }
    }

    private companion object {
        private val WORLD_OWNER = Any()
    }
}
