package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;

/**
 * Sent to create a projectile in the game world.
 * This is used for various in-game projectiles like arrows, spells, etc.
 */
public class Projectile implements OutgoingPacket {

    private final Position casterPosition;
    private final int offsetY;
    private final int offsetX;
    private final int angle;
    private final int speed;
    private final int gfxMoving;
    private final int startHeight;
    private final int endHeight;
    private final int targetIndex;
    private final int begin;
    private final int slope;
    private final int initDistance;

    /**
     * Creates a new Projectile packet.
     * 
     * @param casterPosition The position of the caster (source of the projectile)
     * @param offsetY The Y offset from the caster's position
     * @param offsetX The X offset from the caster's position
     * @param angle The starting angle of the projectile
     * @param speed The speed of the projectile
     * @param gfxMoving The graphic ID of the projectile
     * @param startHeight The starting height of the projectile
     * @param endHeight The ending height of the projectile
     * @param targetIndex The index of the target (NPC or player)
     * @param begin The tick when the projectile is created
     * @param slope The initial slope of the projectile
     * @param initDistance The initial distance from the source
     */
    public Projectile(Position casterPosition, int offsetY, int offsetX, int angle, int speed,
                     int gfxMoving, int startHeight, int endHeight, int targetIndex,
                     int begin, int slope, int initDistance) {
        this.casterPosition = casterPosition;
        this.offsetY = offsetY;
        this.offsetX = offsetX;
        this.angle = angle;
        this.speed = speed;
        this.gfxMoving = gfxMoving;
        this.startHeight = startHeight;
        this.endHeight = endHeight;
        this.targetIndex = targetIndex;
        this.begin = begin;
        this.slope = slope;
        this.initDistance = initDistance;
        System.out.println("Projectile: " + gfxMoving);
    }

    @Override
    public void send(Client client) {
        // Ensure the client has the correct map region loaded
        client.send(new SetMap(new Position(casterPosition.getX() - 3, casterPosition.getY() - 2)));
        
        // Create and send the projectile packet
        ByteMessage message = ByteMessage.message(117, MessageType.FIXED);
        message.put(angle);             // Starting angle of the projectile
        message.put(offsetY);           // Y offset from caster
        message.put(offsetX);           // X offset from caster
        message.putShort(targetIndex);  // Target index (NPC or player)
        message.putShort(gfxMoving);    // Projectile graphic ID
        message.put(startHeight);       // Starting height
        message.put(endHeight);         // Ending height
        message.putShort(begin);        // Creation tick
        message.putShort(speed);        // Speed minus distance
        message.put(slope);             // Initial slope
        message.put(initDistance);      // Initial distance from source
        client.send(message);
    }
}
