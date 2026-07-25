package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.engine.systems.net.PacketItemActionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@net.dodian.uber.game.netty.listener.PacketHandler(opcodes = {41})
public class WearItemListener implements PacketListener {
    private static final Logger logger = LoggerFactory.getLogger(WearItemListener.class);
    private static final int MIN_PAYLOAD_BYTES = 6;

    @Override
    public void handle(Client client, GamePacket packet) {
        net.dodian.uber.game.netty.game.decode.TarnishPackets.WearItem msg =
                net.dodian.uber.game.netty.game.decode.TarnishPackets.WearItem.decode(packet.payload());
        if (msg == null) {
            return;
        }

        if (net.dodian.uber.game.engine.config.DotEnvKt.getGameWorldId() == 2) {
            logger.debug("[WEAR:PACKET] player={} item={} slot={} interface={}",
                    client.getPlayerName(), msg.itemId(), msg.slot(), msg.interfaceId());
        }

        PacketItemActionService.handleWear(client, msg.itemId(), msg.slot(), msg.interfaceId());
    }
}
