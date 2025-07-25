package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty implementation of legacy {@code MagicOnNpc} (opcode 131).
 * Decoding pattern: LESHORTA (npc index), SHORTA (spell id).
 */
public class MagicOnNpcListener implements PacketListener {

    static { PacketListenerManager.register(131, new MagicOnNpcListener()); }

    private static final Logger logger = LoggerFactory.getLogger(MagicOnNpcListener.class);

    // --- helper readers ---
    // little-endian signed short with ADD transform (low byte − 128)
    private static int readSignedWordLEA(ByteBuf buf) {
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        int high = buf.readUnsignedByte();
        int val = (high << 8) | low;
        if (val > 32767) val -= 0x10000;
        return val;
    }

    // big-endian signed short with ADD transform (low byte − 128)
    private static int readSignedWordBEA(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        int val = (high << 8) | low;
        if (val > 32767) val -= 0x10000;
        return val;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();
        if (buf.readableBytes() < 4) { // 2 + 2
            return;
        }

        int npcIndex = readSignedWordLEA(buf);
        int magicId  = readSignedWordBEA(buf);
        client.magicId = magicId;

        if (client.deathStage >= 1) return;

        Npc npc = Server.npcManager.getNpc(npcIndex);
        if (npc == null) return;
        if (client.randomed || client.UsingAgility) return;

        // If already in distance start attack immediately
        if (client.goodDistanceEntity(npc, 5)) {
            client.resetWalkingQueue();
            client.startAttack(npc);
            return;
        }

        // Otherwise set walk task and attack when close enough
        WalkToTask task = new WalkToTask(WalkToTask.Action.ATTACK_NPC, npcIndex, npc.getPosition());
        client.setWalkToTask(task);

        EventManager.getInstance().registerEvent(new Event(600) {
            @Override
            public void execute() {
                if (client.disconnected || client.getWalkToTask() != task) {
                    this.stop();
                    return;
                }
                if (client.goodDistanceEntity(npc, 5)) {
                    client.resetWalkingQueue();
                    client.startAttack(npc);
                    client.setWalkToTask(null);
                    this.stop();
                }
            }
        });

        logger.debug("MagicOnNpcListener: magic {} on npc {}", magicId, npcIndex);
    }
}
