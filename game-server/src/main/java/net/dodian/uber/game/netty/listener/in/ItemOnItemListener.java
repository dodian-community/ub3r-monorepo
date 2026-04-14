package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.engine.metrics.PacketRejectTelemetry;
import net.dodian.uber.game.engine.systems.interaction.items.ItemCombinationService;
import net.dodian.uber.game.engine.event.GameEventBus;
import net.dodian.uber.game.events.item.ItemOnItemEvent;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;

@PacketHandler(opcode = 53)
public class ItemOnItemListener implements PacketListener {

    static {
        PacketListenerManager.register(53, new ItemOnItemListener());
    }

    private static final int MIN_PAYLOAD_BYTES = 8;

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < MIN_PAYLOAD_BYTES) {
            PacketRejectTelemetry.record(packet.opcode(), "short_payload");
            return;
        }

        int usedWithSlot = buf.readUnsignedShort();
        int itemUsedSlot = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
        buf.readUnsignedShort();
        buf.readUnsignedShort();

        if (usedWithSlot < 0 || usedWithSlot >= client.playerItems.length || itemUsedSlot < 0 || itemUsedSlot >= client.playerItems.length) {
            PacketRejectTelemetry.record(packet.opcode(), "invalid_slot_bounds");
            return;
        }
        int usedWithId = client.playerItems[usedWithSlot] - 1;
        int itemUsedId = client.playerItems[itemUsedSlot] - 1;
        if (usedWithId < 0 || itemUsedId < 0) {
            PacketRejectTelemetry.record(packet.opcode(), "invalid_item_id");
            return;
        }
        if (GameEventBus.postWithResult(new ItemOnItemEvent(client, itemUsedSlot, usedWithSlot, itemUsedId, usedWithId))) {
            return;
        }
        ItemCombinationService.handle(client, usedWithSlot, itemUsedSlot);
    }
}
