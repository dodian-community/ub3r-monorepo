package net.dodian.uber.game.networking.game;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.AttributeKey;
import net.dodian.utilities.ISAACCipher;
import net.dodian.uber.comm.PacketData;


public class IsaacEncoder extends MessageToByteEncoder<PacketData> {

    /** Attribute containing the outgoing ISAAC cipher set during login. */
    public static final AttributeKey<ISAACCipher> OUT_CIPHER_KEY = AttributeKey.valueOf("outCipher");

    @Override
    protected void encode(ChannelHandlerContext ctx, PacketData msg, ByteBuf out) {
        ISAACCipher cipher = ctx.channel().attr(OUT_CIPHER_KEY).get();
        if (cipher == null) {
            // Fallback – write raw packet without ISAAC, should never happen.
            writeRaw(msg, out);
            return;
        }

        int encodedOpcode = (msg.getId() + cipher.getNextKey()) & 0xFF;
        out.writeByte(encodedOpcode);
        writeLength(out, msg.getLength());
        out.writeBytes(msg.getData(), 0, msg.getLength());
    }

    private static void writeRaw(PacketData msg, ByteBuf out) {
        out.writeByte(msg.getId());
        writeLength(out, msg.getLength());
        out.writeBytes(msg.getData(), 0, msg.getLength());
    }

    private static void writeLength(ByteBuf out, int length) {
        if (length == 0) {
            return; // Fixed-size packet of zero length – nothing to write.
        }
        if (length < 256) {
            out.writeByte(length);
        } else {
            out.writeShort(length);
        }
    }
}
