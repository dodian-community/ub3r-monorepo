package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.party.Balloons;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty implementation of legacy Bank5 packet (opcode 117).
 * Moves exactly 5 units of an item between inventory/bank/etc.
 */
public class Bank5Listener implements PacketListener {

    static { PacketListenerManager.register(117, new Bank5Listener()); }

    private static final Logger logger = LoggerFactory.getLogger(Bank5Listener.class);

    private static int readSignedWordBigEndianA(ByteBuf buf) {
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        int high = buf.readUnsignedByte();
        int value = (high << 8) | low;
        if (value > 32767) value -= 65536;
        return value;
    }

    private static int readSignedWordBigEndian(ByteBuf buf) {
        int low = buf.readUnsignedByte();
        int high = buf.readUnsignedByte();
        int value = (high << 8) | low;
        if (value > 32767) value -= 65536;
        return value;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();

        // Mystic sends (ItemContainerOption2):
        // int interfaceId (putInt)
        // short nodeId (writeSignedBigEndian)
        // short slot   (writeUnsignedWordBigEndian)
        int interfaceId = buf.readInt();
        int removeId = readSignedWordBigEndianA(buf);
        int removeSlot = readSignedWordBigEndian(buf) & 0xFFFF;

        if (client.playerRights == 2) {
            client.println_debug("Bank5: interfaceId=" + interfaceId + " itemId=" + removeId + " slot=" + removeSlot);
        }

        final int amount = 5;

        switch (interfaceId) {
            case 3322: // bag ↔ trade / duel offer
                if (client.inDuel && client.canOffer) {
                    client.stakeItem(removeId, removeSlot, amount);
                } else if (client.inTrade && client.canOffer) {
                    client.tradeItem(removeId, removeSlot, amount);
                }
                break;
            case 6669: // duel window → inv
                if (client.inDuel && client.canOffer) {
                    client.fromDuel(removeId, removeSlot, amount);
                }
                break;
            case 5064: // inventory → bank / party chest
                if (client.IsBanking) {
                    client.bankItem(removeId, removeSlot, amount);
                } else if (client.isPartyInterface) {
                    Balloons.offerItems(client, removeId, amount, removeSlot);
                }
                client.checkItemUpdate();
                break;
            case 5382: // bank → inventory (legacy)
                client.fromBank(removeId, removeSlot, amount);
                break;
            case 2274: // party chest → inventory
                Balloons.removeOfferItems(client, removeId, amount, removeSlot);
                break;
            case 3415: // trade window → inv
                if (client.inTrade && client.canOffer) {
                    client.fromTrade(removeId, removeSlot, amount);
                }
                break;
            default:
                // Mystic bank tab containers: 50300-50310
                if (interfaceId >= 50300 && interfaceId <= 50310) {
                    client.fromBank(removeId, removeSlot, amount);
                } else {
                    handleSpecialInterfaces(client, interfaceId, removeId, removeSlot);
                }
                break;
        }
    }

    private void handleSpecialInterfaces(Client client, int interfaceId, int removeId, int removeSlot) {
        final int amount = 5;
        if (interfaceId >= 4233 && interfaceId <= 4257) { // Gold crafting
            client.startGoldCrafting(interfaceId, removeSlot, amount);
        } else if (interfaceId == 3823) { // sell to shop (sells 1)
            client.sellItem(removeId, removeSlot, 1);
        } else if (interfaceId == 3900) { // buy from shop (buys 1)
            client.buyItem(removeId, removeSlot, 1);
        } else if (interfaceId >= 1119 && interfaceId <= 1123) { // smithing
            if (client.smithing[2] > 0) {
                client.smithing[4] = removeId;
                client.smithing[0] = 1;
                client.smithing[5] = amount;
                client.send(new RemoveInterfaces());
            } else {
                client.send(new SendMessage("Illigal Smithing !"));
                logger.debug("Illegal Smith attempt by {}", client.getPlayerName());
            }
        } else if (interfaceId == 1688) { // operate equipment – animations and special chats
            if (removeId == 4566) {
                client.requestAnim(1835, 0);
            } else if (removeSlot == 0 && client.gotSlayerHelmet(client)) {
                net.dodian.uber.game.model.player.skills.slayer.SlayerTask.sendTask(client);
            }
        }
    }
}
