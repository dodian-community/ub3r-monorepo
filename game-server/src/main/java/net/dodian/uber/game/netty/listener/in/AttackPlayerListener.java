package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.GameEventScheduler;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.engine.scheduler.QueueTask;
import net.dodian.uber.game.engine.scheduler.QueueTaskService;
import net.dodian.uber.game.systems.combat.CombatIntent;
import net.dodian.uber.game.systems.combat.CombatStartService;
import net.dodian.uber.game.systems.world.player.PlayerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dodian.uber.game.systems.combat.PlayerAttackCombatKt;

/**
 * Opcode 73 – player attacking another player.
 */
public class AttackPlayerListener implements PacketListener {

    static { PacketListenerManager.register(73, new AttackPlayerListener()); }

    private static final Logger logger = LoggerFactory.getLogger(AttackPlayerListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < 2) {
            return;
        }
        int victimSlot = ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);

        if (logger.isTraceEnabled()) {
            logger.trace("AttackPlayer from={} victimSlot={}", client.getPlayerName(), victimSlot);
        }

        if (client.deathStage >= 1) return;

        Client plr = PlayerRegistry.getClient(victimSlot);
        if (plr == null) return;
        if (client.randomed || client.UsingAgility) return;

        boolean rangedAttack = PlayerAttackCombatKt.getAttackStyle(client) != 0;
        CombatStartService.startPlayerAttack(client, plr, CombatIntent.ATTACK_PLAYER);
    }
}
