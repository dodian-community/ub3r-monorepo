package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Skills;
import net.dodian.uber.game.model.player.skills.agility.Werewolf;

import java.util.Date;
import java.util.Objects;

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
        if (client.randomed || client.UsingAgility) {
            return;
        }
        if (!client.playerPotato.isEmpty())
            client.playerPotato.clear();
        if (npcId == 2794) {
            if (client.playerHasItem(1735)) {
                client.addItem(1737, 1);
                client.checkItemUpdate();
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

                if (!client.goodDistanceEntity(tempNpc, 1) || tempNpc.getPosition().withinDistance(client.getPosition(), 0)) {
                    return;
                }

                clickNpc(client, tempNpc);
                client.setWalkToTask(null);
                this.stop();
            }

        });
    }

    public void clickNpc(Client client, Npc tempNpc) {
        if (!tempNpc.isAlive()) {
            client.send(new SendMessage("That monster has been killed!"));
            return;
        }
        int npcId = tempNpc.getId();
        client.resetAction();
        client.faceNpc(tempNpc.getSlot());
        client.skillX = tempNpc.getPosition().getX();
        client.setSkillY(tempNpc.getPosition().getY());
        if (npcId == 5809)
            client.openTan();
        if (npcId == 5792) {
            client.triggerTele(3045, 3372, 0, false);
            client.send(new SendMessage("Welcome to the party room!"));
        }
        if (npcId == 3306) {
            int peopleInEdge = 0;
            int peopleInWild = 0;
            for (int a = 0; a < Constants.maxPlayers; a++) {
                Client checkPlayer = (Client) PlayerHandler.players[a];
                if (checkPlayer != null) {
                    if (checkPlayer.inWildy())
                        peopleInWild++;
                    else if (checkPlayer.inEdgeville())
                        peopleInEdge++;
                }
            }
            client.showNPCChat(3306, 590, new String[]{"There is currently " + peopleInWild + " player" + (peopleInWild != 1 ? "s" : "") + " in the wild!", "There is " + peopleInEdge + " player" + (peopleInEdge != 1 ? "s" : "") + " in Edgeville!"});
        }
        /* Fishing spots */
        client.startFishing(npcId, 1);
        if (npcId == 394 || npcId == 395 || npcId == 7677) { /* Banking */
            client.NpcWanneTalk = 1;
            client.convoId = 0;
        } else if (npcId == 5927) {
            Werewolf wolf = new Werewolf(client);
            wolf.handStick();
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
        } else if (npcId == 3837) {
            client.NpcWanneTalk = npcId;
        } else if (npcId == 2180) {
            client.NpcWanneTalk = npcId;
        } else if (npcId == 555) {
                client.quests[0]++;
                client.send(new SendMessage(client.playerRights > 1 ? "Set your quest to: " + client.quests[0] : "Suddenly the monk had an urge to dissapear!"));
        } else if (npcId == 683) { // Range stuff
            client.WanneShop = 11;
        } else if (npcId == 2053) { // Glass store!
            client.WanneShop = 32;
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
        } else if (npcId == 6080 && tempNpc.getSlot() < Server.npcManager.gnomeSpawn) {
            client.NpcWanneTalk = 162;
        } else if (npcId == 8051) {
            client.NpcWanneTalk = 8051;
        } else if (npcId == 659) {
            client.NpcWanneTalk = 1000;
            client.convoId = 1001;
        } else if (npcId == 3640) { // Beginner store!
            client.WanneShop = 17;
        } else if (npcId == 556) {
            client.WanneShop = 31; // Premium store
        } else if (npcId == 557) { //Boost in test area!
            tempNpc.requestAnim(5643, 0);
            for(int skill = 0; skill < 4; skill++) {
                skill = skill == 3 ? 4 : skill;
                client.boost(5 + (int) (Skills.getLevelForExperience(client.getExperience(Objects.requireNonNull(Skill.getSkill(skill)))) * 0.15), Skill.getSkill(skill));
            }
            int ticks = (1 + Skills.getLevelForExperience(client.getExperience(Skill.HERBLORE))) * 2;
            client.addEffectTime(2, 200 + ticks);
            client.send(new SendMessage("The monk boost your stats!"));
        } else if (npcId == 4808) {
            client.WanneShop = 34; // Battlestaff shop two
        } else if (npcId == 3541) {
            client.WanneShop = 35; // Battlestaff shop three
        } else if (npcId == 2825) {
            client.NpcWanneTalk = 1002;
            client.convoId = -1;
        } else if (npcId == 402 || npcId == 403 || npcId == 405) {
            client.NpcWanneTalk = 11;
        } else if (npcId == 520) {
            client.NpcWanneTalk = 19;
            client.convoId = 4;
        } else if (npcId == 1174) {
            client.NpcWanneTalk = npcId;
        } else if (npcId == 4753) {
            client.NpcWanneTalk = npcId;
        } else if (npcId == 17) {
            client.NpcWanneTalk = 17;
        } else if (npcId == 19) {
            client.NpcWanneTalk = 17;
        } else if (npcId == 20) {
            client.NpcWanneTalk = 17;
        } else if (npcId == 22) {
            client.NpcWanneTalk = 17;
        } else if (npcId == 5842) { //Compensate dialogue!
            boolean canClaim = new Date().before(new Date("06/1/2024")) && !client.checkItem(7927);
            if (canClaim) {
                client.showNPCChat(npcId, 595, new String[]{"Here take a easter ring for all your troubles.", "Enjoy your stay at Dodian."});
                client.addItem(7927, 1);
                client.checkItemUpdate();
            } else
                client.showNPCChat(npcId, 595, new String[]{client.checkItem(7927) ? "You already got the ring." : "It is not May anymore."});
        } else if (npcId == 1779) { //Plunder dialogue TODO!
            client.showNPCChat(1779, 605, new String[]{"What are you even doing in here?!", "Begone from me!"});
        } else if (npcId == 943) {
            int num = 0;
            for (Player p : PlayerHandler.players) {
                if (p != null && p.wildyLevel > 0)
                    num++;
            }
            tempNpc.setText("There are currently " + num + " people in the wilderness");
        } else {
            client.println_debug("atNPC 1: " + npcId);
        }
    }

}
