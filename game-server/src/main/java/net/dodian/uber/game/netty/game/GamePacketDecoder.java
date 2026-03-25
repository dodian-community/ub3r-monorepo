package net.dodian.uber.game.netty.game;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.AttributeKey;
import net.dodian.utilities.ISAACCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Zero-copy Netty decoder that converts the raw RS317 stream into {@link GamePacket}s.
 */
public class GamePacketDecoder extends ByteToMessageDecoder {

    static {
      //  System.out.println("PACKET_SIZES length=" + Constants.PACKET_SIZES.length + " opcode252=" + Constants.PACKET_SIZES[252]);
    }

    private static final Logger logger = LoggerFactory.getLogger(GamePacketDecoder.class);

    private static final int VARIABLE_BYTE  = -1;
    private static final int VARIABLE_SHORT = -2;

    private static final AttributeKey<ISAACCipher> IN_CIPHER_KEY = AttributeKey.valueOf("inCipher");

    private int opcode = -1;
    private int size   = 0;

    private boolean isNpcTraceOpcode(int opcode) {
        return opcode == 155 || opcode == 17 || opcode == 21 || opcode == 18 || opcode == 72;
    }

    private String preview(ByteBuf payload, int maxBytes) {
        if (payload == null || !payload.isReadable() || maxBytes <= 0) {
            return "[]";
        }
        int count = Math.min(maxBytes, payload.readableBytes());
        int start = payload.readerIndex();
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            int value = payload.getUnsignedByte(start + i);
            if (value < 0x10) {
                builder.append('0');
            }
            builder.append(Integer.toHexString(value).toUpperCase());
        }
        if (payload.readableBytes() > count) {
            builder.append(" ...");
        }
        return builder.append(']').toString();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        ISAACCipher cipher = ctx.channel().attr(IN_CIPHER_KEY).get();
        if (cipher == null) {
            logger.warn("[Netty] Missing ISAAC cipher attribute – closing {}", ctx.channel().remoteAddress());
            ctx.close();
            return;
        }

        while (true) {
            if (opcode == -1) {
                if (!in.isReadable()) {
                    return; // need more data
                }
                int raw = in.readUnsignedByte();
                int dec = (raw - cipher.getNextKey()) & 0xFF;
                opcode = dec;
                //logger.info("Raw byte {} decrypted opcode {}", raw, opcode);
                if (opcode < 0 || opcode > 0xFF) {
                    logger.debug("[Netty] Invalid packet opcode {} from {}", opcode, ctx.channel().remoteAddress());
                    ctx.close();
                    return;
                }
                // Mystic client always sends an ISAAC-encrypted length byte
                // after the opcode, so treat every packet as VARIABLE_BYTE.
                size = VARIABLE_BYTE;
            }

            if (size == VARIABLE_BYTE) {
                if (in.readableBytes() < 1) {
                    return; // need more data for length
                }
                int rawLen = in.readUnsignedByte();
                int decLen = (rawLen - cipher.getNextKey()) & 0xFF; // total packet size (opcode+len+payload)
                if (decLen < 2) {
                    logger.debug("[Netty] Invalid packet length {} for opcode {} from {}", decLen, opcode, ctx.channel().remoteAddress());
                    ctx.close();
                    return;
                }
                size = decLen - 2; // payload size only
            } else if (size == VARIABLE_SHORT) {
                // Not used by the mystic client, but keep for safety
                if (in.readableBytes() < 2) {
                    return;
                }
                int rawLen = in.readUnsignedShort();
                int decLen = (rawLen - cipher.getNextKey()) & 0xFFFF;
                if (decLen < 2) {
                    logger.debug("[Netty] Invalid short packet length {} for opcode {} from {}", decLen, opcode, ctx.channel().remoteAddress());
                    ctx.close();
                    return;
                }
                size = decLen - 2;
            }

            if (in.readableBytes() < size) {
                return; // wait for full payload
            }

            ByteBuf payload = in.readSlice(size).retain();
            if (logger.isDebugEnabled() && isNpcTraceOpcode(opcode)) {
                logger.debug(
                        "[Netty] npc-trace decode opcode={} size={} preview={} remote={}",
                        opcode,
                        size,
                        preview(payload, 4),
                        ctx.channel().remoteAddress()
                );
            }
            out.add(new GamePacket(opcode, size, payload));

            opcode = -1;
            size   = 0;

            if (!in.isReadable()) {
                return;
            }
        }
    }
}
