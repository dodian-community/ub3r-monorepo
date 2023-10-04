package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;

public class UseItemOnNpc implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int item = client.getInputStream().readUnsignedWordA();
        int npcIndex = client.getInputStream().readUnsignedWordA();
        int slot = client.getInputStream().readUnsignedWordBigEndian();
        client.getInputStream().readUnsignedWordA();
        if (item != client.playerItems[slot] - 1) {
            return;
        }
        Npc tempNpc = Server.npcManager.getNpc(npcIndex);
        if (tempNpc == null) {
            return;
        }
        int npcId = tempNpc.getId();
        client.faceNpc(npcIndex);
        if (item == 5733) {
            client.playerPotato.clear();
            client.playerPotato.add(0, 2);
            client.playerPotato.add(1, npcIndex);
            client.playerPotato.add(2, npcId);
            client.playerPotato.add(3, 1);
            client.showPlayerOption(new String[]{
                    "What do you wish to do?", "Remove spawn", "Check drops", "Reload drops", "Check config", "Reload config!"});
            client.NpcDialogueSend = true;
            return;
        }
        if (item == 4155) {
            tempNpc.showGemConfig(client);
            return;
        }
        if(npcId == 535 && (item == 1540 || item == 11286)) {
            if (item == 1540 && !client.playerHasItem(11286)) client.showNPCChat(npcId, 596, new String[]{"You need a draconic visage!"});
            else if (item == 11286 && !client.playerHasItem(1540)) client.showNPCChat(npcId, 596, new String[]{"You need a anti-dragon shield!"});
            else if (!client.playerHasItem(995, 1500000)) client.showNPCChat(npcId, 596, new String[]{"You need 1.5 million coins!"});
            else {
                client.deleteItem(item, slot, 1);
                client.deleteItem(item == 1540 ? 11286 : 1540, 1);
                client.deleteItem(995, 1500000);
                client.addItemSlot(11284, 1, slot);
                client.showNPCChat(npcId, 591, new String[]{"Here you go.", "Your shield is done."});
            }
        }

        if (npcId == 2794) {
            if (item == 1735) {
                client.addItem(1737, 1);
            } else {
                client.send(new SendMessage("You need some shears to shear this sheep!"));
            }
            return;
        }
    }

}
