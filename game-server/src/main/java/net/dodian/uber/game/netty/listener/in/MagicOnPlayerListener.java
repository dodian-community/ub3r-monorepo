package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.GameEventScheduler;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
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

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < 4) {
            return;
        }

        int victimIndex = ByteBufReader.readShortSigned(buf, ByteOrder.BIG, ValueType.ADD);
        int magicId = ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
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
            GameEventScheduler.runRepeatingMs(600, () -> {
                if (client.disconnected || client.getWalkToTask() != task) {
                    return false;
                }
                if (client.goodDistanceEntity(victim, 5)) {
                    client.resetWalkingQueue();
                    client.startAttack(victim);
                    client.setWalkToTask(null);
                    return false;
                }
                return true;
            });
        }
    }
}
