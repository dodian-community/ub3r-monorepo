package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.listener.OutgoingPacket;

/**
 * Clears an item container interface by sending empty slots.
 * This replaces legacy methods that manually clear item containers.
 * 
 * Packet structure (must match Mystic's SEND_UPDATE_ITEMS handler):
 * - Opcode: 53 (variable size word)
 * - Interface ID: 4 bytes (int)
 * - Item count: 2 bytes (number of slots to clear)
 * - For each slot:
 *   - Amount: 4 bytes (0)
 *   - No item ID is written when amount is 0
 */
public class ClearItemContainer implements OutgoingPacket {

    private final int interfaceId;
    private final int slotCount;

    /**
     * Creates a new ClearItemContainer packet.
     * 
     * @param interfaceId The interface ID to clear
     * @param slotCount The number of slots to clear (typically 28)
     */
    public ClearItemContainer(int interfaceId, int slotCount) {
        this.interfaceId = interfaceId;
        this.slotCount = slotCount;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);

        message.putInt(interfaceId);
        message.putShort(slotCount);

        for (int i = 0; i < slotCount; i++) {
            message.putInt(0);
        }

        ItemContainerTrace.log(client, "ClearItemContainer", interfaceId, slotCount, "all-zero");
        client.send(message);
    }
}
