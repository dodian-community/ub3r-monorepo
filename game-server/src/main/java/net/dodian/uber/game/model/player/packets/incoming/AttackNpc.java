package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;

import static net.dodian.uber.game.combat.PlayerAttackCombatKt.*;

public class AttackNpc implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int npcIndex = client.getInputStream().readUnsignedWordA();
        if(client.magicId >= 0) client.magicId = -1;
        if (client.deathStage < 1) {
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
            if ((rangedAttack && !client.goodDistanceEntity(tempNpc, 5)) || (getAttackStyle(client) == 0 && !client.goodDistanceEntity(tempNpc, 1))) {
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
    }

}
