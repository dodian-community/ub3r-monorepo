package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.*;

/**
 * Handles object replacement on the client side.
 * Emits the legacy opcodes 101 (remove/clear) and 151 (place) replicating
 * Stream.writeByteC / writeByteS semantics.
 */
public class ReplaceObject implements OutgoingPacket {

    private final int newObjectId;
    private final int face;
    private final int type;

    public ReplaceObject(int newObjectId, int face, int type) {
        this.newObjectId = newObjectId;
        this.face = face;
        this.type = type;
    }

    @Override
    public void send(Client client) {
        int config = (type << 2) + (face & 3);

        // First packet (opcode 101)
        ByteMessage remove = ByteMessage.message(101);
        remove.put(config, ValueType.NEGATE); // equivalent to writeByteC
        remove.put(0);                        // writeByte(0)
        client.send(remove);

        // Second packet (opcode 151) if placing a new object
        if (newObjectId != -1) {
            ByteMessage place = ByteMessage.message(151);
            place.put(0, ValueType.ADD);                      // offset byte with ADD transformation
            place.putShort(newObjectId, ByteOrder.LITTLE);    // object ID (little-endian)
            place.put(config, ValueType.SUBTRACT);            // objectTypeFace with SUBTRACT transformation
            client.send(place);
        }
    }
}
