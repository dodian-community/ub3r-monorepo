package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.netty.codec.ByteMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlayerCachedUpdateBlockLifecycleTest {

    @Test
    public void cacheRetainsAndInvalidateReleasesReference() {
        Client player = new Client(null, 1);
        ByteMessage cached = ByteMessage.raw(4);
        cached.put(0x11);
        cached.put(0x22);

        assertEquals(1, cached.getBuffer().refCnt());

        player.cacheUpdateBlock(cached);
        assertTrue(player.isCachedUpdateBlockValid());
        assertEquals(2, cached.getBuffer().refCnt());

        // Drop local reference, player should still own one retained ref.
        cached.release();
        assertEquals(1, cached.getBuffer().refCnt());

        player.invalidateCachedUpdateBlock();
        assertFalse(player.isCachedUpdateBlockValid());
        assertEquals(0, cached.getBuffer().refCnt());
    }

    @Test
    public void writeCachedBlockAppendsExpectedBytes() {
        Client player = new Client(null, 1);
        ByteMessage cached = ByteMessage.raw(4);
        cached.put(0x33);
        cached.put(0x44);
        player.cacheUpdateBlock(cached);
        cached.release();

        ByteMessage destination = ByteMessage.raw(8);
        try {
            assertTrue(player.isCachedUpdateBlockValid());
            player.writeCachedUpdateBlock(destination);
            assertEquals(2, destination.getBuffer().writerIndex());
            assertEquals(0x33, destination.getBuffer().getUnsignedByte(0));
            assertEquals(0x44, destination.getBuffer().getUnsignedByte(1));
        } finally {
            destination.releaseAll();
            player.invalidateCachedUpdateBlock();
        }
    }
}
