package net.dodian.uber.game.netty.listener.in;
import net.dodian.uber.game.api.content.ContentInteraction;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.engine.systems.interaction.items.ItemDispatcher;
import net.dodian.uber.game.engine.event.GameEventBus;
import net.dodian.uber.game.events.item.ItemClickEvent;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.engine.systems.interaction.PlayerTickThrottleService;
import net.dodian.uber.game.engine.systems.skills.RunecraftingPouchPacketService;
import net.dodian.uber.game.engine.systems.net.PacketItemActionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Netty-based PacketListener that handles all incoming first-click item actions (opcode 122).
 * This class is a complete and faithful replacement for the legacy ClickItem.java file,
 * with corrected packet decoding logic and preserving the original code layout.
 */
@net.dodian.uber.game.netty.listener.PacketHandler(opcodes = {122})
public class ClickItemListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(ClickItemListener.class);
    @Override
    public void handle(Client client, GamePacket packet) {
        net.dodian.uber.game.netty.game.decode.TarnishPackets.ItemOption1 msg =
                net.dodian.uber.game.netty.game.decode.TarnishPackets.ItemOption1.decode(packet.payload());
        if (msg == null) {
            return;
        }
        int interfaceId = msg.interfaceId();
        int itemSlot = msg.slot();
        int itemId = msg.itemId();

        logger.debug("ClickItem: [interface={}, slot={}, id={}] for player {}", interfaceId, itemSlot, itemId, client.getPlayerName());

        if (RunecraftingPouchPacketService.fill(client, itemId)) {
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
        } else if (ContentInteraction.tryAcquireMs(client, ContentInteraction.CLICK_ITEM, 100L)) {
            processItemClick(client, itemId, itemSlot, interfaceId);
        }
    }

    public void processItemClick(Client client, int id, int slot, int interfaceId) {
        PacketItemActionService.handleFirstClickItem(client, id, slot, interfaceId);
    }
}
