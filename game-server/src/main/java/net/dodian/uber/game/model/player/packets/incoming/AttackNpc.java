package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.utilities.Utils;

import static net.dodian.uber.game.combat.PlayerAttackCombatKt.*;

public class AttackNpc implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int npcIndex = client.getInputStream().readUnsignedWordA();
        if (client.deathStage < 1) {
            Npc tempNpc = Server.npcManager.getNpc(npcIndex);
            if (tempNpc == null) {
                return;
            }
            if ((getAttackStyle(client) != 0 && !client.goodDistanceEntity(tempNpc, 5)) || (getAttackStyle(client) == 0 && !client.goodDistanceEntity(tempNpc, 1))) {
                final WalkToTask task = new WalkToTask(WalkToTask.Action.ATTACK_NPC, npcIndex, tempNpc.getPosition());
                client.setWalkToTask(task);
                if ((getAttackStyle(client) != 0 && client.goodDistanceEntity(tempNpc, 5)) || client.goodDistanceEntity(tempNpc, 1)) {
                    client.stopMovement();
                    client.startAttack(tempNpc);
                    client.setWalkToTask(null);
                    return;
                }
                EventManager.getInstance().registerEvent(new Event(600) {
                    @Override
                    public void execute() {
                        if (client.disconnected || client.getWalkToTask() != task) {
                            this.stop();
                            return;
                        }
                        if ((getAttackStyle(client) != 0 && client.goodDistanceEntity(tempNpc, 5)) || client.goodDistanceEntity(tempNpc, 1)) {
                            client.stopMovement();
                            client.startAttack(tempNpc);
                            client.setWalkToTask(null);
                            this.stop();
                        }
                    }
                });
            } else { //In range so attack!
                client.stopMovement();
                client.startAttack(tempNpc);
            }
        }
    }

}
