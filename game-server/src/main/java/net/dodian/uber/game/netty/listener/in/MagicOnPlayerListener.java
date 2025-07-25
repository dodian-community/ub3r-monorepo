package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.player.Client;

import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty implementation of MagicOnPlayer (opcode 249) incoming packet.
 * Decodes packet fields using the same byte order/value transforms as the
 * legacy Stream-based handler and executes the same gameplay logic in-place.
 */
public class MagicOnPlayerListener implements PacketListener {

    static {
        PacketListenerManager.register(249, new MagicOnPlayerListener());
    }

    private static final Logger logger = LoggerFactory.getLogger(MagicOnPlayerListener.class);

    // --- helper readers ----------------------------------------------------
    // readSignedWordA: big-endian, low byte minus 128, signed short (matches Stream.readSignedWordA)
    private static int readSignedWordA(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        int value = (high << 8) | low;
        if (value > 32767) value -= 0x10000;
        return value;
    }

    // readSignedWordBigEndian: little-endian order (low byte first) like Stream.readSignedWordBigEndian
    private static int readSignedWordBigEndian(ByteBuf buf) {
        int low = buf.readUnsignedByte();
        int high = buf.readUnsignedByte();
        int value = (high << 8) | low;
        if (value > 32767) value -= 0x10000;
        return value;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();

        int victimIndex = readSignedWordA(buf);
        int magicId = readSignedWordBigEndian(buf);
        client.magicId = magicId;

        logger.debug("MagicOnPlayerListener: victim {} spell {}", victimIndex, magicId);

        if (client.deathStage >= 1) {
            return;
        }

        Client victim = Server.playerHandler.getClient(victimIndex);
        if (victim == null) {
            return;
        }

        if (client.randomed || client.UsingAgility) {
            return;
        }

        // Replicate legacy distance & task logic
        if (client.goodDistanceEntity(victim, 5)) {
            client.resetWalkingQueue();
            client.startAttack(victim);
            return;
        }

        if (!client.goodDistanceEntity(victim, 5)) {
            final WalkToTask task = new WalkToTask(WalkToTask.Action.ATTACK_PLAYER, victimIndex, victim.getPosition());
            client.setWalkToTask(task);
            EventManager.getInstance().registerEvent(new Event(600) {
                @Override
                public void execute() {
                    if (client.disconnected || client.getWalkToTask() != task) {
                        this.stop();
                        return;
                    }
                    if (client.goodDistanceEntity(victim, 5)) {
                        client.resetWalkingQueue();
                        client.startAttack(victim);
                        client.setWalkToTask(null);
                        this.stop();
                    }
                }
            });
        }
    }
}
