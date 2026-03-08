package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.GameEventScheduler;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.runtime.combat.CombatIntent;
import net.dodian.uber.game.runtime.combat.CombatStartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty implementation of legacy {@code MagicOnNpc} (opcode 131).
 * Decoding pattern: LESHORTA (npc index), SHORTA (spell id).
 */
public class MagicOnNpcListener implements PacketListener {

    static { PacketListenerManager.register(131, new MagicOnNpcListener()); }

    private static final Logger logger = LoggerFactory.getLogger(MagicOnNpcListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < 4) { // 2 + 2
            return;
        }

        int npcIndex = ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.ADD);
        int magicId = ByteBufReader.readShortSigned(buf, ByteOrder.BIG, ValueType.ADD);
        client.magicId = magicId;

        if (client.deathStage >= 1) return;

        Npc npc = Server.npcManager.getNpc(npcIndex);
        if (npc == null) return;
        if (client.randomed || client.UsingAgility) return;

        CombatStartService.startNpcAttack(client, npc, CombatIntent.MAGIC_ON_NPC);

        logger.debug("MagicOnNpcListener: magic {} on npc {}", magicId, npcIndex);
    }
}
