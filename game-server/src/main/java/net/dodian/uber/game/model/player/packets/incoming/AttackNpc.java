package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;

public class AttackNpc implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int npcIndex = client.getInputStream().readUnsignedWordA();
        if (client.deathStage < 1) {
            Npc tempNpc = Server.npcManager.getNpc(npcIndex);
            if (tempNpc == null) {
                return;
            }

            final WalkToTask task = new WalkToTask(WalkToTask.Action.ATTACK_NPC, npcIndex, tempNpc.getPosition());
            client.setWalkToTask(task);
            EventManager.getInstance().registerEvent(new Event(600) {

                @Override
                public void execute() {

                    if (client == null || client.disconnected) {
                        this.stop();
                        return;
                    }

                    if (client.getWalkToTask() != task) {
                        this.stop();
                        return;
                    }
                    client.resetWalkingQueue();
                    client.startAttack(tempNpc);
                    client.setWalkToTask(null);
                    this.stop();
                }

            });
        }
    }

}
