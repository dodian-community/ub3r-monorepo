package net.dodian.uber.game.netty.game;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.NetworkConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Receives decoded game packets from Netty and queues them for game-thread
 * processing on the next tick.
 */
public class GamePacketHandler extends SimpleChannelInboundHandler<GamePacket> {

    private static final Logger logger = LoggerFactory.getLogger(GamePacketHandler.class);

    /**
     * Tracks per-client packet rate in a sliding time window to prevent
     * packet floods from a single connection.
     */
    private static final long PACKET_WINDOW_MILLIS = 600L;

    private long packetWindowStartMillis = 0L;
    private int packetsInWindow = 0;
    private boolean windowRateLimitLogged = false;

    private final Client client;

    public GamePacketHandler(Client client) {
        super(false); // manual release of GamePacket payload
        this.client = client;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GamePacket packet) {
        long now = System.currentTimeMillis();

        if (now - packetWindowStartMillis > PACKET_WINDOW_MILLIS) {
            packetWindowStartMillis = now;
            packetsInWindow = 0;
            windowRateLimitLogged = false;
        }

        if (packetsInWindow >= NetworkConstants.PACKET_PROCESS_LIMIT) {
            if (!windowRateLimitLogged) {
                logger.warn("[Netty] Rate limit exceeded for {}: opcode={} size={} (>{} packets in {}ms window)",
                        client.getPlayerName(), packet.getOpcode(), packet.getSize(),
                        NetworkConstants.PACKET_PROCESS_LIMIT, PACKET_WINDOW_MILLIS);
                windowRateLimitLogged = true;
            }
            logger.debug("[Netty] Dropping packet opcode={} size={} from {} due to rate limit ({} per {}ms)",
                    packet.getOpcode(), packet.getSize(), client.getPlayerName(),
                    NetworkConstants.PACKET_PROCESS_LIMIT, PACKET_WINDOW_MILLIS);
            releasePacket(packet);
            return;
        }

        packetsInWindow++;
        logger.trace("[Netty] Queued packet opcode={} size={} for game-thread handling", packet.getOpcode(), packet.getSize());

        if (!client.queueInboundPacket(packet)) {
            logger.warn("[Netty] Inbound queue overflow for {}, closing channel", client.getPlayerName());
            releasePacket(packet);
            ctx.close();
        }
    }

    private void releasePacket(GamePacket packet) {
        if (packet != null && packet.getPayload() != null && packet.getPayload().refCnt() > 0) {
            packet.getPayload().release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("[Netty] GamePacketHandler error for {}", client.getPlayerName(), cause);
        ctx.close();
    }
}
