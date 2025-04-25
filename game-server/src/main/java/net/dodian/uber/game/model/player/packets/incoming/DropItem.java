package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.content.Skillcape;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;

public class DropItem implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int droppedItem = client.getInputStream().readSignedWordA();
        client.getInputStream().readUnsignedWordA(); // Interface id - We don't use this value currently
        int slot = client.getInputStream().readUnsignedWordA();

        // Prevent actions if logging out, doing agility, or in a random event
        if (client.isLoggingOut || client.randomed || client.UsingAgility) {
            return;
        }

        // Rate limit dropping to once every 600ms
        long currentTime = System.currentTimeMillis();
        if (currentTime - client.lastDropTime < 600) {
             client.send(new SendMessage("You must wait a moment before dropping another item."));
            return;
        }

        // Validate slot number
        if (slot > 27 || slot < 0) {

          // client.disconnected = true; // I don't think this is very useful as it returns so why dc?
            System.out.println("Warning: Player " + client.getPlayerName() + " sent invalid drop slot: " + slot);
            return;
        }

        // Verify the item exists at the specified slot and matches the client's request
        if ((client.playerItems[slot] - 1 != droppedItem) || client.playerItemsN[slot] < 1) {
            // Item mismatch or empty slot, likely sync issue or manipulation.
            return;
        }

        // Special handling for specific items (like potato)
        if (droppedItem == 5733) {
            client.deleteItem(droppedItem, slot, 1);
            client.send(new SendMessage("A magical force removed this item from your inventory!"));
            client.lastDropTime = currentTime; // Update time even for special drops
            return;
        }

        // Prevent dropping skillcapes/hoods
        boolean isHood = Server.itemManager.getName(droppedItem).contains("hood");
        Skillcape skillcape = Skillcape.getSkillCape(isHood ? droppedItem - 1 : droppedItem); // Adjust ID check for hoods if necessary
        if (skillcape != null) {
            client.send(new SendMessage("You cannot drop this valuable cape!"));
            return;
        }

        // Prevent dropping max capes/hoods
        boolean maxCheck = client.GetItemName(droppedItem).contains(("Max cape")) || client.GetItemName(droppedItem).contains(("Max hood"));
        if (maxCheck) {
            client.send(new SendMessage("This cape represents mastery; you shouldn't drop it!"));
            return;
        }

        // If not currently equipping an item (prevents conflicts), proceed to drop
        if (!client.wearing) {
            client.dropItem(droppedItem, slot);
            client.lastDropTime = currentTime; // Update timestamp after successful drop
        }
    }
}
