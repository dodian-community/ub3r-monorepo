package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.Thieving;

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

                if (!client.goodDistanceEntity(tempNpc, 1)) {
                    return;
                }

                clickNpc2(client, tempNpc);
                client.setWalkToTask(null);
                this.stop();
            }

        });
    }

    public void clickNpc2(Client client, Npc tempNpc) {
        int npcId = tempNpc.getId();
        client.faceNPC(tempNpc.getSlot());
        long time = System.currentTimeMillis();
        if (time - client.globalCooldown[0] <= 50) {
            client.send(new SendMessage("Action throttled... please wait longer before acting!"));
            return;
        }

        client.globalCooldown[0] = time;
        int npcX = tempNpc.getPosition().getX();
        int npcY = tempNpc.getPosition().getY();
		/*if (Math.abs(client.getPosition().getX() - npcX) > 50 || Math.abs(client.getPosition().getY() - npcY) > 50) {
			// send(new SendMessage("Client hack detected!");
			// break;
		}*/
        if (!tempNpc.isAlive()) {
            client.send(new SendMessage("That monster has been killed!"));
            return;
        }

        client.skillX = npcX;
        client.setSkillY(npcY);
        client.startFishing(npcId, 2);

        switch (npcId) {
            case 3086:
            case 3257:
                Thieving.attemptSteal(client, npcId, tempNpc.getPosition());
                break;
        }
        if (npcId == 394 || npcId == 495 || npcId == 2619) { /* Banking */
            client.WanneBank = 1;
        } else if (npcId == 5034 || npcId == 844
                || npcId == 462) { /*
         * Essence Mine Guys
         */
            client.stairs = 26;
            client.stairDistance = 1;
            client.Essence = npcId == 5034 ? 1 : npcId == 844 ? 2 : 3;
        } else if (npcId == 1174) {
            client.WanneShop = 39;
        } else if (npcId == 637) { // Aubury rune shop
            client.WanneShop = 9; // Aubury Magic Shop
        } else if (npcId == 522 || npcId == 523) { // Shop Keeper +
            // Assistant
            client.WanneShop = 1; // Varrock General Store
        } else if (npcId == 506 || npcId == 527) { // Shop Keeper +
            // Assistant
            client.WanneShop = 3; // Falador General Store
        } else if (npcId == 577) { // Cassie
            client.WanneShop = 4; // Falador Shield Shop
        } else if (npcId == 580) { // Flynn
            client.WanneShop = 5; // Falador Mace Shop
        } else if (npcId == 538) { // Peksa
            client.WanneShop = 6; // Barbarian Vullage Helmet Shop
        } else if (npcId == 546) { // Zaff
            client.WanneShop = 7; // Varrock Staff Shop
        } else if (npcId == 548) { // Thessalia
            client.WanneShop = 8; // Varrock Cloth shop
        } else if (npcId == 535) { // Horvik
            client.WanneShop = 10; // Varrock Armor shop
        } else if (npcId == 584) { // Heruin
            client.WanneShop = 12; // Falador Gem Shop
        } else if (npcId == 581) { // Wayne
            client.WanneShop = 13; // Falador Chainmail Shop
        } else if (npcId == 585) { // Rommik
            client.WanneShop = 14; // Rimmington Crafting Shop
        } else if (npcId == 531 || npcId == 530) { // Shop Keeper +
            // Assistant
            client.WanneShop = 15; // Rimmington General Store
        } else if (npcId == 1860) { // Brian
            client.WanneShop = 16; // Rimmington Archery Shop
        } else if (npcId == 557) { // Wydin
            client.WanneShop = 17; // Port Sarim Food Shop
        } else if (npcId == 1027) { // Gerrant
            client.WanneShop = 18; // Port Sarim Fishing Shop
        } else if (npcId == 559) { // Brian
            client.WanneShop = 19; // Port Sarim Battleaxe Shop
        } else if (npcId == 556) { // Grum
            client.WanneShop = 20; // Port Sarim Jewelery Shop
        } else if (npcId == 583) { // Betty
            client.WanneShop = 21; // Port Sarim Magic Shop
        } else if (npcId == 520 || npcId == 521) { // Shop Keeper +
            // Assistant
            client.WanneShop = 22; // Lumbridge General Store
        } else if (npcId == 519) { // Bob

            client.WanneShop = 23; // Lumbridge Axe Shop
        } else if (npcId == 541) { // Zeke

            client.WanneShop = 24; // Al-Kharid Scimitar Shop
        } else if (npcId == 545) { // Dommik

            client.WanneShop = 25; // Al-Kharid Crafting Shop
        } else if (npcId == 524 || npcId == 525) { // Shop Keeper +
            // Assistant

            client.WanneShop = 26; // Al-Kharid General Store
        } else if (npcId == 542) { // Louie Legs

            client.WanneShop = 27; // Al-Kharid Legs Shop
        } else if (npcId == 544) { // Ranael

            client.WanneShop = 28; // Al-Kharid Skirt Shop
        } else if (npcId == 2621) { // Hur-Koz

            client.WanneShop = 29; // TzHaar Shop Weapons,Amour
        } else if (npcId == 2622) { // Hur-Lek

            client.WanneShop = 30; // TzHaar Shop Runes
        } else if (npcId == 2620) { // Hur-Tel

            client.WanneShop = 31; // TzHaar Shop General Store
        } else if (npcId == 692) { // Throwing shop

            client.WanneShop = 32; // Authentic Throwing Weapons
        } else if (npcId == 6060) { // Bow and arrows
            client.WanneShop = 11;
        } else if (npcId == 6059) { // Archer's Armour

            client.WanneShop = 34; // Aaron's Archery Appendages
        } else if (npcId == 537) { // Scavvo

            client.WanneShop = 35; // Champion's Rune shop
        } else if (npcId == 536) { // Valaine

            client.WanneShop = 36; // Champion's guild shop
        } else if (npcId == 933) { // Legend's Shop

            client.WanneShop = 37; // Legend's Shop
        } else if (npcId == 932) { // Legends General Store

            client.WanneShop = 38; // Legend's Gen. Store
        } else if (npcId == 5809) {

            client.WanneShop = 25; // Crafting shop
        } else if (npcId == 402 || npcId == 403 || npcId == 405) {
            client.NpcWanneTalk = 13;
        } else {
            client.println_debug("atNPC 2: " + npcId);
        }
    }

}
