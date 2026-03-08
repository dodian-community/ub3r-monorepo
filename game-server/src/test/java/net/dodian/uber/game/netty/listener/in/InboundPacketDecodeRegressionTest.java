package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InboundPacketDecodeRegressionTest {

    @Test
    void moveItemsPacketLayoutDecodesWithoutWrapper() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(3214);
        buf.writeByte(0x01);
        buf.writeByte(0x80 + 0x34);
        buf.writeByte(0x12);
        buf.writeByte(0x78);
        buf.writeByte(0x56);

        assertEquals(3214, ByteBufReader.readInt(buf));
        assertEquals(1, buf.readUnsignedByte());
        assertEquals(0x1234, ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD));
        assertEquals(0x5678, ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL));
    }

    @Test
    void removeItemPacketLayoutDecodesWithoutWrapper() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(5064);
        buf.writeByte(0x00);
        buf.writeByte(0x80 + 0x0A);
        buf.writeByte(0x01);
        buf.writeByte(0x80 + 0xF4);

        assertEquals(5064, ByteBufReader.readInt(buf));
        assertEquals(10, ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD));
        assertEquals(500, ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD));
    }

    @Test
    void magicOnItemsPacketLayoutDecodesWithoutWrapper() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0x00);
        buf.writeByte(0x05);
        buf.writeByte(0x01);
        buf.writeByte(0x80 + 0x2C);
        buf.writeByte(0x00);
        buf.writeByte(0x7E);
        buf.writeByte(0x04);
        buf.writeByte(0x80 + 0x95);

        assertEquals(5, ByteBufReader.readShortSigned(buf, ByteOrder.BIG, ValueType.NORMAL));
        assertEquals(300, ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD));
        assertEquals(126, ByteBufReader.readShortSigned(buf, ByteOrder.BIG, ValueType.NORMAL));
        assertEquals(1173, ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD));
    }

    @Test
    void sendPrivateMessagePacketLayoutDecodesWithoutWrapper() {
        byte[] text = new byte[] {1, 2, 3, 4};
        ByteBuf buf = Unpooled.buffer();
        buf.writeLong(123456789L);
        buf.writeBytes(text);

        assertEquals(123456789L, ByteBufReader.readLong(buf));
        byte[] decoded = new byte[buf.readableBytes()];
        buf.readBytes(decoded);
        assertArrayEquals(text, decoded);
    }

    @Test
    void npcFirstClickPacketLayoutDecodesWithoutWrapper() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0x2A);
        buf.writeByte(0x00);

        assertEquals(42, ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.NORMAL));
    }

    @Test
    void walkingFirstStepFieldsDecodeWithLegacyRsOrdering() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte((319 * 8 + 10 + 128) & 0xFF);
        buf.writeByte(((319 * 8 + 10) >> 8) & 0xFF);
        buf.writeByte((381 * 8 + 11) & 0xFF);
        buf.writeByte(((381 * 8 + 11) >> 8) & 0xFF);

        assertEquals(319 * 8 + 10, ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.ADD));
        assertEquals(381 * 8 + 11, ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.NORMAL));
    }
}
