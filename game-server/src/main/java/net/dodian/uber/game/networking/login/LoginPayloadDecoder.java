package net.dodian.uber.game.networking.login;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;



/**
 * Reads the first part of a RuneScape 317 login request – the 2-byte header
 * (loginType, size) followed by <code>size</code> bytes of payload. Once the
 * full payload is assembled, it emits a {@link ByteBuf} containing the payload
 * downstream and removes itself from the pipeline. The emitted buffer is
 * retained – downstream handlers MUST release it when done.
 */
public class LoginPayloadDecoder extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(LoginPayloadDecoder.class);

    private enum State { HEADER, BODY }

    private State state = State.HEADER;
    private int loginType;
    private int size;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        switch (state) {
            case HEADER:
                if (in.readableBytes() < 2) {
                    return; // need loginType + size
                }
                loginType = in.readUnsignedByte();
                size = in.readUnsignedByte();
                state = State.BODY;
                // intentionally no break to fall through if payload ready
            case BODY:
                if (in.readableBytes() < size) {
                    return; // wait for payload
                }
                ByteBuf payload = in.readRetainedSlice(size);
                out.add(new LoginPayload(loginType, payload));
                logger.debug("[Netty] Received login payload ({} bytes) from {}", size, ctx.channel().remoteAddress());
                ctx.pipeline().remove(this);
                break;
        }
    }
}
