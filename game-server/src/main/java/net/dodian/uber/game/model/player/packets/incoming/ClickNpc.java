package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;

public class ClickNpc implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int npcIndex = client.getInputStream().readSignedWordBigEndian();
        Npc tempNpc = Server.npcManager.getNpc(npcIndex);
        if (tempNpc == null) {
            return;
        }
        int npcId = tempNpc.getId();

        final WalkToTask task = new WalkToTask(WalkToTask.Action.NPC_FIRST_CLICK, npcId, tempNpc.getPosition());
        client.setWalkToTask(task);
        if (client.randomed) {
            return;
        }
        if (!client.playerPotato.isEmpty())
            client.playerPotato.clear();
        if (npcId == 2794) {
            if (client.playerHasItem(1735)) {
                client.addItem(1737, 1);
            } else {
                client.send(new SendMessage("You need some shears to shear this sheep!"));
            }
            return;
        }
        EventManager.getInstance().registerEvent(new Event(600) {

            @Override
            public void execute() {

                if (client.disconnected || client.getWalkToTask() != task) {
                    this.stop();
                    return;
                }

                if (!client.goodDistanceEntity(tempNpc, 1)) {
                    return;
                }

                clickNpc(client, tempNpc);
                client.setWalkToTask(null);
                this.stop();
            }

        });
    }

    public void clickNpc(Client client, Npc tempNpc) {
        int npcId = tempNpc.getId();
        client.faceNpc(tempNpc.getSlot());
        // TurnPlayerTo(tempNpc.getX(), tempNpc.getY());
        client.skillX = tempNpc.getPosition().getX();
        client.setSkillY(tempNpc.getPosition().getY());
        if (npcId == 5809) {
            client.NpcWanneTalk = 804;
            // openTan();
        }
        if (npcId == 5792) {
            client.triggerTele(3045, 3372, 0, false);
            client.send(new SendMessage("Welcome to the party room!"));
        }
        if (npcId == 3306) {
            int peopleInEdge = 0;
            int peopleInWild = 0;
            for (int a = 0; a < Constants.maxPlayers; a++) {
            }
            client.showNPCChat(3306, 590, new String[]{"There is currently " + peopleInWild + " player" + (peopleInWild != 1 ? "s" : "") + " in the wild!", "There is " + peopleInEdge + " player" + (peopleInEdge != 1 ? "s" : "") + " in Edgeville!"});
        }
        client.startFishing(npcId, 1);
        if (npcId == 394 || npcId == 395 || npcId == 7677) { /* Banking */
            client.NpcWanneTalk = 1;
            client.convoId = 0;
        } else if (npcId == 637) { /* Aubury */
            client.NpcWanneTalk = 3;
            client.convoId = 3;
        } else if (npcId == 3648) {
            client.NpcWanneTalk = 3648;
        } else if (npcId == 1307 ||npcId == 1306) {
            client.NpcWanneTalk = 21;
        } else if (npcId == 6481) {
            client.NpcWanneTalk = npcId;
        } else if (npcId == 2345) {
            client.NpcWanneTalk = npcId;
        } else if (npcId == 2180) {
            client.NpcWanneTalk = npcId;
        } else if (npcId == 555) {
                client.quests[0]++;
                client.send(new SendMessage(client.playerRights > 1 ? "Set your quest to: " + client.quests[0] : "Suddenly the monk had an urge to dissapear!"));
        } else if (npcId == 683) { // Range stuff
            client.WanneShop = 11;
        } else if (npcId == 3951) {
            if (client.premium) {
                client.ReplaceObject(2728, 3349, 2391, 0, 0);
                client.ReplaceObject(2729, 3349, 2392, -2, 0);
                client.showNPCChat(npcId, 590, new String[]{"Welcome to the Guild of Legends", "Enjoy your stay."});
            } else {
                client.showNPCChat(npcId, 595, new String[]{"You must be a premium member to enter", "Visit Dodian.net to subscribe"});
            }
        } else if (npcId == 376 && client.playerRights == 2) {
            client.triggerTele(2772, 3234, 0, false);
        } else if (npcId == 6080) {
            client.NpcWanneTalk = 162;
        } else if (npcId == 8051) {
            client.NpcWanneTalk = 8051;
        } else if (npcId == 659) {
            client.NpcWanneTalk = 1000;
            client.convoId = 1001;
        } else if (npcId == 3640) { // Beginner store!
            client.WanneShop = 22;
        } else if (npcId == 2825) {
            client.NpcWanneTalk = 1002;
            client.convoId = -1;
        } else if (npcId == 402 || npcId == 403 || npcId == 405) {
            client.NpcWanneTalk = 11;
        } else if (npcId == 1174) {
            client.NpcWanneTalk = 16;
            client.convoId = 2;
        } else if (npcId == 520) {
            client.NpcWanneTalk = 19;
            client.convoId = 4;
        } else if (npcId == 943) {
            int num = 0;
            tempNpc.setText("There are currently " + num + " people in the wilderness");
        } else {
            client.println_debug("atNPC 1: " + npcId);
        }
    }

}
