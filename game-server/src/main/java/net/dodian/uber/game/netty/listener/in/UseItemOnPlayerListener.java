package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Constants;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.utilities.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opcode 14 – Use item on player (e.g., party-cracker).
 */
public class UseItemOnPlayerListener implements PacketListener {

    static { PacketListenerManager.register(14, new UseItemOnPlayerListener()); }

    private static final Logger logger = LoggerFactory.getLogger(UseItemOnPlayerListener.class);

    private static int readSignedWord(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = buf.readUnsignedByte();
        int value = (high << 8) | low;
        if (value > 32767) value -= 65536;
        return value;
    }

    private static int readSignedWordBigEndian(ByteBuf buf) {
        int low = buf.readUnsignedByte();
        int high = buf.readUnsignedByte();
        int value = (high << 8) | low;
        if (value > 32767) value -= 65536;
        return value;
    }

    private static int readSignedWordBigEndianA(ByteBuf buf) {
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        int high = buf.readUnsignedByte();
        int value = (high << 8) | low;
        if (value > 32767) value -= 65536;
        return value;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();
        readSignedWordBigEndianA(buf); // unused index? matches legacy discard
        int playerSlot = readSignedWord(buf);
        int itemId = readSignedWord(buf);
        int crackerSlot = readSignedWordBigEndian(buf);

        Client player = (playerSlot >= 0 && playerSlot < Constants.maxPlayers) ? ((Client) PlayerHandler.players[playerSlot]) : null;
        if (player == null || !client.playerHasItem(itemId)) return;
        if (client.randomed || client.UsingAgility) return;

        if (logger.isTraceEnabled()) {
            logger.trace("UseItemOnPlayer item={} on playerSlot={} crackerSlot={} from={}", itemId, playerSlot, crackerSlot, client.getPlayerName());
        }

        if (itemId == 5733) { // Potato – internal mini-game setup
            client.playerPotato.clear();
            client.playerPotato.add(0, 1);
            client.playerPotato.add(1, playerSlot);
            client.playerPotato.add(2, player.dbId);
            client.playerPotato.add(3, 1);
            return;
        }

        if (itemId == 962) { // Christmas cracker
            if (player.freeSlots() <= 0) {
                client.send(new SendMessage("Your partner need a slot free in their inventory!"));
                return;
            }
            if (client.connectedFrom.equals(player.connectedFrom)) {
                client.send(new SendMessage("Can't use it on another player from same address!"));
                return;
            }
            client.deleteItem(itemId, crackerSlot, 1);
            int[] hats = {1038, 1040, 1042, 1044, 1046, 1048};
            int partyHat = hats[Misc.random(hats.length - 1)];
            if (Misc.random(99) < 50) {
                client.addItemSlot(partyHat, 1, crackerSlot);
                client.send(new SendMessage("You got a " + client.GetItemName(partyHat).toLowerCase() + " from the cracker!"));
            } else {
                player.addItem(partyHat, 1);
                client.checkItemUpdate();
                player.send(new SendMessage("You got a " + client.GetItemName(partyHat).toLowerCase() + " from " + client.getPlayerName()));
                client.send(new SendMessage(player.getPlayerName() + " got a  " + client.GetItemName(partyHat).toLowerCase() + " from the cracker!"));
            }
        }
    }
}
