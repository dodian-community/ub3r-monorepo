package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dodian.uber.game.combat.PlayerAttackCombatKt;

/**
 * Opcode 73 – player attacking another player.
 */
public class AttackPlayerListener implements PacketListener {

    static { PacketListenerManager.register(73, new AttackPlayerListener()); }

    private static final Logger logger = LoggerFactory.getLogger(AttackPlayerListener.class);

    private static int readSignedWordBigEndian(ByteBuf buf) {
        int low = buf.readUnsignedByte();
        int high = buf.readUnsignedByte();
        int value = (high << 8) | low;
        if (value > 32767) value -= 65536;
        return value;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        int victimSlot = readSignedWordBigEndian(packet.getPayload());

        if (logger.isTraceEnabled()) {
            logger.trace("AttackPlayer from={} victimSlot={}", client.getPlayerName(), victimSlot);
        }

        if (client.deathStage >= 1) return;

        Client plr = Server.playerHandler.getClient(victimSlot);
        if (plr == null) return;
        if (client.randomed || client.UsingAgility) return;

        boolean rangedAttack = PlayerAttackCombatKt.getAttackStyle(client) != 0;
        if ((rangedAttack && client.goodDistanceEntity(plr, 5)) || client.goodDistanceEntity(plr, 1)) {
            client.resetWalkingQueue();
            client.startAttack(plr);
            return;
        }

        // Need to walk closer first
        WalkToTask task = new WalkToTask(WalkToTask.Action.ATTACK_PLAYER, victimSlot, plr.getPosition());
        client.setWalkToTask(task);
        EventManager.getInstance().registerEvent(new Event(600) {
            @Override
            public void execute() {
                if (client.disconnected || client.getWalkToTask() != task) { stop(); return; }
                if ((PlayerAttackCombatKt.getAttackStyle(client) != 0 && client.goodDistanceEntity(plr, 5)) || client.goodDistanceEntity(plr, 1)) {
                    client.resetWalkingQueue();
                    client.startAttack(plr);
                    client.setWalkToTask(null);
                    stop();
                }
            }
        });
    }
}
