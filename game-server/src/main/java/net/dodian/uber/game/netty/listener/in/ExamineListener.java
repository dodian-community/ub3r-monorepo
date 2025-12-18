package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.dodian.utilities.DotEnvKt.getGameWorldId;

@PacketHandler(opcode = 2)
public final class ExamineListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(ExamineListener.class);

    static {
        PacketListenerManager.register(2, new ExamineListener());
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        try {
            ByteBuf buf = packet.getPayload();

            if (packet.getSize() < 5 || buf.readableBytes() < 5) { //Not sure what correct size!
                logger.warn("ExamineItem packet too small (size={}) from {}", packet.getSize(), client.getPlayerName());
                return;
            }

            int slot = buf.readUnsignedShort();
            int posX = buf.readInt();
            int ID = buf.readShort();
            int posY = buf.readShort();
            if (getGameWorldId() > 1) {
                logger.debug("Examine: Slot={}, Id={}, posX={}, posY={}", slot, ID, posX, posY);
            }

            if(slot == 0) { //Item Examine
                client.examineItem(client, ID, posX);
            } else if (slot == 1) { //Npc Examine
                client.examineNpc(client, ID);
            } else if (slot == 2) { //Object Examine
                client.examineObject(client, ID, new Position(posX, posY, client.getPosition().getZ()));
            }
        } catch (Exception e) {
            logger.error("Error handling Examine packet for {}", client.getPlayerName(), e);
        }
    }
}
