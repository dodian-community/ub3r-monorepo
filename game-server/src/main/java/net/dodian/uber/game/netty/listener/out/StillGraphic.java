package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;

/**
 * Sent to display a still graphic at a specific position.
 * This is used for various in-game effects like spell impacts, teleport effects, etc.
 */
public class StillGraphic implements OutgoingPacket {

    private final int id;
    private final Position position;
    private final int height;
    private final int time;
    private final boolean showAll;

    /**
     * Creates a new StillGraphic packet.
     * 
     * @param id The graphic ID to display
     * @param position The position where to display the graphic
     * @param height The height offset of the graphic
     * @param time The time before the graphic is cast (in game ticks)
     * @param showAll Whether to show the graphic to all players in the area
     */
    public StillGraphic(int id, Position position, int height, int time, boolean showAll) {
        this.id = id;
        this.position = position;
        this.height = height;
        this.time = time;
        this.showAll = showAll;
        System.out.println("Still gfx: " + id);
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

        // Send the graphic packet
        ByteMessage message = ByteMessage.message(4, MessageType.FIXED);
        message.put(offsetByte);  // Position offset from region base
        message.putShort(id);     // Graphic ID
        message.put(height);      // Height offset
        message.putShort(time);   // Time before casting the graphic
        client.send(message);
    }
}
