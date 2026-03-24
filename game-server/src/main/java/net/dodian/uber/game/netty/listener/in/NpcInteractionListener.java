package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.combat.PlayerAttackCombatKt;
import net.dodian.uber.game.content.npc.NpcInteractionService;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.skills.fishing.FishingNpcInteractionService;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.runtime.interaction.NpcInteractionIntent;
import net.dodian.uber.game.runtime.interaction.scheduler.InteractionTaskScheduler;
import net.dodian.uber.game.runtime.interaction.scheduler.NpcInteractionTask;
import net.dodian.uber.game.runtime.combat.CombatIntent;
import net.dodian.uber.game.runtime.combat.CombatStartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consolidated Netty handler for npc interaction opcodes:
 * 155 (click1), 17 (click2), 21 (click3), 18 (click4), 72 (attack).
 */
@PacketHandler(opcode = 155)
public class NpcInteractionListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(NpcInteractionListener.class);

    static {
        NpcInteractionListener listener = new NpcInteractionListener();
        PacketListenerManager.register(155, listener);
        PacketListenerManager.register(17, listener);
        PacketListenerManager.register(21, listener);
        PacketListenerManager.register(18, listener);
        PacketListenerManager.register(72, listener);
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        switch (packet.opcode()) {
            case 155:
                handleNpcClick1(client, packet);
                return;
            case 17:
                handleNpcClick2(client, packet);
                return;
            case 21:
                handleNpcClick3(client, packet);
                return;
            case 18:
                handleNpcClick4(client, packet);
                return;
            case 72:
                handleNpcAttack(client, packet);
                return;
            default:
                logger.warn("NpcInteractionListener got unexpected opcode={} for player={}", packet.opcode(), client.getPlayerName());
        }
    }

    private void handleNpcClick1(Client client, GamePacket packet) {
        ByteBuf payload = packet.payload();
        if (payload.readableBytes() < 2) {
            return;
        }
        int npcIndex = ByteBufReader.readShortSigned(payload, ByteOrder.LITTLE, ValueType.NORMAL);
        Npc tempNpc = Server.npcManager.getNpc(npcIndex);
        if (tempNpc == null) {
            return;
        }

        if (client.randomed || client.UsingAgility) {
            return;
        }
        if (!client.playerPotato.isEmpty()) {
            client.playerPotato.clear();
        }

        // OpenRune-style: tick-owned routing. The game thread will execute when in range.
        NpcInteractionIntent intent = new NpcInteractionIntent(packet.opcode(), PlayerHandler.cycle, npcIndex, 1);
        InteractionTaskScheduler.schedule(client, intent, new NpcInteractionTask(client, intent));
    }

    private void performNpcClick1(Client client, Npc tempNpc) {
        if (!tempNpc.isAlive()) {
            client.send(new SendMessage("That monster has been killed!"));
            return;
        }

        int npcId = tempNpc.getId();
        client.resetAction();
        client.faceNpc(tempNpc.getSlot());
        client.setInteractionAnchor(tempNpc.getPosition().getX(), tempNpc.getPosition().getY(), tempNpc.getPosition().getZ());

        if (FishingNpcInteractionService.handleNpcOption(client, npcId, 1)) {
            return;
        }

        if (NpcInteractionService.tryHandleClick(client, 1, tempNpc)) {
            return;
        }
        logger.debug("Unhandled NPC first-click fallback npcId={} player={}", npcId, client.getPlayerName());
    }

    private void handleNpcClick2(Client client, GamePacket packet) {
        ByteBuf payload = packet.payload();
        if (payload.readableBytes() < 2) {
            return;
        }
        int npcIndex = ByteBufReader.readShortSigned(payload, ByteOrder.LITTLE, ValueType.ADD);
        Npc tempNpc = Server.npcManager.getNpc(npcIndex);
        if (tempNpc == null) {
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Npc click2 opcode={} npcIndex={} npcId={} player={}", packet.opcode(), npcIndex, tempNpc.getId(), client.getPlayerName());
        }

        if (client.randomed || client.UsingAgility) {
            return;
        }
        if (!client.playerPotato.isEmpty()) {
            client.playerPotato.clear();
        }

        NpcInteractionIntent intent = new NpcInteractionIntent(packet.opcode(), PlayerHandler.cycle, npcIndex, 2);
        InteractionTaskScheduler.schedule(client, intent, new NpcInteractionTask(client, intent));
    }

    private void performNpcClick2(Client client, Npc tempNpc) {
        if (!tempNpc.isAlive()) {
            client.send(new SendMessage("That monster has been killed!"));
            return;
        }

        int npcId = tempNpc.getId();
        client.resetAction();
        client.faceNpc(tempNpc.getSlot());
        client.setInteractionAnchor(tempNpc.getPosition().getX(), tempNpc.getPosition().getY(), tempNpc.getPosition().getZ());
        if (FishingNpcInteractionService.handleNpcOption(client, npcId, 2)) {
            return;
        }

        if (NpcInteractionService.tryHandleClick(client, 2, tempNpc)) {
            return;
        }

        logger.debug("Unhandled NPC second-click fallback npcId={} player={}", npcId, client.getPlayerName());
    }

    private void handleNpcClick3(Client client, GamePacket packet) {
        ByteBuf payload = packet.payload();
        if (payload.readableBytes() < 2) {
            return;
        }
        int npcIndex = ByteBufReader.readShortSigned(payload, ByteOrder.BIG, ValueType.NORMAL);
        Npc tempNpc = Server.npcManager.getNpc(npcIndex);
        if (tempNpc == null) {
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Npc click3 opcode={} npcIndex={} npcId={} player={}", packet.opcode(), npcIndex, tempNpc.getId(), client.getPlayerName());
        }

        if (client.randomed || client.UsingAgility) {
            return;
        }
        if (!client.playerPotato.isEmpty()) {
            client.playerPotato.clear();
        }

        NpcInteractionIntent intent = new NpcInteractionIntent(packet.opcode(), PlayerHandler.cycle, npcIndex, 3);
        InteractionTaskScheduler.schedule(client, intent, new NpcInteractionTask(client, intent));
    }

    private void performNpcClick3(Client client, Npc tempNpc) {
        if (client.isBusy()) {
            return;
        }

        int npcId = tempNpc.getId();
        client.resetAction();
        client.faceNpc(tempNpc.getSlot());
        client.setInteractionAnchor(tempNpc.getPosition().getX(), tempNpc.getPosition().getY(), tempNpc.getPosition().getZ());

        if (NpcInteractionService.tryHandleClick(client, 3, tempNpc)) {
            return;
        }

        logger.debug("Unhandled NPC third-click fallback npcId={} player={}", npcId, client.getPlayerName());
    }

    private void handleNpcClick4(Client client, GamePacket packet) {
        ByteBuf payload = packet.payload();
        if (payload.readableBytes() < 2) {
            return;
        }
        int npcIndex = ByteBufReader.readShortSigned(payload, ByteOrder.LITTLE, ValueType.NORMAL);
        Npc tempNpc = Server.npcManager.getNpc(npcIndex);
        if (tempNpc == null) {
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Npc click4 opcode={} npcIndex={} npcId={} player={}", packet.opcode(), npcIndex, tempNpc.getId(), client.getPlayerName());
        }

        if (client.randomed || client.UsingAgility) {
            return;
        }
        if (!client.playerPotato.isEmpty()) {
            client.playerPotato.clear();
        }

        NpcInteractionIntent intent = new NpcInteractionIntent(packet.opcode(), PlayerHandler.cycle, npcIndex, 4);
        InteractionTaskScheduler.schedule(client, intent, new NpcInteractionTask(client, intent));
    }

    private void performNpcClick4(Client client, Npc tempNpc) {
        if (client.isBusy()) {
            return;
        }

        int npcId = tempNpc.getId();
        client.setInteractionAnchor(tempNpc.getPosition().getX(), tempNpc.getPosition().getY(), tempNpc.getPosition().getZ());

        if (NpcInteractionService.tryHandleClick(client, 4, tempNpc)) {
            return;
        }

        logger.debug("Unhandled NPC fourth-click fallback npcId={} player={}", npcId, client.getPlayerName());
    }

    private void handleNpcAttack(Client client, GamePacket packet) {
        ByteBuf payload = packet.payload();
        if (payload.readableBytes() < 2) {
            return;
        }
        int npcIndex = ByteBufReader.readShortUnsigned(payload, ByteOrder.BIG, ValueType.ADD);

        logger.debug("Npc attack opcode={} npcIndex={} player={}", packet.opcode(), npcIndex, client.getPlayerName());
        if (client.magicId >= 0) {
            client.magicId = -1;
        }
        if (client.deathStage >= 1) {
            return;
        }

        Npc npc = Server.npcManager.getNpc(npcIndex);
        if (npc == null) {
            return;
        }
        if (client.randomed || client.UsingAgility) {
            return;
        }
        if (NpcInteractionService.tryHandleAttack(client, npc)) {
            return;
        }

        boolean rangedAttack = PlayerAttackCombatKt.getAttackStyle(client) != 0;
        if ((rangedAttack && client.goodDistanceEntity(npc, 5)) || client.goodDistanceEntity(npc, 1)) {
            CombatStartService.startNpcAttack(client, npc, CombatIntent.ATTACK_NPC);
            return;
        }

        NpcInteractionIntent intent = new NpcInteractionIntent(packet.opcode(), PlayerHandler.cycle, npcIndex, 5);
        InteractionTaskScheduler.schedule(client, intent, new NpcInteractionTask(client, intent));
    }

}
