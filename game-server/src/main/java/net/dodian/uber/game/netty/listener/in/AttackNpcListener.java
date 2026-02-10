package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.combat.PlayerAttackCombatKt;
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
 * Netty handler for opcode 72 (attack NPC).
 * Full port of legacy {@code AttackNpc.ProcessPacket}.
 */
public class AttackNpcListener implements PacketListener {

    static { PacketListenerManager.register(72, new AttackNpcListener()); }

    private static final Logger logger = LoggerFactory.getLogger(AttackNpcListener.class);

    // Stream helper matching readUnsignedWordA()
    private static int readUnsignedWordA(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        return (high << 8) | low;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();
        int npcIndex = readUnsignedWordA(buf);

        logger.debug("AttackNpcListener: npcIndex {}", npcIndex);
        if (client.magicId >= 0) {
            client.magicId = -1;
        }
        if (client.deathStage >= 1) return;

        Npc npc = Server.npcManager.getNpc(npcIndex);
        if (npc == null) return;
        if (client.randomed || client.UsingAgility) return;

        boolean rangedAttack = PlayerAttackCombatKt.getAttackStyle(client) != 0;
        if ((rangedAttack && client.goodDistanceEntity(npc, 5)) || client.goodDistanceEntity(npc, 1)) {
            client.resetWalkingQueue();
            client.startAttack(npc);
            return;
        }

        WalkToTask task = new WalkToTask(WalkToTask.Action.ATTACK_NPC, npcIndex, npc.getPosition());
        client.setWalkToTask(task);
        EventManager.getInstance().registerEvent(new Event(600) {
            @Override
            public void execute() {
                if (client.disconnected || client.getWalkToTask() != task) {
                    stop();
                    return;
                }
                if ((PlayerAttackCombatKt.getAttackStyle(client) != 0 && client.goodDistanceEntity(npc, 5))
                        || client.goodDistanceEntity(npc, 1)) {
                    client.resetWalkingQueue();
                    client.startAttack(npc);
                    client.setWalkToTask(null);
                    stop();
                }
            }
        });
    }
}
