package net.dodian.utilities

import net.dodian.uber.game.engine.util.Misc as EngineMisc
import net.dodian.uber.game.engine.util.Utils as EngineUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UtilitiesRefactorParityTest {
    @Test
    fun `md5 output remains stable`() {
        assertEquals("5d41402abc4b2a76b9719d911017c592", MD5("hello").compute())
    }

    @Test
    fun `range values stay in bounds`() {
        val range = Range(3, 7)
        repeat(200) {
            val value = range.getValue()
            assertTrue(value in 3..7)
        }
    }

    @Test
    fun `isaac cipher remains deterministic for equal seeds`() {
        val seed = intArrayOf(1, 2, 3, 4)
        val first = ISAACCipher(seed)
        val second = ISAACCipher(seed)
        repeat(128) {
            assertEquals(first.getNextKey(), second.getNextKey())
        }
    }

    @Test
    fun `player name conversion roundtrip remains stable`() {
        val encoded = Names.playerNameToLong("alex")
        assertEquals("alex", Names.longToPlayerName(encoded))
    }

    @Test
    fun `direction lookup and protocol deltas remain stable`() {
        assertEquals(4, Direction.direction(3200, 3200, 3201, 3200))
        assertEquals(1, Direction.directionDeltaX[1].toInt())
        assertEquals(1, Direction.directionDeltaY[1].toInt())
        assertEquals(4, Direction.xlateDirectionToClient[2].toInt())
    }

    @Test
    fun `legacy misc and utils delegate to canonical engine util facades`() {
        assertEquals(Misc.format(123456), EngineMisc.format(123456))
        assertEquals(Utils.playerNameToLong("alex"), EngineUtils.playerNameToLong("alex"))
        assertEquals(Utils.capitalize("dodian"), EngineUtils.capitalize("dodian"))
        assertSame(Utils.directionDeltaX, EngineUtils.directionDeltaX)
        assertSame(Utils.directionDeltaY, EngineUtils.directionDeltaY)
        assertSame(Utils.xlateDirectionToClient, EngineUtils.xlateDirectionToClient)
    }
}
