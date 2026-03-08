package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.content.dialogue.DialogueService;
import net.dodian.uber.game.content.dialogue.text.DialoguePagingService;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.runtime.action.PlayerActionCancellationService;
import net.dodian.uber.game.runtime.action.PlayerActionCancelReason;
import net.dodian.uber.game.runtime.combat.CombatCancellationReason;
import net.dodian.uber.game.runtime.combat.CombatRuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicLong;
import static net.dodian.utilities.DotEnvKt.getOpcode248HasExtra14ByteSuffix;



/**
 * Netty implementation of the walking packet handler (opcodes 248, 164, 98).
 * Closely mirrors legacy logic in model/player/packets/incoming/Walking.java.
 */
@net.dodian.uber.game.netty.listener.PacketHandler(opcode = 248)
public final class WalkingListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(WalkingListener.class);
    private static final int OP_MINIMAP_WALK = 248;
    private static final int MIN_WALK_PACKET_SIZE = 5;
    private static final int MINIMAP_TRAILING_BYTES = 14;
    private static final int MIN_WORLD_COORD = 0;
    private static final int MAX_WORLD_COORD = 16382;
    private static final long MALFORMED_LOG_INTERVAL_MS = 5000L;
    private static final AtomicLong lastMalformedLogMs = new AtomicLong(0L);

    static {
        WalkingListener handler = new WalkingListener();
        safeRegister(248, handler);
        safeRegister(164, handler);
        safeRegister(98, handler);
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        int opcode = packet.opcode();
        int size   = packet.size();
        long now   = System.currentTimeMillis();

        if (client.deathStage > 0 || client.getCurrentHealth() < 1 || client.randomed || !client.validClient
                || !client.pLoaded || now < client.walkBlock) {
            return;
        }
        if (client.doingTeleport() || client.getPlunder.looting) return;
        if (client.isVerticalTransitionActive()) {
            client.resetWalkingQueue();
            return;
        }
        if (opcode != 98) client.setWalkToTask(null);

        // Auto-decline trade/duel when walking
        if (client.inTrade && (opcode == 164 || opcode == 248)) client.declineTrade();
        else if (client.inDuel && !client.duelFight && (opcode == 164 || opcode == 248)) client.declineDuel();

        if (client.genie) client.genie = false;
        if (client.antique) client.antique = false;
        client.playerPotato.clear();
        client.farming.updateCompost(client); //Need to update the closest compostBin!
        client.farming.updateFarmPatch(client); //Need to update the closest farmingPatch!

        if ((client.getStunTimer() > 0 || client.getSnareTimer() > 0) && opcode != 98) {
            client.send(new SendMessage(client.getSnareTimer() > 0 ? "You are ensnared!" : "You are currently stunned!"));
            client.resetWalkingQueue();
            return;
        }

        if (client.morph) client.unMorph();
        if (client.checkInv) {
            client.checkInv = false;
            client.resetItems(3214);
        }
        if (client.pickupWanted) {
            client.pickupWanted = false;
            client.attemptGround = null;
        }

        ByteBuf buf = packet.payload();
        int effectiveSize = resolveEffectiveSize(opcode, size);
        if (effectiveSize < MIN_WALK_PACKET_SIZE) {
            rejectMalformedWalkPacket(client, opcode, size, -1, -1, "effective size below minimum");
            return;
        }
        if (buf.readableBytes() < effectiveSize) {
            rejectMalformedWalkPacket(client, opcode, size, -1, -1, "payload shorter than effective size");
            return;
        }
        if (((effectiveSize - MIN_WALK_PACKET_SIZE) & 1) != 0) {
            rejectMalformedWalkPacket(client, opcode, size, -1, -1, "step payload has odd byte count");
            return;
        }
        if (client.mapRegionX < 0 || client.mapRegionY < 0) {
            rejectMalformedWalkPacket(client, opcode, size, -1, -1, "map region not initialized");
            return;
        }

        int steps = (effectiveSize - MIN_WALK_PACKET_SIZE) / 2;
        client.newWalkCmdSteps = steps;
        if (++client.newWalkCmdSteps > Player.WALKING_QUEUE_SIZE) {
            client.newWalkCmdSteps = 0;
            return;
        }

        int firstStepXAbs = ByteBufReader.readShort(buf, ValueType.ADD, ByteOrder.LITTLE);
        int firstStepX = firstStepXAbs - client.mapRegionX * 8;

        for (int i = 1; i < client.newWalkCmdSteps; i++) {
            client.newWalkCmdX[i] = ByteBufReader.readSignedByte(buf, ValueType.NORMAL);
            client.newWalkCmdY[i] = ByteBufReader.readSignedByte(buf, ValueType.NORMAL);
            client.tmpNWCX[i] = client.newWalkCmdX[i];
            client.tmpNWCY[i] = client.newWalkCmdY[i];
        }

        int firstStepYAbs = ByteBufReader.readShort(buf, ValueType.NORMAL, ByteOrder.LITTLE);
        int firstStepY = firstStepYAbs - client.mapRegionY * 8;
        if (!isValidWorldCoordinate(firstStepXAbs) || !isValidWorldCoordinate(firstStepYAbs)) {
            rejectMalformedWalkPacket(client, opcode, size, firstStepXAbs, firstStepYAbs, "first step out of world bounds");
            return;
        }
        client.newWalkCmdIsRunning = (ByteBufReader.readSignedByte(buf, ValueType.NEGATE) == 1);

        client.newWalkCmdX[0] = client.newWalkCmdY[0] = client.tmpNWCX[0] = client.tmpNWCY[0] = 0;

        logger.debug("Walk steps {} firstX {} firstY {} running {}", client.newWalkCmdSteps, firstStepX, firstStepY, client.newWalkCmdIsRunning);
        for (int i = 0; i < client.newWalkCmdSteps; i++) {
            client.newWalkCmdX[i] += firstStepX;
            client.newWalkCmdY[i] += firstStepY;
        }



        if (client.newWalkCmdSteps > 0) {
            // Any movement cancels active dialogue sessions and paging.
            DialogueService.closeBlockingDialogue(client, false);

            if (client.inDuel) {
                if (opcode != 98) client.send(new SendMessage("You cannot move during this duel!"));
                client.resetWalkingQueue();
                return;
            }
            // Reset interfaces/actions similar to legacy
            if (client.NpcWanneTalk > 0) {
                client.send(new RemoveInterfaces());
                client.NpcWanneTalk = -1;
            } else if (!client.isBusy()) {
                client.send(new RemoveInterfaces());
            }
            client.rerequestAnim();
            PlayerActionCancellationService.cancel(client, PlayerActionCancelReason.MOVEMENT, true, false, false, true);
            client.discord = false;
            client.playerSkillAction.clear();
            if (client.checkInv) { client.checkInv = false; client.resetItems(3214);}            
            if (opcode != 98 && CombatRuntimeService.hasActiveCombat(client)) {
                client.setCombatCancellationReason(CombatCancellationReason.MOVEMENT_INTERRUPTED);
                client.resetAttack();
            }
            client.faceTarget(65535);
        }

        if (client.chestEventOccur && opcode != 98) client.chestEventOccur = false;
        client.convoId = -1;
        if (DialogueService.hasBlockingDialogue(client)) {
            DialogueService.closeBlockingDialogue(client, true);
        }
        if (client.refundSlot != -1) client.refundSlot = -1;
        if (client.herbMaking != -1) client.herbMaking = -1;
        if (client.IsBanking) { client.IsBanking = false; client.send(new RemoveInterfaces()); client.checkItemUpdate(); }
        if (client.checkBankInterface) { client.checkBankInterface = false; client.send(new RemoveInterfaces()); client.checkItemUpdate(); }
        if (client.bankStyleViewOpen) { client.clearBankStyleView(); client.send(new RemoveInterfaces()); client.checkItemUpdate(); }
        if (client.isPartyInterface) { client.isPartyInterface = false; client.send(new RemoveInterfaces()); client.checkItemUpdate(); }
        if (client.isShopping()) { client.MyShopID = -1; client.send(new RemoveInterfaces()); client.checkItemUpdate(); }

        // done – movement queue will be processed in Client.process()
    }

    private static void safeRegister(int opcode, WalkingListener handler) {
        try {
            PacketListenerManager.register(opcode, handler);
        } catch (RuntimeException ex) {
            logger.debug("Skipping walking listener registration for opcode {}: {}", opcode, ex.getMessage());
        }
    }

    static int resolveEffectiveSize(int opcode, int packetSize) {
        return resolveEffectiveSize(opcode, packetSize, getOpcode248HasExtra14ByteSuffix());
    }

    static int resolveEffectiveSize(int opcode, int packetSize, boolean opcode248HasExtra14ByteSuffix) {
        if (opcode == OP_MINIMAP_WALK) {
            return opcode248HasExtra14ByteSuffix ? packetSize - MINIMAP_TRAILING_BYTES : packetSize;
        }
        return packetSize;
    }

    private static boolean isValidWorldCoordinate(int value) {
        return value >= MIN_WORLD_COORD && value <= MAX_WORLD_COORD;
    }

    private static void rejectMalformedWalkPacket(
            Client client,
            int opcode,
            int packetSize,
            int firstStepXAbs,
            int firstStepYAbs,
            String reason
    ) {
        client.resetWalkingQueue();
        logMalformedWalkPacket(client, opcode, packetSize, firstStepXAbs, firstStepYAbs, reason);
    }

    private static void logMalformedWalkPacket(
            Client client,
            int opcode,
            int packetSize,
            int firstStepXAbs,
            int firstStepYAbs,
            String reason
    ) {
        long now = System.currentTimeMillis();
        long last = lastMalformedLogMs.get();
        if (now - last < MALFORMED_LOG_INTERVAL_MS || !lastMalformedLogMs.compareAndSet(last, now)) {
            return;
        }
        logger.warn(
                "Rejected malformed walk packet player={} opcode={} size={} firstStep=({}, {}) region=({}, {}) reason={}",
                client.getPlayerName(),
                opcode,
                packetSize,
                firstStepXAbs,
                firstStepYAbs,
                client.mapRegionX,
                client.mapRegionY,
                reason
        );
    }
}
