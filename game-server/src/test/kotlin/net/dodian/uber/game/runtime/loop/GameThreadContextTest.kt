package net.dodian.uber.game.runtime.loop

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameThreadContextTest {

    @AfterEach
    fun tearDown() {
        GameThreadContext.clearBindingForTests()
    }

    @Test
    fun `is game thread is false when unbound`() {
        assertFalse(GameThreadContext.isGameThread())
    }

    @Test
    fun `bind current thread marks current as game thread`() {
        GameThreadContext.bindCurrentThread()
        assertTrue(GameThreadContext.isGameThread())
    }

    @Test
    fun `is game thread is false on other thread`() {
        GameThreadContext.bindCurrentThread()
        val isGameThreadOnOtherThread = AtomicBoolean(true)
        val latch = CountDownLatch(1)
        Thread {
            isGameThreadOnOtherThread.set(GameThreadContext.isGameThread())
            latch.countDown()
        }.start()
        latch.await()
        assertFalse(isGameThreadOnOtherThread.get())
    }
}
