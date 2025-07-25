package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;

/**
 * Handles object replacement on the client side (version 2).
 * Emits the legacy opcodes 101 (remove/clear) and 151 (place) with specific
 * byte ordering to match the original implementation.
 */
public class ReplaceObject2 implements OutgoingPacket {

    private final int newObjectId;
    private final int face;
    private final int type;

    public ReplaceObject2(int newObjectId, int face, int type) {
        this.newObjectId = newObjectId;
        this.face = face;
        this.type = type;
    }

    @Override
    public void send(Client client) {
        int config = (type << 2) + (face & 3);

        // First packet (opcode 101) - Clear the object
        ByteMessage remove = ByteMessage.message(101);
        remove.put(config, ValueType.NEGATE);  // writeByteC equivalent
        remove.put(0);                         // writeByte(0)
        client.send(remove);

        // Second packet (opcode 151) - Place new object if needed
        if (newObjectId != -1) {
            ByteMessage place = ByteMessage.message(151);
            place.put(0, ValueType.SUBTRACT);              // writeByteS(0)
            place.putShort(newObjectId, ByteOrder.LITTLE); // writeWordBigEndian (actually little-endian)
            place.put(config, ValueType.SUBTRACT);         // writeByteS(config)
            client.send(place);
        }
    }
}
