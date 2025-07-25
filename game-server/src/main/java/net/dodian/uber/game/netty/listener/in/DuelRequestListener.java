package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.utilities.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty port of DuelRequest (opcode 153).
 */
public class DuelRequestListener implements PacketListener {

    static { PacketListenerManager.register(153, new DuelRequestListener()); }

    private static final Logger logger = LoggerFactory.getLogger(DuelRequestListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();
        int size = packet.getSize();
        byte[] data = new byte[size];
        buf.readBytes(data);
        int pid = Utils.HexToInt(data, 0, size) / 1000;

        Client other = client.getClient(pid);
        if (!client.validClient(pid) || client.getSlot() == pid) {
            return;
        }
        if (client.inWildy() || other.inWildy()) {
            client.send(new SendMessage("You cant duel in the wilderness!"));
            return;
        }
        if (client.isBusy() || other.isBusy()) {
            client.send(new SendMessage(client.isBusy() ? "You are currently busy" : other.getPlayerName() + " is currently busy!"));
            return;
        }

        logger.debug("{} sent duel request to {} (slot {})", client.getPlayerName(), other.getPlayerName(), pid);
        client.duelReq(pid);
    }
}
