package net.dodian.uber.game.networking.game;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.AttributeKey;
import net.dodian.utilities.ISAACCipher;
import net.dodian.uber.comm.PacketData;
import net.dodian.uber.game.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class IsaacDecoder extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(IsaacDecoder.class);

    private static final int VARIABLE_BYTE  = -1;
    private static final int VARIABLE_SHORT = -2;

    private static final AttributeKey<ISAACCipher> IN_CIPHER_KEY = AttributeKey.valueOf("inCipher");

    private int packetType = -1;
    private int packetSize = 0;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        ISAACCipher cipher = ctx.channel().attr(IN_CIPHER_KEY).get();
        if (cipher == null) {
            // Should never happen – close channel to be safe.
            logger.warn("[Netty] Missing ISAAC cipher attribute – closing {}", ctx.channel().remoteAddress());
            ctx.close();
            return;
        }

        while (true) {
            if (packetType == -1) {
                if (!in.isReadable()) {
                    return; // Need more data
                }
                int opcode = (in.readUnsignedByte() - cipher.getNextKey()) & 0xFF;
                packetType = opcode;
                if (packetType < 0 || packetType >= Constants.PACKET_SIZES.length) {
                    logger.debug("[Netty] Invalid packet opcode {} from {}", packetType, ctx.channel().remoteAddress());
                    ctx.close();
                    return;
                }
                packetSize = Constants.PACKET_SIZES[packetType];
            }

            if (packetSize == VARIABLE_BYTE) {
                if (in.readableBytes() < 1) {
                    // Wait for size byte
                    return;
                }
                packetSize = in.readUnsignedByte();
            } else if (packetSize == VARIABLE_SHORT) {
                if (in.readableBytes() < 2) {
                    return;
                }
                packetSize = in.readUnsignedShort();
            }

            if (in.readableBytes() < packetSize) {
                // Wait for full packet
                return;
            }

            byte[] data = new byte[packetSize];
            in.readBytes(data);
            out.add(new PacketData(packetType, data, packetSize));

            // Reset for next packet
            packetType = -1;
            packetSize = 0;

            if (!in.isReadable()) {
                return;
            }
        }
    }
}
