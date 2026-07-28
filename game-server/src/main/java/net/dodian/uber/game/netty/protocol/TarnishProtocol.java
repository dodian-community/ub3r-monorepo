package net.dodian.uber.game.netty.protocol;

import net.dodian.uber.game.netty.codec.MessageType;

/** Wire-format definitions for the immutable Tarnish client. */
public final class TarnishProtocol {
    public static final int VARIABLE_BYTE = -1;
    public static final int VARIABLE_SHORT = -2;

    /** Client -> server sizes from Tarnish's data/io/message_sizes.json. */
    private static final int[] INBOUND_SIZES = {
        0, 0, 0, 1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0,
        6, 2, 2, 0, 0, 2, 0, 6, 0, 12, -1, 0, 0, 0, 0, 0,
        0, 0, 0, 8, 4, 0, 0, 2, 2, 6, 0, 6, 0, -1, 0, 0,
        0, 0, 0, 0, 0, 12, 0, 0, 0, 8, 8, 12, 8, 8, -2, 0,
        0, 0, 0, 0, 0, 0, 6, 0, 2, 2, 8, 6, 0, -1, 0, 6,
        0, 0, 0, 0, 0, 1, 4, 6, 0, 0, 0, 0, 0, 0, 0, 4,
        0, 0, 5, 0, 0, 13, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 6, 0, 0, 1, 0, 6, 0, 0, 0, -1, -1,
        2, 6, 0, 4, 6, 8, 0, 6, 2, 0, 0, 2, 6, 10, -1, 0,
        0, 6, 0, 0, 0, 7, 7, 0, 1, 2, 0, 2, 6, 0, 0, 0,
        0, 0, 0, 0, 5, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 8, 0, 3, 0, 2, 2, 6, 8, 1, 0, 0,
        12, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0,
        4, 0, 4, 0, 0, 0, 7, 8, 0, 0, 10, 0, 0, 0, 0, 0,
        0, 0, -1, 0, 6, 0, 1, 0, 0, 0, 6, 0, 6, 8, 1, 0,
        0, 4, 0, 0, 0, 0, -1, 0, 5, 4, 0, 0, 6, 6, 0, 5
    };

    /** Server -> client sizes copied from the Tarnish client's SizeConstants. */
    private static final int[] OUTBOUND_SIZES = {
        0,0,0,0,6,5,0,0,4,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,1,0,0,-2,-2,0,
        0,0,0,0,-2,4,3,0,0,0, 0,0,0,0,12,0,0,6,0,0, 10,0,0,-2,0,0,0,0,0,0,
        -2,1,0,0,2,-2,0,0,0,0, 6,3,2,4,2,4,0,0,0,4, 0,-2,0,0,7,2,0,6,0,0,
        0,0,0,0,0,0,0,4,0,1, 0,2,0,8,-1,4,1,0,6,0, 1,2,0,0,-1,0,5,15,0,0,
        0,4,6,6,0,0,-2,6,0,-1, 0,0,0,0,6,0,0,1,-1,2, 0,0,2,0,0,0,0,14,0,0,
        0,4,0,0,0,0,3,0,0,0, 4,0,0,0,2,0,6,0,0,0, 0,3,0,4,5,6,10,6,3,0,
        0,0,1,1,0,2,0,-2,0,-2, 0,-2,0,0,0,0,-1,0,8,0, 4,2,-1,-2,8,-2,4,0,2,0,
        0,0,0,0,8,7,0,-2,2,1, 0,1,0,0,0,0,0,0,0,0, 8,0,0,0,0,0,0,0,0,0,
        2,-2,0,0,0,0,6,0,6,3, 0,0,0,-1,6,0
    };

    public static int inboundSize(int opcode) {
        return size(INBOUND_SIZES, opcode);
    }

    public static int outboundSize(int opcode) {
        return size(OUTBOUND_SIZES, opcode);
    }

    public static void validateOutbound(int opcode, MessageType type, int payloadSize) {
        if (type == MessageType.RAW) return;
        int expected = outboundSize(opcode);
        MessageType expectedType = expected == VARIABLE_BYTE ? MessageType.VAR
                : expected == VARIABLE_SHORT ? MessageType.VAR_SHORT : MessageType.FIXED;
        if (type != expectedType) {
            throw new IllegalArgumentException("Tarnish opcode " + opcode + " requires " + expectedType + " but packet used " + type);
        }
        if (expected >= 0 && payloadSize != expected) {
            throw new IllegalArgumentException("Tarnish opcode " + opcode + " requires " + expected + " payload bytes but got " + payloadSize);
        }
        if (type == MessageType.VAR && payloadSize > 255) {
            throw new IllegalArgumentException("Tarnish variable-byte opcode " + opcode + " exceeds 255 bytes");
        }
        if (payloadSize > 65535) {
            throw new IllegalArgumentException("Tarnish opcode " + opcode + " exceeds 65535 bytes");
        }
    }

    private static int size(int[] sizes, int opcode) {
        if (opcode < 0 || opcode >= sizes.length) {
            throw new IllegalArgumentException("Opcode out of Tarnish range: " + opcode);
        }
        return sizes[opcode];
    }

    private TarnishProtocol() {}
}
