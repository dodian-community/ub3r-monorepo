package net.dodian.uber.game.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteBufReaderTest {

    @Test
    void readsSignedShortBigEndianWithAddTransform() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0x12);
        buf.writeByte(0x80 + 0x34);

        assertEquals(0x1234, ByteBufReader.readShortSigned(buf, ByteOrder.BIG, ValueType.ADD));
    }

    @Test
    void readsUnsignedShortLittleEndianWithAddTransform() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0x80 + 0x34);
        buf.writeByte(0x12);

        assertEquals(0x1234, ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD));
    }

    @Test
    void readsSignedAndUnsignedByteTransforms() {
        ByteBuf negateBuf = Unpooled.buffer();
        negateBuf.writeByte(0xFF);
        assertEquals(1, ByteBufReader.readUnsignedByte(negateBuf, ValueType.NEGATE));

        ByteBuf subtractBuf = Unpooled.buffer();
        subtractBuf.writeByte(0x7F);
        assertEquals(1, ByteBufReader.readSignedByte(subtractBuf, ValueType.SUBTRACT));
    }

    @Test
    void readsTerminatedStringUntilNewlineOrNull() {
        ByteBuf newlineBuf = Unpooled.wrappedBuffer(new byte[] {'h', 'i', 10, 'x'});
        assertEquals("hi", ByteBufReader.readTerminatedString(newlineBuf, 10));

        ByteBuf nullBuf = Unpooled.wrappedBuffer(new byte[] {'o', 'k', 0, 'x'});
        assertEquals("ok", ByteBufReader.readTerminatedString(nullBuf, 10));
    }
}
