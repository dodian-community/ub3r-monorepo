package net.dodian.uber.game.netty.login;

import net.dodian.uber.game.model.entity.player.PlayerHandler;

/**
 * Thin wrapper that lives in the new clean package but simply reuses the logic
 * from the legacy implementation while we incrementally migrate.
 */
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ThreadLocalRandom;

public class LoginHandshakeHandler extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(LoginHandshakeHandler.class);
    public static final AttributeKey<Long> SERVER_SEED_KEY = AttributeKey.valueOf("serverSeed");

    public LoginHandshakeHandler(PlayerHandler playerHandler) {
        // The playerHandler is currently unused in the handshake but may be needed later.
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, java.util.List<Object> out) {
        if (in.readableBytes() < 2) {
            return; // Not enough bytes yet, ByteToMessageDecoder will buffer for us.
        }

        byte opcode = in.readByte();
        if (opcode != 14) { // 14 is the RS2 login opcode
            logger.warn("[Netty] Unexpected login opcode {} from {} – closing", opcode & 0xFF, ctx.channel().remoteAddress());
            ctx.close();
            return;
        }

        in.readByte(); // nameHash (unused by server, but must be read)

        long serverSeed = ThreadLocalRandom.current().nextLong();

        // Build 17-byte handshake response: 8 bytes of 0, 1 byte stage, 8-byte seed
        ByteBuf response = ctx.alloc().buffer(17);
        for (int i = 0; i < 8; i++) {
            response.writeByte(0);
        }
        response.writeByte(0); // Response code (0 = proceed)
        response.writeLong(serverSeed);
        ctx.writeAndFlush(response);

        // Set the seed for the payload decoder and then remove this handler.
        ctx.channel().attr(SERVER_SEED_KEY).set(serverSeed);
        logger.debug("[Netty] Handshake complete for {} (seed {})", ctx.channel().remoteAddress(), serverSeed);

        // The handshake is done, so this handler is no longer needed.
        ctx.pipeline().remove(this);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("[Netty] Handshake error for {}: {}", ctx.channel().remoteAddress(), cause.getMessage());
        ctx.close();
    }
}
