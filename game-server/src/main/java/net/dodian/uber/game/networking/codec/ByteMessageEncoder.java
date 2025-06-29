package net.dodian.uber.game.networking.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * Encodes {@link ByteMessage} instances into a {@link ByteBuf} containing the
 * RuneScape packet header (opcode + optional length) followed by the payload.
 * <p>
 * The resulting buffer is passed down the pipeline (e.g., to {@code GamePacketEncoder})
 * for optional encryption or direct write.
 */
@Sharable
public class ByteMessageEncoder extends MessageToMessageEncoder<ByteMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteMessage msg, List<Object> out) throws Exception {
        MessageType type = msg.getType();

        // RAW means the wrapped buffer is already complete.
        if (type == MessageType.RAW) {
            out.add(msg.getBuffer().retain());
            return;
        }

        int opcode = msg.getOpcode();
        ByteBuf payload = msg.getBuffer();
        int length = payload.readableBytes();

        int headerSize;
        switch (type) {
            case FIXED:
                headerSize = 1; // opcode only
                break;
            case VAR:
                headerSize = 2; // opcode + byte length
                break;
            case VAR_SHORT:
                headerSize = 3; // opcode + short length
                break;
            default:
                headerSize = 1;
        }

        ByteBuf outBuf = ByteBufAllocator.DEFAULT.buffer(headerSize + length);
        outBuf.writeByte(opcode);
        if (type == MessageType.VAR) {
            outBuf.writeByte(length);
        } else if (type == MessageType.VAR_SHORT) {
            outBuf.writeShort(length);
        }
        outBuf.writeBytes(payload, payload.readerIndex(), length);

        out.add(outBuf);
    }
}
