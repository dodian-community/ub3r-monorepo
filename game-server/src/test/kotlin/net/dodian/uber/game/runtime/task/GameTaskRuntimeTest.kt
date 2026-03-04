package net.dodian.uber.game.runtime.task

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.runtime.queue.QueueTask
import net.dodian.uber.game.runtime.queue.QueueTaskService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameTaskRuntimeTest {

    @AfterEach
    fun tearDown() {
        GameTaskRuntime.clear()
        PlayerHandler.playersOnline.clear()
    }

    @Test
    fun `player queue runs sequentially per owner`() {
        val player = activeClient(1, 100)
        val trace = mutableListOf<String>()

        GameTaskRuntime.queuePlayer(player) {
            trace += "first-start"
            wait(1)
            trace += "first-end"
        }
        GameTaskRuntime.queuePlayer(player) {
            trace += "second"
        }

        GameTaskRuntime.cycle()
        assertEquals(listOf("second", "first-start", "first-end"), trace)

        GameTaskRuntime.cycle()
        assertEquals(listOf("second", "first-start", "first-end"), trace)
    }

    @Test
    fun `world queue advances all tasks and wait resumes on exact tick`() {
        val trace = mutableListOf<String>()

        GameTaskRuntime.queueWorld {
            trace += "a0"
            wait(2)
            trace += "a1"
        }
        GameTaskRuntime.queueWorld {
            trace += "b0"
            wait(1)
            trace += "b1"
        }

        GameTaskRuntime.cycle()
        assertEquals(listOf("b0", "b1", "a0"), trace)

        GameTaskRuntime.cycle()
        assertEquals(listOf("b0", "b1", "a0", "a1"), trace)
    }

    @Test
    fun `wait return value resumes once submitted`() {
        val player = activeClient(2, 200)
        val key = TaskRequestKey<String>("dialogue")
        val trace = mutableListOf<String>()

        GameTaskRuntime.queuePlayer(player) {
            trace += "waiting"
            trace += waitReturnValue(key)
        }

        GameTaskRuntime.cycle()
        assertEquals(listOf("waiting"), trace)

        GameTaskRuntime.submitReturnValue(player, key, "done")
        GameTaskRuntime.cycle()
        assertEquals(listOf("waiting", "done"), trace)
    }

    @Test
    fun `strong priority preempts weaker player tasks`() {
        val player = activeClient(3, 300)
        val trace = mutableListOf<String>()

        val weakHandle =
            GameTaskRuntime.queuePlayer(player, TaskPriority.WEAK) {
                trace += "weak"
                wait(1)
            }

        GameTaskRuntime.queuePlayer(player, TaskPriority.STRONG) {
            trace += "strong"
        }

        GameTaskRuntime.cycle()
        assertEquals(listOf("strong"), trace)
        assertTrue(weakHandle.isCompleted())
    }

    @Test
    fun `standard player task pauses while blocking dialogue is open`() {
        val player = activeClient(4, 400)
        player.NpcDialogue = 1
        val trace = mutableListOf<String>()

        GameTaskRuntime.queuePlayer(player) {
            trace += "ran"
        }

        GameTaskRuntime.cycle()
        assertTrue(trace.isEmpty())

        player.NpcDialogue = 0
        GameTaskRuntime.cycle()
        assertEquals(listOf("ran"), trace)
    }

    @Test
    fun `queue task service preserves delay repeat and cancellation`() {
        val trace = mutableListOf<Int>()
        val handle =
            QueueTaskService.schedule(2, 1, QueueTask {
                trace += trace.size + 1
                trace.size < 3
            })

        QueueTaskService.processDue()
        assertTrue(trace.isEmpty())

        QueueTaskService.processDue()
        assertEquals(listOf(1), trace)

        QueueTaskService.processDue()
        assertEquals(listOf(1, 2), trace)

        handle.cancel()
        QueueTaskService.processDue()
        assertEquals(listOf(1, 2), trace)
        assertTrue(handle.isCancelled())
        assertTrue(handle.isCompleted())
    }

    private fun activeClient(slot: Int, dbId: Int): Client {
        val client = Client(EmbeddedChannel(), slot)
        client.dbId = dbId
        client.isActive = true
        client.disconnected = false
        PlayerHandler.playersOnline[slot.toLong()] = client
        return client
    }
}
