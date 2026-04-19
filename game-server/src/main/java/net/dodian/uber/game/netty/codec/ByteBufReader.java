package net.dodian.uber.game.netty.codec;

import io.netty.buffer.ByteBuf;

/**
 * Static helpers for reading RuneScape-protocol values from a {@link ByteBuf}
 * using {@link ByteOrder} and {@link ValueType} semantics.
 */
public final class ByteBufReader {

    private ByteBufReader() {}

    public static int readShort(ByteBuf buf, ValueType valueType, ByteOrder order) {
        return readShortSigned(buf, order, valueType);
    }

    public static int readShortSigned(ByteBuf buf, ByteOrder order, ValueType valueType) {
        int value = readShortInternal(buf, order, valueType);
        return value > 32767 ? value - 65536 : value;
    }

    public static int readShortUnsigned(ByteBuf buf, ByteOrder order, ValueType valueType) {
        return readShortInternal(buf, order, valueType) & 0xFFFF;
    }

    public static int readByte(ByteBuf buf, ValueType valueType) {
        return readSignedByte(buf, valueType);
    }

    public static int readSignedByte(ByteBuf buf, ValueType valueType) {
        int value = buf.readUnsignedByte();
        int transformed = applyTransform(value, valueType);
        return transformed > 127 ? transformed - 256 : transformed;
    }

    public static int readUnsignedByte(ByteBuf buf, ValueType valueType) {
        return applyTransform(buf.readUnsignedByte(), valueType);
    }

    public static int readInt(ByteBuf buf) {
        return buf.readInt();
    }

    public static long readLong(ByteBuf buf) {
        return buf.readLong();
    }

    public static String readTerminatedString(ByteBuf buf) {
        return readTerminatedString(buf, buf.readableBytes());
    }

    public static String readTerminatedString(ByteBuf buf, int maxLength) {
        int safeLength = Math.max(0, Math.min(maxLength, buf.readableBytes()));
        StringBuilder builder = new StringBuilder(safeLength);
        for (int i = 0; i < safeLength && buf.isReadable(); i++) {
            int value = buf.readUnsignedByte();
            if (value == 0 || value == 10) {
                break;
            }
            builder.append((char) value);
        }
        return builder.toString();
    }

    private static int readShortInternal(ByteBuf buf, ByteOrder order, ValueType valueType) {
        int value = 0;
        switch (order) {
            case BIG:
                value |= readUnsignedByte(buf, ValueType.NORMAL) << 8;
                value |= readUnsignedByte(buf, valueType);
                break;
            case LITTLE:
                value |= readUnsignedByte(buf, valueType);
                value |= readUnsignedByte(buf, ValueType.NORMAL) << 8;
                break;
            case MIDDLE:
                throw new UnsupportedOperationException("Middle-endian short is impossible");
            case INVERSE_MIDDLE:
                throw new UnsupportedOperationException("Inverse-middle-endian short is impossible");
            default:
                throw new UnsupportedOperationException("Unsupported ByteOrder " + order);
        }
        return value;
    }

    private static int applyTransform(int value, ValueType valueType) {
        switch (valueType) {
            case ADD:
                return (value - 128) & 0xFF;
            case NEGATE:
                return (-value) & 0xFF;
            case SUBTRACT:
                return (128 - value) & 0xFF;
            case NORMAL:
            default:
                return value & 0xFF;
        }
    }
}
