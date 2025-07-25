package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;

/**
 * Sent to update a player's equipment in a specific slot.
 */
public class SetEquipment implements OutgoingPacket {

    private final int wearId;
    private final int amount;
    private final int targetSlot;

    /**
     * Creates a new SetEquipment packet.
     * 
     * @param wearId The ID of the item to equip (0 to clear the slot)
     * @param amount The amount/stack size of the item
     * @param targetSlot The equipment slot to update
     */
    public SetEquipment(int wearId, int amount, int targetSlot) {
        this.wearId = wearId;
        this.amount = amount;
        this.targetSlot = targetSlot;
        
        //System.out.println("SetEquipment: Slot=" + targetSlot + ", ItemID=" + wearId + ", Amount=" + amount);
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(34, MessageType.VAR_SHORT);
        message.putShort(1688); // writeWord - interface ID (1688 = equipment interface)
        message.put(targetSlot); // writeByte - equipment slot
        
        // writeWord - item ID (add 1 if not zero, same as original)
        int itemId = wearId > 0 ? wearId + 1 : 0;
        message.putShort(itemId); // Use default byte order (BIG_ENDIAN) for item ID
        
        // Handle large quantities
        if (amount > 254) {
            message.put(255); // writeByte - flag for large amount
            message.putInt(amount, ByteOrder.BIG); // writeDWord - full 32-bit amount in big endian
        } else {
            message.put(amount & 0xFF); // writeByte - small amount (ensure it's a single byte)
        }
        
        client.send(message);
        
        // Additional equipment-related updates
        if (targetSlot == 3) { // 3 is the weapon slot
            client.CheckGear();
            // CombatStyleHandler.setWeaponHandler(client); // Uncomment if needed
            client.requestWeaponAnims();
        }
        
        client.GetBonus(true);
        client.getUpdateFlags().setRequired(net.dodian.uber.game.model.UpdateFlag.APPEARANCE, true);
    }
}
