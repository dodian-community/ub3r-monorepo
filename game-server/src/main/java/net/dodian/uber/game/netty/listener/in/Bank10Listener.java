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
import net.dodian.uber.game.party.Balloons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Netty implementation of legacy Bank10 packet (opcode 43).
 * Handles withdraw/deposit 10 items and related special interfaces.
 */
public class Bank10Listener implements PacketListener {

    static { PacketListenerManager.register(43, new Bank10Listener()); }

    private static final Logger logger = LoggerFactory.getLogger(Bank10Listener.class);

    private static final int MIN_PAYLOAD_BYTES = 8;

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < MIN_PAYLOAD_BYTES) {
            return;
        }

        // Mystic sends: int interfaceId, short nodeId (WordA), short slot (WordA)
        int interfaceId = ByteBufReader.readInt(buf);
        int removeId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
        int removeSlot = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
        int bankSlot = removeSlot;

        if ((interfaceId == 5382 || (interfaceId >= 50300 && interfaceId <= 50310)) && client.bankStyleViewOpen) {
            return;
        }

        if (interfaceId == 5382 || (interfaceId >= 50300 && interfaceId <= 50310)) {
            bankSlot = client.resolveBankSlot(interfaceId, removeSlot);
            removeId = client.resolveBankItemId(interfaceId, removeSlot, removeId);
        }

        if (client.playerRights == 2) {
            client.println_debug("Bank10: interfaceId=" + interfaceId + " itemId=" + removeId + " slot=" + removeSlot);
        }

        final int amount = 10;

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
        if (SkillingInterfaceItemService.handleContainerAmount(client, interfaceId, removeId, removeSlot, interfaceId >= 1119 && interfaceId <= 1123 ? client.getInvAmt(removeId) : 10)) {
        } else if (interfaceId == 3823) { // sell 5 to shop (legacy behaviour)
            client.sellItem(removeId, removeSlot, 5);
        } else if (interfaceId == 3900) { // buy 5 from shop
            client.buyItem(removeId, removeSlot, 5);
        }
    }
}
