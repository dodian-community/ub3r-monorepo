package net.dodian.uber.game.engine.sync

import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.engine.sync.cache.SharedPlayerBlockCache
import net.dodian.uber.game.engine.sync.protocol.PackedUpdateBlock
import net.dodian.uber.game.engine.sync.util.SlotBitSet
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerUpdating
import net.dodian.uber.game.netty.codec.ByteMessage
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PackedUpdateProtocolTest {
    @Test
    fun `bit slices concatenate without per-slice padding`() {
        val out = ByteMessage.wrap(Unpooled.buffer(8))
        out.startBitAccess()
        out.putBits(3, 0b101)
        out.putBitSlice(byteArrayOf(0xD2.toByte()), 1, 6)
        out.endBitAccess()

        assertArrayEquals(byteArrayOf(0xB4.toByte(), 0x80.toByte()), out.toByteArray())
        out.releaseAll()
    }

    @Test
    fun `player hit is twenty bits and two hits share five bytes`() {
        val player = Client(EmbeddedChannel(), 1).apply {
            playerName = "packed-hit"
            moveTo(3200, 3200, 0)
            dealDamage(null, 10, Entity.hitType.STANDARD)
            updateFlags.setRequired(UpdateFlag.HIT, true)
        }
        val block = requireNotNull(PlayerUpdating.getInstance().buildSharedBlock(player, "UPDATE_LOCAL"))

        assertEquals(0x20, block.mask)
        assertEquals(20, block.fixedBitCount)
        assertArrayEquals(byteArrayOf(0x0A, 0x20, 0x00), block.fixedBytes)

        val plane = ByteMessage.wrap(Unpooled.buffer(8))
        plane.startBitAccess()
        repeat(2) { plane.putBitSlice(block.fixedBytes, 0, block.fixedBitCount) }
        plane.endBitAccess()
        assertArrayEquals(
            byteArrayOf(0x0A, 0x20, 0x00, 0xA2.toByte(), 0x00),
            plane.toByteArray(),
        )
        plane.releaseAll()
        player.saveNeeded = false
        player.destruct()
    }

    @Test
    fun `slot cache rejects another player occupying the same slot`() {
        val cache = SharedPlayerBlockCache()
        val first = Client(EmbeddedChannel(), 1)
        val replacement = Client(EmbeddedChannel(), 1)
        val block = PackedUpdateBlock(0x20, byteArrayOf(0), 1, byteArrayOf())

        cache.put(first, "UPDATE_LOCAL", block, 7)

        assertSame(block, cache.get(first, "UPDATE_LOCAL", 7))
        assertNull(cache.get(first, "UPDATE_LOCAL", 8))
        assertNull(cache.get(replacement, "UPDATE_LOCAL", 7))
        first.saveNeeded = false
        replacement.saveNeeded = false
        first.destruct()
        replacement.destruct()
    }

    @Test
    fun `slot bitset handles word boundaries and touched-word clearing`() {
        val slots = SlotBitSet(16_384)
        assertTrue(slots.add(63))
        assertTrue(slots.add(64))
        assertTrue(slots.add(16_383))
        assertFalse(slots.add(64))
        assertTrue(slots.contains(63))
        assertTrue(slots.contains(64))
        assertTrue(slots.contains(16_383))

        slots.clear()

        assertFalse(slots.contains(63))
        assertFalse(slots.contains(64))
        assertFalse(slots.contains(16_383))
    }
}
