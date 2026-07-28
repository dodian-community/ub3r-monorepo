package net.dodian.uber.game.netty.listener.`in`

import io.netty.buffer.Unpooled
import net.dodian.uber.game.netty.game.decode.TarnishPackets
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TarnishPlayerMenuPacketTest {
    @Test
    fun `active trade follow and duel packets decode little endian player slots`() {
        for (opcode in listOf(39, 139, 153)) {
            val payload = Unpooled.buffer(2).writeShortLE(513)
            try {
                assertEquals(513, TarnishPackets.PlayerMenuClick.decode(opcode, payload)?.playerIndex())
            } finally {
                payload.release()
            }
        }
    }

    @Test
    fun `legacy primary player action remains big endian`() {
        val payload = Unpooled.buffer(2).writeShort(513)
        try {
            assertEquals(513, TarnishPackets.PlayerMenuClick.decode(128, payload)?.playerIndex())
        } finally {
            payload.release()
        }
    }
}
