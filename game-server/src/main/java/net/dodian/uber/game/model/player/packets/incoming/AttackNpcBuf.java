package net.dodian.uber.game.model.player.packets.incoming;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.networking.game.ByteBufPacket;

import static net.dodian.uber.game.combat.PlayerAttackCombatKt.getAttackStyle;

/**
 * ByteBuf implementation of NPC attack handler (opcode 72, size 2).
 */
public class AttackNpcBuf implements ByteBufPacket {

    /**
     * Jagex big-endian A variant (low byte minus 128).
     */
    private static int readUnsignedShortBEA(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        return (high << 8) | low;
    }

    @Override
    public void process(Client client, int opcode, int size, ByteBuf payload) {
        int npcIndex = readUnsignedShortBEA(payload);

        if (client.magicId >= 0) {
            client.magicId = -1;
        }
        if (client.deathStage >= 1) {
            return;
        }

        Npc tempNpc = Server.npcManager.getNpc(npcIndex);
        if (tempNpc == null) {
            return;
        }
        if (client.randomed || client.UsingAgility) {
            return;
        }

        boolean rangedAttack = getAttackStyle(client) != 0;
        if ((rangedAttack && client.goodDistanceEntity(tempNpc, 5)) || client.goodDistanceEntity(tempNpc, 1)) {
            client.resetWalkingQueue();
            client.startAttack(tempNpc);
            return;
        }

        // Too far: walk closer then attack
        final WalkToTask task = new WalkToTask(WalkToTask.Action.ATTACK_NPC, npcIndex, tempNpc.getPosition());
        client.setWalkToTask(task);
        EventManager.getInstance().registerEvent(new Event(600) {
            @Override
            public void execute() {
                if (client.disconnected || client.getWalkToTask() != task) {
                    this.stop();
                    return;
                }
                if ((getAttackStyle(client) != 0 && client.goodDistanceEntity(tempNpc, 5)) || client.goodDistanceEntity(tempNpc, 1)) {
                    client.resetWalkingQueue();
                    client.startAttack(tempNpc);
                    client.setWalkToTask(null);
                    this.stop();
                }
            }
        });
    }
}
