package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.content.npcs.attack.NpcAttackDispatcher;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty handler for opcode 72 (attack NPC).
 * Full port of legacy {@code AttackNpc.ProcessPacket}.
 */
public class AttackNpcListener implements PacketListener {

    static { PacketListenerManager.register(72, new AttackNpcListener()); }

    private static final Logger logger = LoggerFactory.getLogger(AttackNpcListener.class);

    // Stream helper matching readUnsignedWordA()
    private static int readUnsignedWordA(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        return (high << 8) | low;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();
        int npcIndex = readUnsignedWordA(buf);

        logger.debug("AttackNpcListener: npcIndex {}", npcIndex);
        NpcAttackDispatcher.handle(client, npcIndex);
    }
}
