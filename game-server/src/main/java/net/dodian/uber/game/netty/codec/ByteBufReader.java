package net.dodian.uber.game.netty.codec;

import io.netty.buffer.ByteBuf;

/**
 * Static helpers for reading RuneScape-protocol values from a {@link ByteBuf}
 * using {@link ByteOrder} and {@link ValueType} semantics.
 */
public final class ByteBufReader {

    private ByteBufReader() {}

    /**
     * Reads a signed short (16-bit) according to the given order and value type.
     * Supports BIG and LITTLE orders used by Dodian.
     */
    public static int readShort(ByteBuf buf, ValueType vt, ByteOrder order) {
        int b1 = buf.readUnsignedByte();
        int b2 = buf.readUnsignedByte();

        int high, low;
        switch (order) {
            case BIG: // RuneScape variant: low byte is first in stream
                low  = applyTransform(b1, vt);
                high = b2;
                break;
            case LITTLE:
                low  = applyTransform(b1, vt);
                high = b2;
                break;
            default:
                throw new UnsupportedOperationException("ByteOrder " + order + " not yet supported for readShort");
        }
        int value = (high << 8) | low;
        if (value > 32767) value -= 65536;
        return value;
    }

    /** Reads a signed byte with a transform. */
    public static int readByte(ByteBuf buf, ValueType vt) {
        int val = buf.readByte(); // signed
        switch (vt) {
            case ADD:
                return val - 128;
            case NEGATE:
                return -val;
            case SUBTRACT:
                return 128 - val;
            case NORMAL:
            default:
                return val;
        }
    }

    private static int applyTransform(int value, ValueType vt) {
        switch (vt) {
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
