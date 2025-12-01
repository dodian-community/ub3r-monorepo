package net.dodian.uber.game.netty.game;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.NetworkConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Temporary game packet handler that simply logs receipt of packets.
 * The real opcode dispatching will be implemented once legacy handlers are
 * migrated. Retains a reference to the {@link Client} for future use.
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
        super(false); // manual release of GamePacket's ByteBuf happens here
        this.client = client;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GamePacket packet) {
        try {
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
                return;
            }

            packetsInWindow++;

            int opcode = packet.getOpcode();
            logger.trace("[Netty] Received packet opcode={} size={}", opcode, packet.getSize());

            net.dodian.uber.game.netty.listener.PacketListener listener =
                    net.dodian.uber.game.netty.listener.PacketListenerManager.get(opcode);

            if (listener == null) {
                logger.debug("[Netty] No listener found for opcode={}, checking legacy bridge", opcode);
                // Fallback to legacy bridge without registration lookup
                // listener = net.dodian.uber.game.netty.listener.in.LegacyBridgeListenerHolder.INSTANCE;
            }

            if (listener != null) {
                logger.trace("[Netty] Dispatching opcode={} to {}", opcode, listener.getClass().getSimpleName());
                listener.handle(client, packet);
            } else {
                logger.debug("[Netty] Unhandled opcode={} size={} (no legacy handler either)", opcode, packet.getSize());
            }
        } catch (Exception ex) {
            logger.warn("[Netty] Packet listener error opcode {} for {}", packet.getOpcode(), client.getPlayerName(), ex);
        } finally {
            if (packet.getPayload().refCnt() > 0) {
                packet.getPayload().release();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("[Netty] GamePacketHandler error for {}", client.getPlayerName(), cause);
        ctx.close();
    }
}
