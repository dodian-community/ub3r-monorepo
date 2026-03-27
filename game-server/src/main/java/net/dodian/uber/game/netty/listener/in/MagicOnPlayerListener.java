package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.systems.combat.CombatIntent;
import net.dodian.uber.game.systems.combat.CombatStartService;
import net.dodian.uber.game.systems.world.player.PlayerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty implementation of MagicOnPlayer (opcode 249) incoming packet.
 * Decodes packet fields using the same byte order/value transforms as the
 * legacy Stream-based handler and executes the same gameplay logic in-place.
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
        client.magicId = magicId;

        logger.debug("MagicOnPlayerListener: victim {} spell {}", victimIndex, magicId);

        if (client.deathStage >= 1) {
            return;
        }

        Client victim = PlayerRegistry.getClient(victimIndex);
        if (victim == null) {
            return;
        }

        if (client.randomed || client.UsingAgility) {
            return;
        }

        CombatStartService.startPlayerAttack(client, victim, CombatIntent.MAGIC_ON_PLAYER);
    }
}
