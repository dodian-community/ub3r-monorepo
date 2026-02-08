package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.content.items.ItemDispatcher;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Netty-based PacketListener that handles all incoming first-click item actions (opcode 122).
 * This class is a complete and faithful replacement for the legacy ClickItem.java file,
 * with corrected packet decoding logic and preserving the original code layout.
 */
@PacketHandler(opcode = 122)
public class ClickItemListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(ClickItemListener.class);

    static {
        PacketListenerManager.register(122, new ClickItemListener());
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();

        // Faithful replication of legacy decoding order with the corrected item ID read.
        int interfaceId = buf.readUnsignedShort();
        int itemId = buf.readUnsignedShort();
        int itemSlot = buf.readUnsignedShort();

        // Debug line to confirm packet data is now being read correctly.
        logger.debug("ClickItem: [interface={}, slot={}, id={}] for player {}", interfaceId, itemSlot, itemId, client.getPlayerName());

        if (client.fillEssencePouch(itemId)) {
            return;
        }

        if (client.randomed || client.UsingAgility) {
            return;
        }

        if (itemSlot < 0 || itemSlot >= client.playerItems.length) {
            client.disconnected = true;
            return;
        }

        if (client.playerItems[itemSlot] - 1 != itemId) {
            logger.warn("ClickItem Mismatch: Player {} tried to use item {} from slot {}, but found {}",
                    client.getPlayerName(), itemId, itemSlot, client.playerItems[itemSlot] - 1);
            return;
        }

        boolean isHerb = (itemId >= 199 && itemId <= 219) || itemId == 3049 || itemId == 3051;
        if (isHerb) {
            processItemClick(client, itemId, itemSlot, interfaceId);
        } else if (System.currentTimeMillis() - client.lastAction > 100) {
            processItemClick(client, itemId, itemSlot, interfaceId);
            client.lastAction = System.currentTimeMillis();
        }
    }

    public void processItemClick(Client client, int id, int slot, int interfaceId) {
        int item = client.playerItems[slot] - 1;
        if (item != id) {
            return;
        }
        boolean bypassDuelGuards = item == 4155 || item == 2528 || item == 6543 || item == 5733;
        if (!bypassDuelGuards && client.duelRule[7] && client.inDuel && client.duelFight) {
            client.send(new SendMessage("Food has been disabled for this duel"));
            return;
        }
        if (!bypassDuelGuards && (client.inDuel || client.duelFight || client.duelConfirmed || client.duelConfirmed2)) {
            if (item != 4155) {
                client.send(new SendMessage("This item cannot be used in a duel!"));
                return;
            }
        }
        if (ItemDispatcher.tryHandle(client, 1, item, slot, interfaceId)) {
            client.checkItemUpdate();
            return;
        }
    }
}
