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

        int removeSlot = readUnsignedWordA(buf);
        int interfaceId = buf.readUnsignedShort();
        int removeId = readUnsignedWordA(buf);
        boolean stack = Server.itemManager.isStackable(removeId);

        if (interfaceId == 5064) { // inventory → bank / party chest
            if (client.IsBanking) {
                client.bankItem(removeId, removeSlot, stack ? client.playerItemsN[removeSlot] : client.getInvAmt(removeId));
            } else if (client.isPartyInterface) {
                Balloons.offerItems(client, removeId, stack ? client.playerItemsN[removeSlot] : client.getInvAmt(removeId), removeSlot);
            }
            client.checkItemUpdate();
        } else if (interfaceId == 5382) { // bank → inventory (withdraw all)
            client.fromBank(removeId, removeSlot, -2);
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
                client.sellItem(removeId, removeSlot, 10);
            } else {
                client.send(new SendFrame27());
                client.XinterfaceID = interfaceId;
                client.XremoveID = removeId;
                client.XremoveSlot = removeSlot;
            }
        } else if (interfaceId == 3900) { // buy 10 items from shop
            client.send(new SendFrame27());
            client.XinterfaceID = interfaceId;
            client.XremoveID = removeId;
            client.XremoveSlot = removeSlot;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("BankAll: if={} id={} slot={} stack={} from {}", interfaceId, removeId, removeSlot, stack, client.getPlayerName());
        }
    }
}
