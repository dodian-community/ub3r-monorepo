package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;

/**
 * Sends animation/graphic at a specific position.
 * This replaces the legacy animation2() method with proper Netty implementation.
 *
 * Packet structure (opcode 4):
 * - Offset byte: Position encoding (localX << 4 | localY)
 * - Animation/Graphic ID: 2 bytes
 * - Height offset: 1 byte
 * - Delay: 2 bytes
 */
public class Animation2 implements OutgoingPacket {

    private final int animationId;
    private final Position position;
    private final int height;
    private final int delay;

    /**
     * Creates a new Animation2 packet.
     *
     * @param animationId The animation/graphic ID to play
     * @param position The position where to display the animation
     * @param height The height offset
     * @param delay The delay before displaying the animation
     */
    public Animation2(int animationId, Position position, int height, int delay) {
        this.animationId = animationId;
        this.position = position;
        this.height = height;
        this.delay = delay;
    }

    @Override
    public void send(Client client) {
        // Calculate the base position for the region (align to 8x8 region)
        int baseX = (position.getX() >> 3) << 3;
        int baseY = (position.getY() >> 3) << 3;

        // Ensure the client has the correct map region loaded
        client.send(new SetMap(new Position(baseX, baseY)));

        // Calculate the offset byte: (localX << 4) | localY
        int localX = position.getX() - baseX;
        int localY = position.getY() - baseY;
        int offsetByte = (localX << 4) | localY;

        // Send the animation packet
        ByteMessage message = ByteMessage.message(4, MessageType.FIXED);
        message.put(offsetByte);      // Position offset from region base
        message.putShort(animationId); // Animation/Graphic ID
        message.put(height);          // Height offset
        message.putShort(delay);      // Delay before display

        client.send(message);
    }
}