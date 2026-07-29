package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.engine.systems.interaction.items.ItemDispatcher;
import net.dodian.uber.game.engine.systems.skills.RunecraftingPouchPacketService;
import net.dodian.uber.game.engine.systems.skills.SlayerTaskMessageBridge;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.engine.systems.net.PacketItemActionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inventory second-click (opcode 16) – mirrors legacy {@code ClickItem2}.
 */
@net.dodian.uber.game.netty.listener.PacketHandler(opcodes = {16})
public class ClickItem2Listener implements PacketListener {
  private static final Logger logger = LoggerFactory.getLogger(ClickItem2Listener.class);
    @Override
    public void handle(Client client, GamePacket packet) {
        net.dodian.uber.game.netty.game.decode.TarnishPackets.ItemOption2 msg =
                net.dodian.uber.game.netty.game.decode.TarnishPackets.ItemOption2.decode(packet.payload());
        if (msg == null) {
            return;
        }
        int itemId = msg.itemId();
        int itemSlot = msg.slot();
        int interfaceId = msg.interfaceId();

        logger.debug("ClickItem2Listener: slot {} item {} interface {}", itemSlot, itemId, interfaceId);

        if (!PacketItemActionService.validateInventorySlot(client, itemSlot)) return;
        if (client.playerItems[itemSlot] - 1 != itemId) return;
        if (client.randomed || client.UsingAgility) return;

        if (RunecraftingPouchPacketService.check(client, itemId)) {
            return;
        }

        if (ItemDispatcher.tryHandle(client, 2, itemId, itemSlot, interfaceId)) {
            return;
        }

        String itemName = client.getItemName(itemId);

        /* Slayer helm task reminder */
        if (itemName.startsWith("Slayer helm")) {
            SlayerTaskMessageBridge.sendCurrentTask(client);
        }
    }
}
