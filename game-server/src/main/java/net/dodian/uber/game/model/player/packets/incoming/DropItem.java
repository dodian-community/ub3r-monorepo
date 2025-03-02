package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.content.Skillcape;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;

import static net.dodian.utilities.DotEnvKt.getGameWorldId;

public class DropItem implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int droppedItem, slot;
        droppedItem = client.getInputStream().readSignedWordA();
        client.getInputStream().readUnsignedWordA(); //Interface id!
        slot = client.getInputStream().readUnsignedWordA();
        if (client.randomed || client.UsingAgility) {
            return;
        }
        if(slot > 27 || slot < 0) { //No need to go out of scope!
            client.disconnected = true; //Do we need a disconnect here?!
            return;
        }
        if((client.playerItems[slot] - 1 != droppedItem || client.playerItems[slot] - 1 < 0) || client.playerItemsN[slot] < 1) {
            return;
        } //Will this fix the dupe?!
        if (droppedItem == 5733) {
            client.deleteItem(droppedItem, slot, 1);
            client.send(new SendMessage("A magical force removed this item from your inventory!"));
            return;
        }
        boolean isHood = Server.itemManager.getName(droppedItem).contains("hood");
        Skillcape skillcape = Skillcape.getSkillCape(isHood ? droppedItem - 1 : droppedItem);
        if (skillcape != null) { //Cant drop the skillcape or skillcape hood!
            client.send(new SendMessage("I might be skillful, but dropping this gain no skill!"));
            return;
        }
        boolean maxCheck = client.GetItemName(droppedItem).contains(("Max cape")) || client.GetItemName(droppedItem).contains(("Max hood"));
        if (maxCheck) { //Cant drop the max cape or hood!
            client.send(new SendMessage("I might be skillful, but dropping this gain no skill!"));
            return;
        }
        if (!client.wearing) {
            client.dropItem(droppedItem, slot);
        }
    }

}
