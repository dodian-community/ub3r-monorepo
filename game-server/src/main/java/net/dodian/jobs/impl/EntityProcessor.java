package net.dodian.jobs.impl;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.EntityType;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.chunk.Chunk;
import net.dodian.uber.game.model.chunk.ChunkRepository;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.runtime.interaction.InteractionProcessor;
import net.dodian.uber.game.runtime.loop.GameThreadTaskQueue;
import net.dodian.uber.game.runtime.world.npc.NpcTimerScheduler;
import net.dodian.uber.game.netty.NetworkConstants;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.party.Balloons;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static net.dodian.utilities.DotEnvKt.getInteractionPipelineEnabled;
import static net.dodian.utilities.DotEnvKt.getRuntimePhaseWarnMs;

public class EntityProcessor implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(EntityProcessor.class);
    private static final int[][] NPC_ROAM_DELTAS = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},           {0, 1},
            {1, -1},  {1, 0},  {1, 1}
    };

    private Set<Npc> activeNpcsForTick = Collections.emptySet();
    private static volatile Npc[] SPAWN_ALWAYS_ACTIVE = null;

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        GameThreadTaskQueue.drain();
        runInboundPacketPhase();
        runNpcMainPhase(now);
        runPlayerMainPhase();
        runMovementFinalizePhase();
        runHousekeepingPhase(now);
    }

    public void runInboundPacketPhase() {
        processInboundPackets();
    }

    public void runNpcMainPhase(long now) {
        long startNs = System.nanoTime();
        long chunksNsStart = startNs;
        // Advance offscreen timers (death-floor, respawn, boosted stat decay) without scanning all NPCs.
        NpcTimerScheduler.runDue(now);
        Set<Chunk> activeNpcChunks = buildActiveNpcChunks();
        long chunksNs = System.nanoTime() - chunksNsStart;
        long npcLoopNsStart = System.nanoTime();
        Set<Npc> activeNpcs = collectActiveNpcs(activeNpcChunks);
        this.activeNpcsForTick = activeNpcs;
        for (Npc npc : activeNpcs) {
            processNpc(now, npc, activeNpcChunks);
            // Keep chunk membership current for active NPCs; avoids full-world chunk-sync scans.
            npc.syncChunkMembership();
        }
        long npcLoopNs = System.nanoTime() - npcLoopNsStart;
        long syncNs = 0L;

        long totalMs = (System.nanoTime() - startNs) / 1_000_000L;
        if (totalMs >= getRuntimePhaseWarnMs()) {
            logger.warn(
                    "NPC_MAIN slow: total={}ms activeChunks={} chunks={}ms loop={}ms syncChunks={}ms",
                    totalMs,
                    activeNpcChunks.size(),
                    chunksNs / 1_000_000L,
                    npcLoopNs / 1_000_000L,
                    syncNs / 1_000_000L
            );
        }
    }

    public void runPlayerMainPhase() {
        for (int i = 0; i < Constants.maxPlayers; i++) {
            Client player = (Client) PlayerHandler.players[i];
            if (player == null || player.disconnected || !player.isActive) {
                continue;
            }
            processPlayer(player);
        }
    }

    public void runMovementFinalizePhase() {
        syncActivePlayerChunksForTick();
        consumeNpcDirectionsForTick();
    }

    public void runHousekeepingPhase(long now) {
        if (Server.updateRunning && now - Server.updateStartTime > (Server.updateSeconds * 1000L)) {
            if (PlayerHandler.getPlayerCount() < 1) {
                System.exit(0);
            }
        }
        handleServerCycles();
    }

    private void processNpc(long now, Npc npc, Set<Chunk> activeNpcChunks) {
        if (!shouldProcessNpc(npc, activeNpcChunks)) {
            return;
        }

        // Keep static NPCs facing their configured spawn direction, but do not
        // force roaming NPCs back to spawn-facing every tick.
        if (!npc.isFighting() && npc.isAlive() && npc.getWalkRadius() <= 0) {
            npc.setFocus(npc.getPosition().getX() + Utils.directionDeltaX[npc.getFace()], npc.getPosition().getY() + Utils.directionDeltaY[npc.getFace()]);
        }

        if (npc.getLastAttack() > 0) {
            npc.setLastAttack(npc.getLastAttack() - 1); // Decrease attack timer manually
        }

        if (npc.alive && npc.isFighting() && npc.getLastAttack() == 0) {
            npc.attack();
            handleNpcSpecialCases(npc);
        }

        handleNpcRoaming(npc);
        npc.effectChange();
        handleNpcRandomActions(npc);
    }

    private boolean shouldProcessNpc(Npc npc, Set<Chunk> activeNpcChunks) {
        if (npc == null) {
            return false;
        }
        if (npc.isSpawnAlwaysActive()) {
            return true;
        }
        return activeNpcChunks.contains(npc.getPosition().getChunk());
    }

    static boolean withinWalkRadius(Position origin, int targetX, int targetY, int walkRadius) {
        if (walkRadius <= 0) {
            return true;
        }
        return Math.abs(targetX - origin.getX()) <= walkRadius && Math.abs(targetY - origin.getY()) <= walkRadius;
    }

    private void handleNpcRoaming(Npc npc) {
        if (!npc.isAlive() || !npc.isVisible() || npc.isFighting()) {
            return;
        }

        int walkRadius = npc.getWalkRadius();
        if (walkRadius <= 0) {
            return;
        }

        // Keep idle roaming sparse to avoid jittery movement.
        if (Misc.chance(10) != 1) {
            return;
        }

        for (int attempt = 0; attempt < NPC_ROAM_DELTAS.length; attempt++) {
            int[] delta = NPC_ROAM_DELTAS[Utils.random(NPC_ROAM_DELTAS.length - 1)];
            int dx = delta[0];
            int dy = delta[1];

            int fromX = npc.getPosition().getX();
            int fromY = npc.getPosition().getY();
            int toX = fromX + dx;
            int toY = fromY + dy;

            if (!withinWalkRadius(npc.getOriginalPosition(), toX, toY, walkRadius)) {
                continue;
            }
            if (!npc.canMove(dx, dy)) {
                continue;
            }

            npc.moveTo(toX, toY, npc.getPosition().getZ());
            npc.markWalkStep(fromX, fromY, toX, toY);
            return;
        }
    }

    static Set<Chunk> buildActiveNpcChunks() {
        Set<Chunk> activeChunks = new HashSet<>();
        for (int i = 0; i < Constants.maxPlayers; i++) {
            Client player = (Client) PlayerHandler.players[i];
            if (player == null || player.disconnected || !player.isActive) {
                continue;
            }
            Chunk center = player.getPosition().getChunk();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    activeChunks.add(center.translate(dx, dy));
                }
            }
        }
        return activeChunks;
    }

    private Set<Npc> collectActiveNpcs(Set<Chunk> activeChunks) {
        Set<Npc> active = new HashSet<>();
        if (activeChunks.isEmpty()) {
            return active;
        }
        if (Server.chunkManager != null) {
            for (Chunk chunk : activeChunks) {
                ChunkRepository repo = Server.chunkManager.getLoaded(chunk);
                if (repo == null) {
                    continue;
                }
                for (Npc npc : repo.<Npc>getAll(EntityType.NPC)) {
                    if (npc != null) {
                        active.add(npc);
                    }
                }
            }
        }
        else {
            for (Npc npc : Server.npcManager.getNpcs()) {
                if (npc == null || npc.getPosition() == null) {
                    continue;
                }
                if (activeChunks.contains(npc.getPosition().getChunk())) {
                    active.add(npc);
                }
            }
        }

        // Preserve legacy "spawn always active" semantics without scanning the full npc list every tick.
        for (Npc npc : getSpawnAlwaysActiveNpcs()) {
            if (npc != null) {
                active.add(npc);
            }
        }

        return active;
    }

    private static Npc[] getSpawnAlwaysActiveNpcs() {
        Npc[] cached = SPAWN_ALWAYS_ACTIVE;
        if (cached != null) {
            return cached;
        }
        synchronized (EntityProcessor.class) {
            cached = SPAWN_ALWAYS_ACTIVE;
            if (cached != null) {
                return cached;
            }
            ArrayList<Npc> list = new ArrayList<>();
            for (Npc npc : Server.npcManager.getNpcs()) {
                if (npc != null && npc.isSpawnAlwaysActive()) {
                    list.add(npc);
                }
            }
            cached = list.toArray(new Npc[0]);
            SPAWN_ALWAYS_ACTIVE = cached;
            return cached;
        }
    }

    static void syncActivePlayerChunksForTick() {
        for (int i = 0; i < Constants.maxPlayers; i++) {
            Client player = (Client) PlayerHandler.players[i];
            if (player == null || player.disconnected || !player.isActive) {
                continue;
            }
            player.syncChunkMembership();
        }
    }

    private void processInboundPackets() {
        net.dodian.uber.game.runtime.metrics.InboundOpcodeProfiler.beginTick();
        long startNs = System.nanoTime();
        int activePlayers = 0;
        int processedPackets = 0;
        int totalPendingBefore = 0;
        int totalPendingAfter = 0;
        int backlogPlayers = 0;
        int maxPendingBefore = 0;
        int maxPendingAfter = 0;

        for (int i = 0; i < Constants.maxPlayers; i++) {
            Client player = (Client) PlayerHandler.players[i];
            if (player == null || player.disconnected || !player.isActive) {
                continue;
            }
            activePlayers++;

            int pendingBefore = player.getPendingInboundPacketCount();
            totalPendingBefore += pendingBefore;
            if (pendingBefore > maxPendingBefore) {
                maxPendingBefore = pendingBefore;
            }

            processedPackets += player.processQueuedPackets(NetworkConstants.PACKET_PROCESS_LIMIT_PER_TICK);

            int pendingAfter = player.getPendingInboundPacketCount();
            totalPendingAfter += pendingAfter;
            if (pendingAfter > maxPendingAfter) {
                maxPendingAfter = pendingAfter;
            }
            if (pendingAfter > 0) {
                backlogPlayers++;
            }
        }

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        if (elapsedMs >= getRuntimePhaseWarnMs()) {
            logger.warn(
                    "INBOUND_PACKETS slow: total={}ms activePlayers={} processedPackets={} backlogPlayers={} pendingBeforeTotal={} pendingAfterTotal={} maxBefore={} maxAfter={} top={}",
                    elapsedMs,
                    activePlayers,
                    processedPackets,
                    backlogPlayers,
                    totalPendingBefore,
                    totalPendingAfter,
                    maxPendingBefore,
                    maxPendingAfter,
                    net.dodian.uber.game.runtime.metrics.InboundOpcodeProfiler.top3Summary()
            );
        }
    }

    private void consumeNpcDirectionsForTick() {
        for (Npc npc : activeNpcsForTick) {
            npc.setDirection(npc.getNextWalkingDirection());
        }
    }

    private void handleNpcSpecialCases(Npc npc) {
        if (npc.getId() == 2261) {
            handleDwayneEffect(npc);
        }
    }

    private void handleDwayneEffect(Npc npc) {
        int hp = (int) (npc.getMaxHealth() * 0.40);
        if (npc.inFrenzy != -1 && !npc.enraged(20000)) {
            npc.calmedDown();
            npc.sendFightMessage(npc.npcName() + " have calmed down.");
        } else if (!npc.hadFrenzy && npc.inFrenzy == -1 && npc.getCurrentHealth() < hp) {
            npc.inFrenzy = System.currentTimeMillis();
            npc.sendFightMessage(npc.npcName() + " have become enraged!");
        }
    }

    private void handleNpcRandomActions(Npc npc) {
        int npcId = npc.getId();
        switch (npcId) {
            case 3805:
                handleJackpotAnnouncement(npc);
                break;
            case 4218:
                handlePlagueWarning(npc);
                break;
            case 2805:
                handleCowMooing(npc);
                break;
            case 5924:
                handleNpcAnimation(npc, 6549, 20);
                break;
            case 555:
                handlePlagueMessage(npc);
                break;
            case 5792:
                handlePartyAnnouncement(npc);
                break;
            case 3306:
                handlePlayerCountsAnnouncement(npc);
                break;
            default:
                break;
        }
    }

    private void handleJackpotAnnouncement(Npc npc) {
        if (Misc.chance(100) == 1) {
            int jackpot = Math.min(Server.slots.slotsJackpot + Server.slots.peteBalance, Integer.MAX_VALUE);
            npc.setText("Current Jackpot is " + jackpot + " coins!");
        }
    }

    private void handlePlagueWarning(Npc npc) {
        if (Misc.chance(8) == 1) {
            npc.setText("Watch out for the plague!!");
        }
    }

    private void handleCowMooing(Npc npc) {
        if (Misc.chance(50) == 1) {
            npc.setText(Misc.chance(2) == 1 ? "Moo" : "Moo!!");
        }
    }

    private void handleNpcAnimation(Npc npc, int animId, int chance) {
        if (Misc.chance(chance) == 1) {
            npc.requestAnim(animId, 0);
        }
    }

    private void handlePlagueMessage(Npc npc) {
        if (Misc.chance(10) == 1) {
            npc.setText(Misc.chance(2) == 1 ? "The plague is coming!" : "Watch out for the plague!!");
        }
    }

    private void handlePartyAnnouncement(Npc npc) {
        if (Balloons.eventActive()) {
            npc.requestAnim(866, 0);
            npc.setText(Balloons.spawnedBalloons() ? "A party is going on right now!" : "A party is about to Start!!!!");
        }
    }

    private void handlePlayerCountsAnnouncement(Npc npc) {
        if (Misc.chance(25) == 1) {
            int peopleInEdge = 0;
            int peopleInWild = 0;
            for (int i = 0; i < Constants.maxPlayers; i++) {
                Client checkPlayer = (Client) PlayerHandler.players[i];
                if (checkPlayer != null) {
                    if (checkPlayer.inWildy()) {
                        peopleInWild++;
                    } else if (checkPlayer.inEdgeville()) {
                        peopleInEdge++;
                    }
                }
            }
            npc.setText("There is currently " + peopleInWild + " player" + (peopleInWild != 1 ? "s" : "") + " in the wild and " + peopleInEdge + " player" + (peopleInEdge != 1 ? "s" : "") + " in Edgeville!");
        }
    }

    private void handleServerCycles() {
        if (PlayerHandler.cycle % 10 == 0) {
            Server.connections.clear();
            Server.nullConnections = 0;
        }
        if (PlayerHandler.cycle % 100 == 0) {
            Server.banned.clear();
        }
        if (PlayerHandler.cycle > 10000) {
            PlayerHandler.cycle = 0;
        }
        PlayerHandler.cycle++;
    }

    private void processPlayer(Client player) {
        if (!player.initialized) {
            player.initialize();
            player.initialized = true;
        }
        player.setLastProcessedCycle(PlayerHandler.cycle);
        player.process();

        player.postProcessing();
        player.getNextPlayerMovement();
    }

}
