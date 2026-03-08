package net.dodian.jobs.impl;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.content.dialogue.LegacyDialogueTickBridge;
import net.dodian.uber.game.model.EntityType;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.chunk.ChunkRepository;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.runtime.loop.GameThreadTaskQueue;
import net.dodian.uber.game.runtime.loop.GameCycleClock;
import net.dodian.uber.game.runtime.animation.PlayerAnimationService;
import net.dodian.uber.game.runtime.combat.CombatRuntimeService;
import net.dodian.uber.game.runtime.sync.util.IntHashSet;
import net.dodian.uber.game.runtime.sync.util.LongHashSet;
import net.dodian.uber.game.runtime.tasking.GameTaskRuntime;
import net.dodian.uber.game.runtime.world.npc.NpcTimerScheduler;
import net.dodian.uber.game.netty.NetworkConstants;
import net.dodian.uber.game.party.Balloons;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static net.dodian.utilities.DotEnvKt.getRuntimePhaseWarnMs;

public class EntityProcessor implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(EntityProcessor.class);
    private static final int[][] NPC_ROAM_DELTAS = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},           {0, 1},
            {1, -1},  {1, 0},  {1, 1}
    };

    private final ArrayList<Npc> activeNpcsForTick = new ArrayList<>();
    private final LongHashSet activeNpcChunks = new LongHashSet(128);
    private final IntHashSet activeNpcSlots = new IntHashSet(256);
    private static volatile Npc[] SPAWN_ALWAYS_ACTIVE = null;
    private static final ConcurrentLinkedQueue<Client> READY_INBOUND_PLAYERS = new ConcurrentLinkedQueue<>();

    public static void enqueueInboundReady(Client player) {
        if (player != null) {
            READY_INBOUND_PLAYERS.add(player);
        }
    }

    @Override
    public void run() {
        GameCycleClock.advance();
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
        // Advance offscreen timers (death-floor, respawn, boosted stat decay) without scanning all NPCs.
        long timerNsStart = startNs;
        NpcTimerScheduler.runDue(now);
        long timerNs = System.nanoTime() - timerNsStart;
        long chunkBuildNsStart = System.nanoTime();
        buildActiveNpcChunks(activeNpcChunks);
        long chunkBuildNs = System.nanoTime() - chunkBuildNsStart;
        long collectNsStart = System.nanoTime();
        List<Npc> activeNpcs = collectActiveNpcs(activeNpcChunks, activeNpcsForTick);
        long collectNs = System.nanoTime() - collectNsStart;
        long npcLoopNsStart = System.nanoTime();
        for (Npc npc : activeNpcs) {
            try {
                processNpc(now, npc, activeNpcChunks);
                // Keep chunk membership current for active NPCs; avoids full-world chunk-sync scans.
                npc.syncChunkMembership();
            } catch (Throwable throwable) {
                logger.error(
                        "NPC_MAIN actor failed slot={} id={} pos={}",
                        npc != null ? npc.getSlot() : -1,
                        npc != null ? npc.getId() : -1,
                        npc != null ? npc.getPosition() : null,
                        throwable
                );
            }
        }
        long npcLoopNs = System.nanoTime() - npcLoopNsStart;

        long totalMs = (System.nanoTime() - startNs) / 1_000_000L;
        if (totalMs >= getRuntimePhaseWarnMs()) {
            logger.warn(
                    "NPC_MAIN slow: total={}ms activeChunks={} timer={}ms chunks={}ms collect={}ms loop={}ms",
                    totalMs,
                    activeNpcChunks.size(),
                    timerNs / 1_000_000L,
                    chunkBuildNs / 1_000_000L,
                    collectNs / 1_000_000L,
                    npcLoopNs / 1_000_000L
            );
        }
    }

    public void runPlayerMainPhase() {
        PlayerHandler.forEachActivePlayer(this::processPlayer);
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

    private void processNpc(long now, Npc npc, LongHashSet activeNpcChunks) {
        if (!shouldProcessNpc(npc, activeNpcChunks)) {
            return;
        }
        npc.setCurrentGameCycle(GameCycleClock.currentCycle());

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
        npc.setProcessedGameCycle(npc.getCurrentGameCycle());
        GameTaskRuntime.cycleNpc(npc);
    }

    private boolean shouldProcessNpc(Npc npc, LongHashSet activeNpcChunks) {
        if (npc == null) {
            return false;
        }
        if (npc.isSpawnAlwaysActive()) {
            return true;
        }
        Position position = npc.getPosition();
        if (position == null) {
            return false;
        }
        return activeNpcChunks.contains(packChunkKey(position.getChunkX(), position.getChunkY()));
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

    static void buildActiveNpcChunks(LongHashSet activeChunks) {
        activeChunks.clear();
        PlayerHandler.forEachActivePlayer(player -> {
            Position position = player.getPosition();
            if (position == null) {
                return;
            }
            int centerChunkX = position.getChunkX();
            int centerChunkY = position.getChunkY();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    activeChunks.add(packChunkKey(centerChunkX + dx, centerChunkY + dy));
                }
            }
        });
    }

    private List<Npc> collectActiveNpcs(LongHashSet activeChunks, ArrayList<Npc> output) {
        output.clear();
        activeNpcSlots.clear();
        if (activeChunks.isEmpty()) {
            return output;
        }
        if (Server.chunkManager != null) {
            activeChunks.forEach(key -> {
                int chunkX = unpackChunkX(key);
                int chunkY = unpackChunkY(key);
                ChunkRepository repo = Server.chunkManager.getLoaded(chunkX, chunkY);
                if (repo == null) {
                    return;
                }
                for (Npc npc : repo.<Npc>getAll(EntityType.NPC)) {
                    if (npc != null && activeNpcSlots.add(npc.getSlot())) {
                        output.add(npc);
                    }
                }
            });
        }
        else {
            for (Npc npc : Server.npcManager.getNpcs()) {
                if (npc == null || npc.getPosition() == null) {
                    continue;
                }
                Position position = npc.getPosition();
                if (activeChunks.contains(packChunkKey(position.getChunkX(), position.getChunkY()))
                        && activeNpcSlots.add(npc.getSlot())) {
                    output.add(npc);
                }
            }
        }

        // Preserve legacy "spawn always active" semantics without scanning the full npc list every tick.
        for (Npc npc : getSpawnAlwaysActiveNpcs()) {
            if (npc != null && activeNpcSlots.add(npc.getSlot())) {
                output.add(npc);
            }
        }

        return output;
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
        PlayerHandler.forEachActivePlayer(Client::syncChunkMembership);
    }

    private void processInboundPackets() {
        net.dodian.uber.game.runtime.metrics.InboundOpcodeProfiler.beginTick();
        long startNs = System.nanoTime();
        ArrayList<Client> readyPlayers = new ArrayList<>();
        for (;;) {
            Client player = READY_INBOUND_PLAYERS.poll();
            if (player == null) {
                break;
            }
            readyPlayers.add(player);
        }

        int readyPlayersBefore = readyPlayers.size();
        int readyPlayersProcessed = 0;
        int processedPackets = 0;
        int processedWalkPackets = 0;
        int processedMousePackets = 0;
        int replacedWalkPackets = 0;
        int replacedMousePackets = 0;
        int droppedFifoPackets = 0;
        int totalPendingBefore = 0;
        int totalPendingAfter = 0;
        int deferredPlayers = 0;
        int maxPendingBefore = 0;
        int maxPendingAfter = 0;

        for (Client player : readyPlayers) {
            if (player == null) {
                continue;
            }
            player.clearInboundReadyFlag();
            if (player.disconnected || !player.isActive) {
                continue;
            }

            int pendingBefore = player.getPendingInboundPacketCount();
            if (pendingBefore <= 0) {
                continue;
            }

            readyPlayersProcessed++;
            totalPendingBefore += pendingBefore;
            if (pendingBefore > maxPendingBefore) {
                maxPendingBefore = pendingBefore;
            }

            Client.InboundProcessResult result = player.processQueuedPackets(NetworkConstants.PACKET_PROCESS_LIMIT_PER_TICK);
            processedPackets += result.processedPackets();
            processedWalkPackets += result.walkPacketsProcessed();
            processedMousePackets += result.mousePacketsProcessed();
            replacedWalkPackets += result.walkPacketsReplaced();
            replacedMousePackets += result.mousePacketsReplaced();
            droppedFifoPackets += result.fifoPacketsDropped();

            int pendingAfter = player.getPendingInboundPacketCount();
            totalPendingAfter += pendingAfter;
            if (pendingAfter > maxPendingAfter) {
                maxPendingAfter = pendingAfter;
            }
            if (pendingAfter > 0) {
                deferredPlayers++;
                player.markInboundReadyIfNeeded();
            }
        }

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        if (elapsedMs >= getRuntimePhaseWarnMs()) {
            logger.warn(
                    "INBOUND_PACKETS slow: total={}ms readyPlayers={} processedReadyPlayers={} processedPackets={} walkProcessed={} mouseProcessed={} walkReplaced={} mouseReplaced={} fifoDropped={} deferredPlayers={} readyAfter={} pendingBeforeTotal={} pendingAfterTotal={} maxBefore={} maxAfter={} top={}",
                    elapsedMs,
                    readyPlayersBefore,
                    readyPlayersProcessed,
                    processedPackets,
                    processedWalkPackets,
                    processedMousePackets,
                    replacedWalkPackets,
                    replacedMousePackets,
                    droppedFifoPackets,
                    deferredPlayers,
                    READY_INBOUND_PLAYERS.size(),
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
            final int[] counts = new int[2];
            PlayerHandler.forEachActivePlayer(checkPlayer -> {
                if (checkPlayer.inWildy()) {
                    counts[0]++;
                } else if (checkPlayer.inEdgeville()) {
                    counts[1]++;
                }
            });
            int peopleInWild = counts[0];
            int peopleInEdge = counts[1];
            npc.setText("There is currently " + peopleInWild + " player" + (peopleInWild != 1 ? "s" : "") + " in the wild and " + peopleInEdge + " player" + (peopleInEdge != 1 ? "s" : "") + " in Edgeville!");
        }
    }

    private void handleServerCycles() {
        long cycle = GameCycleClock.currentCycle();
        if (cycle % 10 == 0) {
            Server.connections.clear();
            Server.nullConnections = 0;
        }
        if (cycle % 100 == 0) {
            Server.banned.clear();
        }
    }

    private void processPlayer(Client player) {
        if (!player.initialized) {
            player.initialize();
            player.initialized = true;
        }
        player.setCurrentGameCycle(GameCycleClock.currentCycle());
        int startingHealth = player.getCurrentHealth();
        int startingPrayer = player.getCurrentPrayer();
        int startingX = player.getPosition().getX();
        int startingY = player.getPosition().getY();
        int startingZ = player.getPosition().getZ();
        player.process();
        player.setProcessedGameCycle(player.getCurrentGameCycle());
        player.setLastProcessedCycle(player.getProcessedGameCycle());
        GameTaskRuntime.cyclePlayer(player);
        LegacyDialogueTickBridge.flushIfNeeded(player);
        CombatRuntimeService.process(player, player.getProcessedGameCycle());
        PlayerAnimationService.flush(player, player.getProcessedGameCycle());

        if (startingHealth != player.getCurrentHealth() || startingPrayer != player.getCurrentPrayer()) {
            player.markSaveDirty(net.dodian.uber.game.persistence.player.PlayerSaveSegment.STATS.getMask()
                    | net.dodian.uber.game.persistence.player.PlayerSaveSegment.EFFECTS.getMask()
                    | net.dodian.uber.game.persistence.player.PlayerSaveSegment.META.getMask());
        }
        if (startingX != player.getPosition().getX() || startingY != player.getPosition().getY() || startingZ != player.getPosition().getZ()) {
            player.markSaveDirty(net.dodian.uber.game.persistence.player.PlayerSaveSegment.POSITION.getMask());
        }

        player.postProcessing();
        player.getNextPlayerMovement();
    }

    private static long packChunkKey(int chunkX, int chunkY) {
        return (((long) chunkX) << 32) ^ (chunkY & 0xffffffffL);
    }

    private static int unpackChunkX(long key) {
        return (int) (key >> 32);
    }

    private static int unpackChunkY(long key) {
        return (int) key;
    }

}
