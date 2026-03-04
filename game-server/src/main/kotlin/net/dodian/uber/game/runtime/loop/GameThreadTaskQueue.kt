package net.dodian.uber.game.runtime.loop

import java.util.concurrent.ConcurrentLinkedQueue
import org.slf4j.LoggerFactory

object GameThreadTaskQueue {
    private val logger = LoggerFactory.getLogger(GameThreadTaskQueue::class.java)
    private val queue = ConcurrentLinkedQueue<Runnable>()

    @JvmStatic
    fun submit(task: Runnable) {
        queue.add(task)
    }

    @JvmStatic
    fun drain() {
        drain(10_000)
    }

    @JvmStatic
    fun drain(maxTasks: Int) {
        var processed = 0
        while (processed < maxTasks) {
            val task = queue.poll() ?: break
            try {
                task.run()
            } catch (exception: Throwable) {
                logger.warn("Game thread task failed", exception)
            }
            processed++
        }
        if (processed >= maxTasks && queue.isNotEmpty()) {
            logger.warn("GameThreadTaskQueue reached maxTasks={}, remaining={}", maxTasks, queue.size)
        }
    }
}
