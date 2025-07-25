package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.model.player.skills.slayer.SlayerTask;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.utilities.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inventory second-click (opcode 16) – mirrors legacy {@code ClickItem2}.
 */
public class ClickItem2Listener implements PacketListener {

    static { PacketListenerManager.register(16, new ClickItem2Listener()); }

    private static final Logger logger = LoggerFactory.getLogger(ClickItem2Listener.class);

    // --- helpers matching Stream ---
    private static int readUnsignedWordA(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        return (high << 8) | low;
    }
    private static int readSignedWordBigEndianA(ByteBuf buf) {
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        int high = buf.readUnsignedByte();
        int val = (high << 8) | low;
        if (val > 32767) val -= 65536;
        return val;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();

        int itemId   = readUnsignedWordA(buf);
        int itemSlot = readSignedWordBigEndianA(buf);

        logger.debug("ClickItem2Listener: slot {} item {}", itemSlot, itemId);

        if (itemSlot < 0 || itemSlot > 28) { client.disconnected = true; return; }
        if (client.playerItems[itemSlot] - 1 != itemId) return;
        if (client.randomed || client.UsingAgility) return;

        String itemName = client.GetItemName(itemId);

        /* Rune pouches – show stored ess count */
        int slot = itemId == 5509 ? 0 : ((itemId - 5508) / 2);
        if (slot >= 0 && slot <= 3) {
            client.send(new SendMessage("There is " + client.runePouchesAmount[slot] + " rune essence in this pouch!"));
        }

        /* Spunky item compliments */
        if (itemId == 13203) {
            String[] quotes = {
                    "You are easily the spunkiest warrior alive!",
                    "Not a single soul can challenge your spunk!",
                    "You are clearly the most spunktastic in all the land!",
                    "Your might is spunktacular!",
                    "It's spunkalicious!",
                    "You... are... spunky!",
                    "You are too spunktacular to measure!",
                    "You are the real M.V.P. dude!",
                    "More lazier then Spunky is Ivan :D"
            };
            client.send(new SendMessage(quotes[Misc.random(quotes.length - 1)]));
        }

        /* Slayer helm task reminder */
        if (itemName.startsWith("Slayer helm")) {
            SlayerTask.sendTask(client);
        }

        /* Slayer gem partner message */
        if (itemId == 4155) {
            client.NpcDialogue = 16;
            client.NpcDialogueSend = false;
            client.nextDiag = -1;
        }

        /* Misc info messages */
        if (itemId == 11997) {
            client.send(new SendMessage("Event is over! Will use in the future?!"));
        }
        if (itemId == 4936) {
            client.send(new SendMessage("This crossbow need a Seercull bow to be fully repaired."));
        }
        if (itemId == 4864) {
            client.send(new SendMessage("This staff need a Master wand to be fully repaired."));
        }

        /* Rubber chicken emote */
        if (itemId == 4566) {
            client.requestAnim(1835, 0);
        }
    }
}
