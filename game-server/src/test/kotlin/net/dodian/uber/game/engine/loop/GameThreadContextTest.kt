package net.dodian.uber.game.engine.loop

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameThreadContextTest {
    @BeforeEach
    fun setUp() {
        GameThreadContext.clearBindingForTests()
        GameThreadContext.resetDiagnosticsForTests()
        GameThreadContext.forceDebugModeForTests = false
    }

    @AfterEach
    fun tearDown() {
        GameThreadContext.clearBindingForTests()
        GameThreadContext.resetDiagnosticsForTests()
    }

    @Test
    fun `unbound violation is reported without throwing`() {
        assertFalse(GameThreadContext.validateGameThread("test.unbound"))
        assertEquals(1L, GameThreadContext.violationCount())
        assertEquals(1L, GameThreadContext.unboundViolationCount())
        assertEquals(0L, GameThreadContext.wrongThreadViolationCount())
    }

    @Test
    fun `bound game thread passes without a violation`() {
        GameThreadContext.bindCurrentThread()
        assertTrue(GameThreadContext.validateGameThread("test.correct"))
        assertEquals(0L, GameThreadContext.violationCount())
    }

    @Test
    fun `wrong thread violation is reported without throwing`() {
        GameThreadContext.bindCurrentThread()
        var result = true
        val worker = Thread({ result = GameThreadContext.validateGameThread("test.wrong") }, "wrong-game-thread")
        worker.start()
        worker.join()

        assertFalse(result)
        assertEquals(1L, GameThreadContext.violationCount())
        assertEquals(0L, GameThreadContext.unboundViolationCount())
        assertEquals(1L, GameThreadContext.wrongThreadViolationCount())
    }
}
