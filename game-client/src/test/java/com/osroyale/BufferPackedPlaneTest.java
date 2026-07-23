package com.osroyale;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BufferPackedPlaneTest {
    @Test
    public void readsAnUnalignedPackedPlaneAndRoundsOnlyAtTheEnd() {
        Buffer buffer = new Buffer(new byte[]{(byte) 0xB4, (byte) 0x80, 0x55});

        buffer.initBitAccess();
        assertEquals(0b101, buffer.readBits(3));
        assertEquals(0b101001, buffer.readBits(6));
        buffer.finishBitAccess();

        assertEquals(2, buffer.position);
        assertEquals(0x55, buffer.readUnsignedByte());
    }
}
