package net.dodian.uber.game.systems.pathing

import net.dodian.utilities.Direction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DirectionAndFollowProtocolTest {
    @Test
    fun `317 direction nibble mapping matches protocol`() {
        assertEquals(0, Direction.direction(-1, 1))
        assertEquals(1, Direction.direction(0, 1))
        assertEquals(2, Direction.direction(1, 1))
        assertEquals(3, Direction.direction(-1, 0))
        assertEquals(4, Direction.direction(1, 0))
        assertEquals(5, Direction.direction(-1, -1))
        assertEquals(6, Direction.direction(0, -1))
        assertEquals(7, Direction.direction(1, -1))
    }
}
