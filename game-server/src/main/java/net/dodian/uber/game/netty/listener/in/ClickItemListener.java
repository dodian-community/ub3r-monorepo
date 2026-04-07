package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.systems.dispatch.items.ItemDispatcher;
import net.dodian.uber.game.engine.event.GameEventBus;
import net.dodian.uber.game.events.item.ItemClickEvent;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.systems.interaction.PlayerTickThrottleService;
import net.dodian.uber.game.content.skills.runecrafting.Runecrafting;
import net.dodian.uber.game.systems.net.PacketItemActionService;
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
        ByteBuf buf = packet.payload();

        // Faithful replication of legacy decoding order with the corrected item ID read.
        int interfaceId = buf.readUnsignedShort();
        int itemId = buf.readUnsignedShort();
        int itemSlot = buf.readUnsignedShort();

        // Debug line to confirm packet data is now being read correctly.
        logger.debug("ClickItem: [interface={}, slot={}, id={}] for player {}", interfaceId, itemSlot, itemId, client.getPlayerName());

        if (Runecrafting.fillPouch(client, itemId)) {
            return;
        }

        if (client.randomed || client.UsingAgility) {
            return;
        }

        if (!PacketItemActionService.validateInventorySlot(client, itemSlot)) {
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
        } else if (PlayerTickThrottleService.tryAcquireMs(client, PlayerTickThrottleService.CLICK_ITEM, 100L)) {
            processItemClick(client, itemId, itemSlot, interfaceId);
        }
    }

    public void processItemClick(Client client, int id, int slot, int interfaceId) {
        PacketItemActionService.handleFirstClickItem(client, id, slot, interfaceId);
    }
}
