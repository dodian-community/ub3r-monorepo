package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.party.Balloons;
import net.dodian.utilities.Utils;

public class ClickNpc3 implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int npcIndex = client.getInputStream().readSignedWord();
        Npc tempNpc = Server.npcManager.getNpc(npcIndex);
        if (tempNpc == null)
            return;
        int npcId = tempNpc.getId();

        final WalkToTask task = new WalkToTask(WalkToTask.Action.NPC_THIRD_CLICK, npcId, tempNpc.getPosition());
        client.setWalkToTask(task);
        if (client.randomed || client.UsingAgility) {
            return;
        }
        if (!client.playerPotato.isEmpty())
            client.playerPotato.clear();
        EventManager.getInstance().registerEvent(new Event(600) {

            @Override
            public void execute() {

                if (client.disconnected) {
                    this.stop();
                    return;
                }

                if (client.getWalkToTask() != task) {
                    this.stop();
                    return;
                }

                if (!client.goodDistanceEntity(tempNpc, 1) || tempNpc.getPosition().withinDistance(client.getPosition(), 0)) {
                    return;
                }

                clickNpc3(client, tempNpc);
                client.setWalkToTask(null);
                this.stop();
            }

        });
    }

    public void clickNpc3(Client client, Npc tempNpc) {
        if (client.isBusy()) {
            return;
        }
        int npcId = tempNpc.getId();
        client.resetAction();
        client.faceNpc(tempNpc.getSlot());
        client.skillX = tempNpc.getPosition().getX();
        client.setSkillY(tempNpc.getPosition().getY());
        if (npcId == 637) { /* Mage arena tele */
            if(Balloons.eventActive()) {
                client.triggerTele(3045, 3372, 0, false);
                client.send(new SendMessage("Welcome to the party room!"));
            } else {
                client.triggerTele(3086 + Utils.random(2), 3488 + Utils.random(2), 0, false);
                client.send(new SendMessage("Welcome to Edgeville!"));
            }
        } else if (npcId == 70) {
            client.WanneShop = 2; // Crafting shop
        } else if (npcId >= 402 && npcId <= 405) {
            client.WanneShop = 15; // Slayer Store
        } else if (npcId == 1307 || npcId == 1306) {
            client.NpcWanneTalk = 23;
        } else if (npcId == 4753) {
            client.NpcWanneTalk = 4756;
        }
    }

}
