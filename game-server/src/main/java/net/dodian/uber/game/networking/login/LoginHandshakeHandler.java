package net.dodian.uber.game.networking.login;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;


/**
 * Handles the very first 2-byte RuneScape handshake (opcode 14) and responds
 * with the server seed. After a successful handshake it swaps itself for
 * {@link LoginProcessorHandler} so that the rest of the login payload can be
 * decoded.
 */
public class LoginHandshakeHandler extends SimpleChannelInboundHandler<ByteBuf> {

    public static final AttributeKey<Long> SERVER_SEED_KEY = AttributeKey.valueOf("serverSeed");

    private static final Logger logger = LoggerFactory.getLogger(LoginHandshakeHandler.class);

    private final PlayerHandler playerHandler;
    private boolean handshakeDone = false;
    private byte opcode = -1;

    public LoginHandshakeHandler(PlayerHandler playerHandler) {
        this.playerHandler = playerHandler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf in) {
        if (handshakeDone) {
            // Already finished, forward any bytes
            ctx.fireChannelRead(in.retain());
            return;
        }

        if (in.readableBytes() < 2) {
            return; // wait for more data
        }

        opcode = in.readByte();
        if (opcode != 14) {
            logger.warn("[Netty] Unexpected login opcode {} from {} – closing", opcode & 0xFF, ctx.channel().remoteAddress());
            ctx.close();
            return;
        }

        in.readByte(); // nameHash (ignored)

        long serverSeed = ThreadLocalRandom.current().nextLong();

        ByteBuf response = ctx.alloc().buffer(17);
        for (int i = 0; i < 8; i++) {
            response.writeByte(10); // historic 0x0A padding bytes expected by client
        }
        response.writeByte(0);            // login stage indicator
        response.writeLong(serverSeed);   // 8-byte server seed
        ctx.writeAndFlush(response);

        ctx.channel().attr(SERVER_SEED_KEY).set(serverSeed);
        handshakeDone = true;

        logger.debug("[Netty] Handshake complete for {} (seed {})", ctx.channel().remoteAddress(), serverSeed);

        if (in.isReadable()) {
            // Forward any remaining bytes (unlikely but safe)
            ctx.fireChannelRead(in.readRetainedSlice(in.readableBytes()));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("[Netty] Handshake error for {}: {}", ctx.channel().remoteAddress(), cause.getMessage());
        ctx.close();
    }
}
