package net.dodian.uber.game.networking.game;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.AttributeKey;
import net.dodian.utilities.ISAACCipher;
import net.dodian.uber.game.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Zero-copy Netty decoder that converts the raw RS317 stream into {@link GamePacket}s.
 * Logic mirrors {@link net.dodian.uber.game.networking.game.IsaacDecoder} but keeps the
 * payload inside a {@link ByteBuf} slice instead of copying into a byte[].
 */
public class GamePacketDecoder extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(GamePacketDecoder.class);

    private static final int VARIABLE_BYTE  = -1;
    private static final int VARIABLE_SHORT = -2;

    private static final AttributeKey<ISAACCipher> IN_CIPHER_KEY = AttributeKey.valueOf("inCipher");

    private int opcode = -1;
    private int size   = 0;

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
                opcode = (in.readUnsignedByte() - cipher.getNextKey()) & 0xFF;
                if (opcode < 0 || opcode >= Constants.PACKET_SIZES.length) {
                    logger.debug("[Netty] Invalid packet opcode {} from {}", opcode, ctx.channel().remoteAddress());
                    ctx.close();
                    return;
                }
                size = Constants.PACKET_SIZES[opcode];
            }

            if (size == VARIABLE_BYTE) {
                if (in.readableBytes() < 1) {
                    return;
                }
                size = in.readUnsignedByte();
            } else if (size == VARIABLE_SHORT) {
                if (in.readableBytes() < 2) {
                    return;
                }
                size = in.readUnsignedShort();
            }

            if (in.readableBytes() < size) {
                return; // wait for full payload
            }

            // Slice without copy
            ByteBuf payload = in.readSlice(size).retain();
            out.add(new GamePacket(opcode, size, payload));

            // reset state for next packet
            opcode = -1;
            size   = 0;

            if (!in.isReadable()) {
                return;
            }
        }
    }
}
