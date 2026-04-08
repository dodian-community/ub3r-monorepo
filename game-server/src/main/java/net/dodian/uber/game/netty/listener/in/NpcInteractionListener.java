package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.systems.dispatch.npcs.NpcClickMetrics;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.systems.net.PacketInteractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consolidated Netty handler for npc interaction opcodes:
 * 155 (click1), 17 (click2), 21 (click3), 18 (click4), 72 (attack), 230 (legacy click2 compat).
 */
@PacketHandler(opcode = 155)
public class NpcInteractionListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(NpcInteractionListener.class);
    private static final boolean COMPAT_OPCODE_230_ENABLED = readFlag("npc.click.compat230.enabled", true);

    static {
        NpcInteractionListener listener = new NpcInteractionListener();
        PacketListenerManager.register(155, listener);
        PacketListenerManager.register(17, listener);
        PacketListenerManager.register(21, listener);
        PacketListenerManager.register(18, listener);
        PacketListenerManager.register(230, listener);
        PacketListenerManager.register(72, listener);
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        switch (packet.opcode()) {
            case 155:
                handleNpcClick1(client, packet);
                return;
            case 17:
                handleNpcClick2(client, packet);
                return;
            case 21:
                handleNpcClick3(client, packet);
                return;
            case 18:
                handleNpcClick4(client, packet);
                return;
            case 230:
                handleNpcClick2LegacyCompat(client, packet);
                return;
            case 72:
                handleNpcAttack(client, packet);
                return;
            default:
                logger.warn("NpcInteractionListener got unexpected opcode={} for player={}", packet.opcode(), client.getPlayerName());
        }
    }

    private void handleNpcClick1(Client client, GamePacket packet) {
        ByteBuf payload = packet.payload();
        if (payload.readableBytes() < 2) {
            NpcClickMetrics.recordRejected("short_payload", packet.opcode(), 1, -1, client.getPlayerName());
            return;
        }
        int npcIndex = ByteBufReader.readShortUnsigned(payload, ByteOrder.LITTLE, ValueType.NORMAL);
        NpcClickMetrics.recordDecoded(packet.opcode(), 1, npcIndex, client.getPlayerName());
        PacketInteractionService.handleNpcClick(client, packet.opcode(), 1, npcIndex);
    }

    private void handleNpcClick2(Client client, GamePacket packet) {
        ByteBuf payload = packet.payload();
        if (payload.readableBytes() < 2) {
            NpcClickMetrics.recordRejected("short_payload", packet.opcode(), 2, -1, client.getPlayerName());
            return;
        }
        int npcIndex = ByteBufReader.readShortUnsigned(payload, ByteOrder.LITTLE, ValueType.ADD);
        NpcClickMetrics.recordDecoded(packet.opcode(), 2, npcIndex, client.getPlayerName());
        PacketInteractionService.handleNpcClick(client, packet.opcode(), 2, npcIndex);
    }

    private void handleNpcClick3(Client client, GamePacket packet) {
        ByteBuf payload = packet.payload();
        if (payload.readableBytes() < 2) {
            NpcClickMetrics.recordRejected("short_payload", packet.opcode(), 3, -1, client.getPlayerName());
            return;
        }
        int npcIndex = ByteBufReader.readShortUnsigned(payload, ByteOrder.BIG, ValueType.NORMAL);
        NpcClickMetrics.recordDecoded(packet.opcode(), 3, npcIndex, client.getPlayerName());
        PacketInteractionService.handleNpcClick(client, packet.opcode(), 3, npcIndex);
    }

    private void handleNpcClick4(Client client, GamePacket packet) {
        ByteBuf payload = packet.payload();
        if (payload.readableBytes() < 2) {
            NpcClickMetrics.recordRejected("short_payload", packet.opcode(), 4, -1, client.getPlayerName());
            return;
        }
        int npcIndex = ByteBufReader.readShortUnsigned(payload, ByteOrder.LITTLE, ValueType.NORMAL);
        NpcClickMetrics.recordDecoded(packet.opcode(), 4, npcIndex, client.getPlayerName());
        PacketInteractionService.handleNpcClick(client, packet.opcode(), 4, npcIndex);
    }

    private void handleNpcClick2LegacyCompat(Client client, GamePacket packet) {
        if (!COMPAT_OPCODE_230_ENABLED) {
            NpcClickMetrics.recordRejected("compat230_disabled", packet.opcode(), 2, -1, client.getPlayerName());
            return;
        }
        ByteBuf payload = packet.payload();
        if (payload.readableBytes() < 2) {
            NpcClickMetrics.recordRejected("short_payload", packet.opcode(), 2, -1, client.getPlayerName());
            return;
        }
        int npcIndex = decodeCompat230NpcIndex(payload);
        NpcClickMetrics.recordDecoded(packet.opcode(), 2, npcIndex, client.getPlayerName());
        PacketInteractionService.handleNpcClick(client, packet.opcode(), 2, npcIndex);
    }

    private void handleNpcAttack(Client client, GamePacket packet) {
        ByteBuf payload = packet.payload();
        if (payload.readableBytes() < 2) {
            NpcClickMetrics.recordRejected("short_payload", packet.opcode(), 5, -1, client.getPlayerName());
            return;
        }
        int npcIndex = ByteBufReader.readShortUnsigned(payload, ByteOrder.BIG, ValueType.ADD);
        NpcClickMetrics.recordDecoded(packet.opcode(), 5, npcIndex, client.getPlayerName());

        logger.debug("Npc attack opcode={} npcIndex={} player={}", packet.opcode(), npcIndex, client.getPlayerName());
        PacketInteractionService.handleNpcAttack(client, packet.opcode(), npcIndex);
    }

    private int decodeCompat230NpcIndex(ByteBuf payload) {
        ByteBuf addOrder = payload.duplicate();
        int addIndex = ByteBufReader.readShortUnsigned(addOrder, ByteOrder.LITTLE, ValueType.ADD);
        if (Server.npcManager.getNpcMap().containsKey(addIndex)) {
            return addIndex;
        }
        ByteBuf normalOrder = payload.duplicate();
        int normalIndex = ByteBufReader.readShortUnsigned(normalOrder, ByteOrder.LITTLE, ValueType.NORMAL);
        if (Server.npcManager.getNpcMap().containsKey(normalIndex)) {
            return normalIndex;
        }
        return addIndex;
    }

    private static boolean readFlag(String property, boolean defaultValue) {
        String prop = System.getProperty(property);
        if (prop != null && !prop.trim().isEmpty()) {
            return "true".equalsIgnoreCase(prop.trim());
        }
        String envKey = property.toUpperCase().replace('.', '_');
        String env = System.getenv(envKey);
        if (env != null && !env.trim().isEmpty()) {
            return "true".equalsIgnoreCase(env.trim());
        }
        return defaultValue;
    }
}
