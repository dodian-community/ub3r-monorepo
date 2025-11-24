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
 * Netty implementation of legacy Bank10 packet (opcode 43).
 * Handles withdraw/deposit 10 items and related special interfaces.
 */
public class Bank10Listener implements PacketListener {

    static { PacketListenerManager.register(43, new Bank10Listener()); }

    private static final Logger logger = LoggerFactory.getLogger(Bank10Listener.class);

    /* ---------------- Stream helper equivalence ---------------- */
    private static int readUnsignedWordBigEndian(ByteBuf buf) {
        int low = buf.readUnsignedByte();  // first byte (low)
        int high = buf.readUnsignedByte(); // second byte (high)
        return (high << 8) | low;
    }

    private static int readUnsignedWordA(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        return (high << 8) | low;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();

        // Mystic sends: int interfaceId, short nodeId (WordA), short slot (WordA)
        int interfaceId = buf.readInt();
        int removeId = readUnsignedWordA(buf);
        int removeSlot = readUnsignedWordA(buf);

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
        if (interfaceId >= 4233 && interfaceId <= 4257) { // Gold crafting quantity 10
            client.startGoldCrafting(interfaceId, removeSlot, 10);
        } else if (interfaceId == 3823) { // sell 5 to shop (legacy behaviour)
            client.sellItem(removeId, removeSlot, 5);
        } else if (interfaceId == 3900) { // buy 5 from shop
            client.buyItem(removeId, removeSlot, 5);
        } else if (interfaceId >= 1119 && interfaceId <= 1123) { // smithing quantity depends on inv
            if (client.smithing[2] > 0) {
                client.smithing[4] = removeId;
                client.smithing[0] = 1;
                client.smithing[5] = client.smithing[3] != -1 ? client.getInvAmt(client.smithing[3]) : 10;
                client.send(new RemoveInterfaces());
            } else {
                client.send(new SendMessage("Illigal Smithing !"));
                logger.debug("Illegal Smith attempt by {}", client.getPlayerName());
            }
        }
    }
}
