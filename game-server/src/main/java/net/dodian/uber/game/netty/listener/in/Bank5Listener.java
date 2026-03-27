package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.content.interfaces.skilling.SkillingInterfaceItemService;
import net.dodian.uber.game.content.skills.smithing.SmeltingInterfaceService;
import net.dodian.uber.game.content.skills.smithing.SmithingInterfaceService;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.content.events.partyroom.Balloons;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty implementation of legacy Bank5 packet (opcode 117).
 * Moves exactly 5 units of an item between inventory/bank/etc.
 */
public class Bank5Listener implements PacketListener {

    static { PacketListenerManager.register(117, new Bank5Listener()); }

    private static final Logger logger = LoggerFactory.getLogger(Bank5Listener.class);
    private static final int MIN_PAYLOAD_BYTES = 8;

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < MIN_PAYLOAD_BYTES) {
            return;
        }

        // Mystic sends (ItemContainerOption2):
        // int interfaceId (putInt)
        // short nodeId (writeSignedBigEndian)
        // short slot   (writeUnsignedWordBigEndian)
        int interfaceId = ByteBufReader.readInt(buf);
        int removeId = ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.ADD);
        int removeSlot = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
        int bankSlot = removeSlot;

        if ((interfaceId == 5382 || (interfaceId >= 50300 && interfaceId <= 50310)) && client.bankStyleViewOpen) {
            return;
        }

        if (interfaceId == 5382 || (interfaceId >= 50300 && interfaceId <= 50310)) {
            bankSlot = client.resolveBankSlot(interfaceId, removeSlot);
            removeId = client.resolveBankItemId(interfaceId, removeSlot, removeId);
        }

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
                if (bankSlot >= 0) {
                    client.fromBank(removeId, bankSlot, amount);
                }
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
                    if (bankSlot >= 0) {
                        client.fromBank(removeId, bankSlot, amount);
                    }
                } else {
                    handleSpecialInterfaces(client, interfaceId, removeId, removeSlot);
                }
                break;
        }
    }

    private void handleSpecialInterfaces(Client client, int interfaceId, int removeId, int removeSlot) {
        final int amount = 5;
        if (SkillingInterfaceItemService.handleContainerAmount(client, interfaceId, removeId, removeSlot, amount)) {
        } else if (interfaceId == 3823) { // sell to shop (sells 1)
            client.sellItem(removeId, removeSlot, 1);
        } else if (interfaceId == 3900) { // buy from shop (buys 1)
            client.buyItem(removeId, removeSlot, 1);
        } else if (interfaceId == 1688) { // operate equipment – animations and special chats
            if (removeId == 4566) {
                client.performAnimation(1835, 0);
            } else if (removeSlot == 0 && client.gotSlayerHelmet(client)) {
                net.dodian.uber.game.content.skills.slayer.SlayerPlugin.sendCurrentTask(client);
            }
        }
    }
}
