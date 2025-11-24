package net.dodian.uber.game.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.AttributeKey;
import net.dodian.utilities.ISAACCipher;

/**
 * Very thin wrapper that simply forwards a {@link ByteBuf} message downstream without modification.
 * This exists to retain the exact pipeline structure expected by the old server code while we
 * fully migrate to Netty-based packet builders.
 */
public class ByteMessageEncoder extends MessageToByteEncoder<ByteMessage> {
    @Override
    protected void encode(ChannelHandlerContext ctx, ByteMessage bm, ByteBuf out) throws Exception {
        
            
            int opcode = bm.getOpcode();
            MessageType type = bm.getType();
            ByteBuf payload = bm.content();
            int length = payload.readableBytes();

                        AttributeKey<ISAACCipher> key = AttributeKey.valueOf("outCipher");
            ISAACCipher cipher = ctx.channel().attr(key).get();
            int encOpcode = cipher == null ? opcode : (opcode + cipher.getNextKey()) & 0xFF;

            out.writeByte(encOpcode);
            out.writeShort(length);
            out.writeBytes(payload, payload.readerIndex(), length);
        }
    }
