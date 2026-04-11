package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.engine.systems.net.PacketGameplayFacade;
import net.dodian.uber.game.engine.systems.net.WalkRequest;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty implementation of the walking packet handler (opcodes 248, 164, 98).
 * Closely mirrors legacy logic in model/player/packets/incoming/Walking.java.
 */
@net.dodian.uber.game.netty.listener.PacketHandler(opcode = 248)
public final class WalkingListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(WalkingListener.class);
    private static final int OP_MINIMAP_WALK = 248;
    private static final int MIN_WALK_PACKET_SIZE = 5;
    private static final int MINIMAP_TRAILING_BYTES = 14;
    private static final int MIN_WORLD_COORD = 0;
    private static final int MAX_WORLD_COORD = 16382;

    static {
        WalkingListener handler = new WalkingListener();
        safeRegister(248, handler);
        safeRegister(164, handler);
        safeRegister(98, handler);
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        int opcode = packet.opcode();
        int size   = packet.size();

        ByteBuf buf = packet.payload();
        int effectiveSize = resolveEffectiveSize(opcode, size);
        if (effectiveSize < MIN_WALK_PACKET_SIZE) {
            rejectMalformedWalkPacket(client, opcode, size, -1, -1, "effective size below minimum");
            return;
        }
        if (buf.readableBytes() < effectiveSize) {
            rejectMalformedWalkPacket(client, opcode, size, -1, -1, "payload shorter than effective size");
            return;
        }
        if (((effectiveSize - MIN_WALK_PACKET_SIZE) & 1) != 0) {
            rejectMalformedWalkPacket(client, opcode, size, -1, -1, "step payload has odd byte count");
            return;
        }
        if (client.mapRegionX < 0 || client.mapRegionY < 0) {
            rejectMalformedWalkPacket(client, opcode, size, -1, -1, "map region not initialized");
            return;
        }

        int stepCount = ((effectiveSize - MIN_WALK_PACKET_SIZE) / 2) + 1;
        if (stepCount > Player.WALKING_QUEUE_SIZE) {
            rejectMalformedWalkPacket(client, opcode, size, -1, -1, "walk step count exceeds queue size");
            return;
        }
        int[] deltasX = new int[stepCount];
        int[] deltasY = new int[stepCount];
        deltasX[0] = 0;
        deltasY[0] = 0;

        int firstStepXAbs = ByteBufReader.readShort(buf, ValueType.ADD, ByteOrder.LITTLE);

        for (int i = 1; i < stepCount; i++) {
            deltasX[i] = ByteBufReader.readSignedByte(buf, ValueType.NORMAL);
            deltasY[i] = ByteBufReader.readSignedByte(buf, ValueType.NORMAL);
        }

        int firstStepYAbs = ByteBufReader.readShort(buf, ValueType.NORMAL, ByteOrder.LITTLE);
        if (!isValidWorldCoordinate(firstStepXAbs) || !isValidWorldCoordinate(firstStepYAbs)) {
            rejectMalformedWalkPacket(client, opcode, size, firstStepXAbs, firstStepYAbs, "first step out of world bounds");
            return;
        }
        boolean running = (ByteBufReader.readSignedByte(buf, ValueType.NEGATE) == 1);
        WalkRequest request = new WalkRequest(opcode, firstStepXAbs, firstStepYAbs, running, deltasX, deltasY);
        PacketGameplayFacade.handleWalk(client, request);
    }

    private static void safeRegister(int opcode, WalkingListener handler) {
        try {
            PacketListenerManager.register(opcode, handler);
        } catch (RuntimeException ex) {
            logger.debug("Skipping walking listener registration for opcode {}: {}", opcode, ex.getMessage());
        }
    }

    static int resolveEffectiveSize(int opcode, int packetSize) {
        return resolveEffectiveSize(opcode, packetSize, false);
    }

    static int resolveEffectiveSize(int opcode, int packetSize, boolean hasMinimapSuffix) {
        if (opcode == OP_MINIMAP_WALK) {
            return hasMinimapSuffix ? packetSize - MINIMAP_TRAILING_BYTES : packetSize;
        }
        return packetSize;
    }

    private static boolean isValidWorldCoordinate(int value) {
        return value >= MIN_WORLD_COORD && value <= MAX_WORLD_COORD;
    }

    private static void rejectMalformedWalkPacket(
            Client client,
            int opcode,
            int packetSize,
            int firstStepXAbs,
            int firstStepYAbs,
            String reason
    ) {
        PacketGameplayFacade.rejectMalformedWalk(client, opcode, packetSize, firstStepXAbs, firstStepYAbs, reason);
    }
}
