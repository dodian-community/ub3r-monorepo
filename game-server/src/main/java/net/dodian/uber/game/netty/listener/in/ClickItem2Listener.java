package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.engine.systems.interaction.items.ItemDispatcher;
import net.dodian.uber.game.skill.slayer.Slayer;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.engine.systems.net.PacketItemActionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inventory second-click (opcode 16) – mirrors legacy {@code ClickItem2}.
 */
public class ClickItem2Listener implements PacketListener {

    static { PacketListenerManager.register(16, new ClickItem2Listener()); }

    private static final Logger logger = LoggerFactory.getLogger(ClickItem2Listener.class);
    private static final int MIN_PAYLOAD_BYTES = 4;

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < MIN_PAYLOAD_BYTES) {
            return;
        }

        int itemId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
        int itemSlot = ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.ADD);

        logger.debug("ClickItem2Listener: slot {} item {}", itemSlot, itemId);

        if (!PacketItemActionService.validateInventorySlot(client, itemSlot)) return;
        if (client.playerItems[itemSlot] - 1 != itemId) return;
        if (client.randomed || client.UsingAgility) return;

        if (ItemDispatcher.tryHandle(client, 2, itemId, itemSlot, -1)) {
            return;
        }

        String itemName = client.getItemName(itemId);

        /* Slayer helm task reminder */
        if (itemName.startsWith("Slayer helm")) {
            Slayer.sendCurrentTask(client);
        }
    }
}

