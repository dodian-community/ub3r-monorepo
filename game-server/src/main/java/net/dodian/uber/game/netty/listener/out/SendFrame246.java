package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;

/**
 * Sends a frame 246 packet with main frame and two sub frame parameters.
 * This replaces the legacy sendFrame246() method with proper Netty implementation.
 * 
 * Packet structure:
 * - Opcode: 246 (fixed size)
 * - Main Frame: 2 bytes (writeWordBigEndian - big-endian byte order)
 * - Sub Frame: 2 bytes 
 * - Sub Frame 2: 2 bytes
 */
public class SendFrame246 implements OutgoingPacket {

    private final int mainFrame;
    private final int subFrame;
    private final int subFrame2;

    /**
     * Creates a new SendFrame246 packet.
     * 
     * @param mainFrame The main frame parameter
     * @param subFrame The first sub frame parameter
     * @param subFrame2 The second sub frame parameter
     */
    public SendFrame246(int mainFrame, int subFrame, int subFrame2) {
        this.mainFrame = mainFrame;
        this.subFrame = subFrame;
        this.subFrame2 = subFrame2;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(246, MessageType.FIXED);
        
        message.putShort(mainFrame, ByteOrder.LITTLE);
        message.putShort(subFrame);
        message.putShort(subFrame2);
        
        client.send(message);
        System.out.println("Send frame 246: " + mainFrame + ", " + subFrame + ", " + subFrame2);
    }
}