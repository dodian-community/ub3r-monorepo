package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.engine.systems.net.PacketMagicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty implementation of MagicOnPlayer (opcode 249) incoming packet.
 * Decodes packet fields then delegates to PacketMagicService.
 */
public class MagicOnPlayerListener implements PacketListener {

    static {
        PacketListenerManager.register(249, new MagicOnPlayerListener());
    }

    private static final Logger logger = LoggerFactory.getLogger(MagicOnPlayerListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < 4) {
            return;
        }

        int victimIndex = ByteBufReader.readShortSigned(buf, ByteOrder.BIG, ValueType.ADD);
        int magicId = ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);

        logger.debug("MagicOnPlayerListener: victim {} spell {}", victimIndex, magicId);

        PacketMagicService.handleMagicOnPlayer(client, packet.opcode(), victimIndex, magicId);
    }
}
