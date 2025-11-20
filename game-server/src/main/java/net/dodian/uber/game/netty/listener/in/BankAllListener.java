package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.netty.listener.out.SendFrame27;
import net.dodian.uber.game.party.Balloons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty port of legacy BankAll packet (opcode 129).
 * The packet moves all units of an item between inventory/bank/trade/duel/etc.
 */
public class BankAllListener implements PacketListener {

    static { PacketListenerManager.register(129, new BankAllListener()); }

    private static final Logger logger = LoggerFactory.getLogger(BankAllListener.class);

    /**
     * Helper to reproduce Stream.readUnsignedWordA(): big-endian short where the low byte is offset by -128.
     */
    private static int readUnsignedWordA(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        return (high << 8) | low;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();

        // Mystic sends (ItemContainerOption4):
        // short slot (writeUnsignedWordA)
        // int   interfaceId (putInt)
        // short nodeId (writeUnsignedWordA)
        int removeSlot = readUnsignedWordA(buf);
        int interfaceId = buf.readInt();
        int removeId = readUnsignedWordA(buf);

        // Resolve the real item id from local container for bank / inventory
        int resolvedItemId = removeId;
        if (interfaceId == 5064) { // inventory
            if (removeSlot >= 0 && removeSlot < client.playerItems.length && client.playerItems[removeSlot] > 0) {
                resolvedItemId = client.playerItems[removeSlot] - 1;
            }
        } else if (interfaceId == 5382 || (interfaceId >= 50300 && interfaceId <= 50310)) { // bank tabs
            if (removeSlot >= 0 && removeSlot < client.bankItems.length && client.bankItems[removeSlot] > 0) {
                resolvedItemId = client.bankItems[removeSlot] - 1;
            }
        }

        boolean stack = Server.itemManager.isStackable(resolvedItemId);

        if (interfaceId == 5064) { // inventory → bank / party chest
            if (client.IsBanking) {
                int amount = stack ? client.playerItemsN[removeSlot] : client.getInvAmt(resolvedItemId);
                client.bankItem(resolvedItemId, removeSlot, amount);
            } else if (client.isPartyInterface) {
                int amount = stack ? client.playerItemsN[removeSlot] : client.getInvAmt(resolvedItemId);
                Balloons.offerItems(client, resolvedItemId, amount, removeSlot);
            }
            client.checkItemUpdate();
        } else if (interfaceId == 5382 || (interfaceId >= 50300 && interfaceId <= 50310)) { // bank → inventory (withdraw all, mystic tabs: 50300-50310)
            client.fromBank(resolvedItemId, removeSlot, -2);
        } else if (interfaceId == 2274) { // party chest → inventory
            Balloons.removeOfferItems(client, removeId, !stack ? 8 : Integer.MAX_VALUE, removeSlot);
        } else if (interfaceId == 3322 && client.inTrade && client.canOffer) { // inventory → trade
            client.tradeItem(removeId, removeSlot, stack ? client.playerItemsN[removeSlot] : client.getInvAmt(removeId));
        } else if (interfaceId == 3322 && client.inDuel && client.canOffer) { // inventory → duel
            client.stakeItem(removeId, removeSlot, stack ? client.playerItemsN[removeSlot] : client.getInvAmt(removeId));
        } else if (interfaceId == 6669 && client.inDuel && client.canOffer) { // duel → inventory
            client.fromDuel(removeId, removeSlot, stack ? client.offeredItems.get(removeSlot).getAmount() : 28);
        } else if (interfaceId == 3415 && client.inTrade && client.canOffer) { // trade → inventory
            client.fromTrade(removeId, removeSlot, stack ? client.offeredItems.get(removeSlot).getAmount() : 28);
        } else if (interfaceId == 3823) { // sell 10 items to shop
            if (client.playerRights < 2) {
                client.sellItem(resolvedItemId, removeSlot, 10);
            } else {
                client.send(new SendFrame27());
                client.XinterfaceID = interfaceId;
                client.XremoveID = resolvedItemId;
                client.XremoveSlot = removeSlot;
            }
        } else if (interfaceId == 3900) { // buy 10 items from shop
            client.send(new SendFrame27());
            client.XinterfaceID = interfaceId;
            client.XremoveID = resolvedItemId;
            client.XremoveSlot = removeSlot;
        }

        if (client.playerRights == 2) {
            client.println_debug("BankAll: interfaceId=" + interfaceId + " itemId=" + resolvedItemId + " slot=" + removeSlot + " stack=" + stack);
        }
    }
}
