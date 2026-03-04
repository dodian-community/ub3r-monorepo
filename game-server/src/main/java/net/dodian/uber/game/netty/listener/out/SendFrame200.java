package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.dodian.utilities.DotEnvKt.getClientPacketTraceEnabled;

/**
 * Sends a frame 200 packet with main and sub frame parameters.
 * This replaces the legacy sendFrame200() method with proper Netty implementation.
 * 
 * Packet structure:
 * - Opcode: 200 (fixed size)
 * - Main Frame: 2 bytes
 * - Sub Frame: 2 bytes
 */
public class SendFrame200 implements OutgoingPacket {
    private static final Logger logger = LoggerFactory.getLogger(SendFrame200.class);

    private final int mainFrame;
    private final int subFrame;

    /**
     * Creates a new SendFrame200 packet.
     * 
     * @param mainFrame The main frame parameter
     * @param subFrame The sub frame parameter
     */
    public SendFrame200(int mainFrame, int subFrame) {
        this.mainFrame = mainFrame;
        this.subFrame = subFrame;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(200, MessageType.FIXED);
        
        // Legacy sendFrame200: writeWordBigEndian for both values
        message.putShort(mainFrame, ByteOrder.BIG);
        message.putShort(subFrame, ByteOrder.BIG);
        
        client.send(message);
        if (getClientPacketTraceEnabled() && logger.isTraceEnabled()) {
            logger.trace("SendFrame200 mainFrame={} subFrame={} player={}", mainFrame, subFrame, client.getPlayerName());
        }
    }
}
