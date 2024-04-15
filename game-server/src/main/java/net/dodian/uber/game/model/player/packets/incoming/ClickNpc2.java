package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.thieving.Thieving;

public class ClickNpc2 implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int npcIndex = client.getInputStream().readSignedWordBigEndianA();
        Npc tempNpc = Server.npcManager.getNpc(npcIndex);
        if (tempNpc == null)
            return;
        int npcId = tempNpc.getId();

        final WalkToTask task = new WalkToTask(WalkToTask.Action.NPC_SECOND_CLICK, npcId, tempNpc.getPosition());
        client.setWalkToTask(task);
        if (client.randomed) {
            return;
        }
        if (!client.playerPotato.isEmpty())
            client.playerPotato.clear();
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

                clickNpc2(client, tempNpc);
                client.setWalkToTask(null);
                this.stop();
            }

        });
    }

    public void clickNpc2(Client client, Npc tempNpc) {
        if (!tempNpc.isAlive()) {
            client.send(new SendMessage("That monster has been killed!"));
            return;
        }
        int npcId = tempNpc.getId();
        client.resetAction();
        client.faceNpc(tempNpc.getSlot());
        client.skillX = tempNpc.getPosition().getX();
        client.setSkillY(tempNpc.getPosition().getY());
        client.startFishing(npcId, 2);

        switch (npcId) {
            case 3086:
            case 3257:
                Thieving.attemptSteal(client, npcId, tempNpc.getPosition());
                break;
        }
        if (npcId == 394 || npcId == 395 || npcId == 7677) { /* Banking */
            client.WanneBank = 1;
        } else if (npcId == 5034 || npcId == 844 || npcId == 462) {
            client.stairs = 26;
            client.stairDistance = 1;
        } else if (npcId == 1174) {
            client.WanneShop = 19;
        } else if (npcId == 4753) {
            client.WanneShop = 39;
        } else if (npcId == 2345) {
            client.NpcWanneTalk = npcId + 1;
        } else if (npcId == 2180) {
            client.NpcWanneTalk = npcId + 1;
        } else if (npcId == 3648) {
            client.setTravelMenu();
        } else if (npcId == 1779) { // Start plunder
            client.getPlunder.startPlunder();
        } else if (npcId == 506 || npcId == 527) {
            client.WanneShop = 3; // Yanille General Store
        } else if (npcId == 4965) { // Gnome general store
            client.WanneShop = 4;
        } else if (npcId == 1032) { // Catherby general store
            client.WanneShop = 5;
        } else if (npcId == 538) {
            client.WanneShop = 6; // Just a general store?
        } else if (npcId == 6478) { //Axe store
            client.WanneShop = 7;
        } else if (npcId == 3890) { //Pickaxe store
            client.WanneShop = 8;
        } else if (npcId == 637) { // Aubury rune shop
            client.WanneShop = 9; // Aubury Magic Shop
        } else if (npcId == 535) { // Horvik
            client.WanneShop = 10; // Varrock Armor shop
        } else if (npcId == 6060) { // Bow and arrows
            client.WanneShop = 11;
        } else if (npcId == 1027) { // Gerrant
            client.WanneShop = 16; // Fishing shop
        } else if (npcId == 5809) {
            client.WanneShop = 18; // Crafting shop
        } else if (npcId == 6059) { // Archer's Armour
            client.WanneShop = 30; // Cape store!
        } else if (npcId == 3837) { // Baba Yaga
            client.WanneShop = 33;
        } else if (npcId == 4642) { // Shanty
            client.WanneShop = 36;
        } else if (npcId == 402 || npcId == 403 || npcId == 405) {
            client.NpcWanneTalk = 13;
        } else if (npcId == 17) {
            client.NpcWanneTalk = 20;
        } else if (npcId == 19) {
            client.NpcWanneTalk = 20;
        } else if (npcId == 20) {
            client.NpcWanneTalk = 20;
        } else if (npcId == 22) {
            client.NpcWanneTalk = 20;
        } else {
            client.println_debug("atNPC 2: " + npcId);
        }
    }

}
