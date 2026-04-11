package net.dodian.uber.game.engine.processing

import java.util.ArrayList
import java.util.concurrent.ConcurrentLinkedQueue
import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.config.runtimePhaseWarnMs
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.engine.loop.GameLoopService
import net.dodian.uber.game.engine.loop.GameThreadTaskQueue
import net.dodian.uber.game.engine.sync.util.IntHashSet
import net.dodian.uber.game.engine.sync.util.LongHashSet
import net.dodian.uber.game.engine.tasking.GameTaskRuntime
import net.dodian.uber.game.model.EntityType
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.chunk.ChunkRepository
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.follow.FollowPathfindingTelemetry
import net.dodian.uber.game.engine.systems.follow.FollowService
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.model.`object`.GlobalObject
import net.dodian.uber.game.netty.NetworkConstants
import net.dodian.uber.game.content.events.partyroom.Balloons
import net.dodian.uber.game.engine.lifecycle.PlayerLifecycleTickService
import net.dodian.uber.game.engine.systems.animation.PlayerAnimationService
import net.dodian.uber.game.content.combat.CombatRuntimeService
import net.dodian.uber.game.content.social.dialogue.DialogueService
import net.dodian.uber.game.engine.systems.world.npc.NpcTimerScheduler
import net.dodian.uber.game.engine.util.Misc
import net.dodian.uber.game.engine.util.Utils
import org.slf4j.LoggerFactory

class EntityProcessor : Runnable {
    private val activeNpcsForTick = ArrayList<Npc>()
    private val activeNpcChunks = LongHashSet(128)
    private val activeNpcSlots = IntHashSet(256)

    override fun run() {
        GameCycleClock.advance()
        val now = System.currentTimeMillis()
        GameThreadTaskQueue.drain()
        runInboundPacketPhase()
        runNpcMainPhase(now)
        runPlayerMainPhase(now)
        runMovementFinalizePhase()
        runHousekeepingPhase(now)
    }

    fun runInboundPacketPhase() {
        processInboundPackets()
    }

    fun runNpcMainPhase(now: Long) {
        val startNs = System.nanoTime()
        val timerNsStart = startNs
        NpcTimerScheduler.runDue(now)
        val timerNs = System.nanoTime() - timerNsStart
        val chunkBuildNsStart = System.nanoTime()
        buildActiveNpcChunks(activeNpcChunks)
        val chunkBuildNs = System.nanoTime() - chunkBuildNsStart
        val collectNsStart = System.nanoTime()
        val activeNpcs = collectActiveNpcs(activeNpcChunks, activeNpcsForTick)
        val collectNs = System.nanoTime() - collectNsStart
        val npcLoopNsStart = System.nanoTime()
        for (npc in activeNpcs) {
            try {
                processNpc(now, npc, activeNpcChunks)
                npc.syncChunkMembership()
            } catch (throwable: Throwable) {
                logger.error(
                    "NPC_MAIN actor failed slot={} id={} pos={}",
                    npc.slot,
                    npc.id,
                    npc.position,
                    throwable,
                )
            }
        }
        val npcLoopNs = System.nanoTime() - npcLoopNsStart

        val totalMs = (System.nanoTime() - startNs) / 1_000_000L
        if (totalMs >= runtimePhaseWarnMs) {
            logger.warn(
                "NPC_MAIN slow: total={}ms activeChunks={} timer={}ms chunks={}ms collect={}ms loop={}ms",
                totalMs,
                activeNpcChunks.size(),
                timerNs / 1_000_000L,
                chunkBuildNs / 1_000_000L,
                collectNs / 1_000_000L,
                npcLoopNs / 1_000_000L,
            )
        }
    }

    fun runPlayerMainPhase(wallClockNow: Long) {
        FollowPathfindingTelemetry.beginTick()
        FollowService.processTick()
        FollowPathfindingTelemetry.logIfSlow(GameCycleClock.currentCycle())
        val activePlayers = PlayerRegistry.snapshotActivePlayersSortedBySlot()
        for (player in activePlayers) {
            processPlayer(player, wallClockNow)
        }
    }

    fun runMovementFinalizePhase() {
        syncActivePlayerChunksForTick()
        consumeNpcDirectionsForTick()
    }

    fun runHousekeepingPhase(now: Long) {
        if (Server.updateRunning && now - Server.updateStartTime > (Server.updateSeconds * 1000L)) {
            if (PlayerRegistry.getPlayerCount() < 1) {
                requestControlledShutdown()
            }
        }
        handleServerCycles()
    }

    private fun processNpc(now: Long, npc: Npc, activeNpcChunks: LongHashSet) {
        if (!shouldProcessNpc(npc, activeNpcChunks)) {
            return
        }
        npc.currentGameCycle = GameCycleClock.currentCycle()

        if (!npc.isFighting && npc.isAlive && npc.walkRadius <= 0) {
            npc.setFocus(
                npc.position.x + Utils.directionDeltaX[npc.face],
                npc.position.y + Utils.directionDeltaY[npc.face],
            )
        }

        if (npc.lastAttack > 0) {
            npc.lastAttack = npc.lastAttack - 1
        }

        if (npc.alive && npc.isFighting && npc.lastAttack == 0) {
            npc.attack()
            handleNpcSpecialCases(npc)
        }

        handleNpcRoaming(npc)
        npc.effectChange()
        handleNpcRandomActions(npc)
        npc.processedGameCycle = npc.currentGameCycle
        GameTaskRuntime.cycleNpc(npc)
    }

    private fun shouldProcessNpc(npc: Npc?, activeNpcChunks: LongHashSet): Boolean {
        if (npc == null) {
            return false
        }
        if (npc.isSpawnAlwaysActive) {
            return true
        }
        val position = npc.position ?: return false
        return activeNpcChunks.contains(packChunkKey(position.chunkX, position.chunkY))
    }

    private fun handleNpcRoaming(npc: Npc) {
        if (!npc.isAlive || !npc.isVisible || npc.isFighting) {
            return
        }

        val walkRadius = npc.walkRadius
        if (walkRadius <= 0) {
            return
        }

        if (Misc.chance(10) != 1) {
            return
        }

        repeat(NPC_ROAM_DELTAS.size) {
            val delta = NPC_ROAM_DELTAS[Utils.random(NPC_ROAM_DELTAS.size - 1)]
            val dx = delta[0]
            val dy = delta[1]

            val fromX = npc.position.x
            val fromY = npc.position.y
            val toX = fromX + dx
            val toY = fromY + dy

            if (!withinWalkRadius(npc.originalPosition, toX, toY, walkRadius)) {
                return@repeat
            }
            if (!npc.canMove(dx, dy)) {
                return@repeat
            }

            npc.moveTo(toX, toY, npc.position.z)
            npc.markWalkStep(fromX, fromY, toX, toY)
            return
        }
    }

    private fun collectActiveNpcs(activeChunks: LongHashSet, output: ArrayList<Npc>): List<Npc> {
        output.clear()
        activeNpcSlots.clear()
        if (activeChunks.isEmpty()) {
            return output
        }
        val chunkManager = Server.chunkManager
        if (chunkManager == null) {
            if (chunkIndexMissingLogged.compareAndSet(false, true)) {
                logger.warn("Chunk manager unavailable during NPC collection; skipping chunk-indexed NPC pass.")
            }
        } else {
            activeChunks.forEach { key ->
                val chunkX = unpackChunkX(key)
                val chunkY = unpackChunkY(key)
                val repo: ChunkRepository = chunkManager.getLoaded(chunkX, chunkY) ?: return@forEach
                for (npc in repo.getAll<Npc>(EntityType.NPC)) {
                    if (npc != null && activeNpcSlots.add(npc.slot)) {
                        output.add(npc)
                    }
                }
            }
        }

        for (npc in getSpawnAlwaysActiveNpcs()) {
            if (npc != null && activeNpcSlots.add(npc.slot)) {
                output.add(npc)
            }
        }

        return output
    }

    private fun processInboundPackets() {
        net.dodian.uber.game.engine.metrics.InboundOpcodeProfiler.beginTick()
        val startNs = System.nanoTime()
        val readyPlayers = ArrayList<Client>()
        while (true) {
            val player = READY_INBOUND_PLAYERS.poll() ?: break
            readyPlayers.add(player)
        }

        val readyPlayersBefore = readyPlayers.size
        var readyPlayersProcessed = 0
        var processedPackets = 0
        var processedWalkPackets = 0
        var processedMousePackets = 0
        var replacedWalkPackets = 0
        var replacedMousePackets = 0
        var droppedFifoPackets = 0
        var totalPendingBefore = 0
        var totalPendingAfter = 0
        var deferredPlayers = 0
        var maxPendingBefore = 0
        var maxPendingAfter = 0

        for (player in readyPlayers) {
            player.clearInboundReadyFlag()
            if (player.disconnected || !player.isActive) {
                continue
            }

            val pendingBefore = player.pendingInboundPacketCount
            if (pendingBefore <= 0) {
                continue
            }

            readyPlayersProcessed++
            totalPendingBefore += pendingBefore
            if (pendingBefore > maxPendingBefore) {
                maxPendingBefore = pendingBefore
            }

            val result = player.processQueuedPackets(NetworkConstants.PACKET_PROCESS_LIMIT_PER_TICK)
            processedPackets += result.processedPackets()
            processedWalkPackets += result.walkPacketsProcessed()
            processedMousePackets += result.mousePacketsProcessed()
            replacedWalkPackets += result.walkPacketsReplaced()
            replacedMousePackets += result.mousePacketsReplaced()
            droppedFifoPackets += result.fifoPacketsDropped()

            val pendingAfter = player.pendingInboundPacketCount
            totalPendingAfter += pendingAfter
            if (pendingAfter > maxPendingAfter) {
                maxPendingAfter = pendingAfter
            }
            if (pendingAfter > 0) {
                deferredPlayers++
                player.markInboundReadyIfNeeded()
            }
        }

        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
        if (elapsedMs >= runtimePhaseWarnMs) {
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
                READY_INBOUND_PLAYERS.size,
                totalPendingBefore,
                totalPendingAfter,
                maxPendingBefore,
                maxPendingAfter,
                net.dodian.uber.game.engine.metrics.InboundOpcodeProfiler.top3Summary(),
            )
        }
    }

    private fun consumeNpcDirectionsForTick() {
        for (npc in activeNpcsForTick) {
            npc.direction = npc.nextWalkingDirection
        }
    }

    private fun handleNpcSpecialCases(npc: Npc) {
        if (npc.id == 2261) {
            handleDwayneEffect(npc)
        }
    }

    private fun handleDwayneEffect(npc: Npc) {
        val hp = (npc.maxHealth * 0.40).toInt()
        if (npc.inFrenzy != -1L && !npc.enraged(20000)) {
            npc.calmedDown()
            npc.sendFightMessage("${npc.npcName()} have calmed down.")
        } else if (!npc.hadFrenzy && npc.inFrenzy == -1L && npc.currentHealth < hp) {
            npc.inFrenzy = System.currentTimeMillis()
            npc.sendFightMessage("${npc.npcName()} have become enraged!")
        }
    }

    private fun handleNpcRandomActions(npc: Npc) {
        when (npc.id) {
            3805 -> handleJackpotAnnouncement(npc)
            4218 -> handlePlagueWarning(npc)
            2805 -> handleCowMooing(npc)
            5924 -> handleNpcAnimation(npc, 6549, 20)
            555 -> handlePlagueMessage(npc)
            5792 -> handlePartyAnnouncement(npc)
            3306 -> handlePlayerCountsAnnouncement(npc)
        }
    }

    private fun handleJackpotAnnouncement(npc: Npc) {
        if (Misc.chance(100) == 1) {
            val jackpot = minOf(Server.slots.slotsJackpot + Server.slots.peteBalance, Int.MAX_VALUE)
            npc.setText("Current Jackpot is $jackpot coins!")
        }
    }

    private fun handlePlagueWarning(npc: Npc) {
        if (Misc.chance(8) == 1) {
            npc.setText("Watch out for the plague!!")
        }
    }

    private fun handleCowMooing(npc: Npc) {
        if (Misc.chance(50) == 1) {
            npc.setText(if (Misc.chance(2) == 1) "Moo" else "Moo!!")
        }
    }

    private fun handleNpcAnimation(npc: Npc, animId: Int, chance: Int) {
        if (Misc.chance(chance) == 1) {
            npc.performAnimation(animId, 0)
        }
    }

    private fun handlePlagueMessage(npc: Npc) {
        if (Misc.chance(10) == 1) {
            npc.setText(if (Misc.chance(2) == 1) "The plague is coming!" else "Watch out for the plague!!")
        }
    }

    private fun handlePartyAnnouncement(npc: Npc) {
        if (Balloons.eventActive()) {
            npc.performAnimation(866, 0)
            npc.setText(if (Balloons.spawnedBalloons()) "A party is going on right now!" else "A party is about to Start!!!!")
        }
    }

    private fun handlePlayerCountsAnnouncement(npc: Npc) {
        if (Misc.chance(25) == 1) {
            var peopleInWild = 0
            var peopleInEdge = 0
            PlayerRegistry.forEachActivePlayer { checkPlayer ->
                if (checkPlayer.inWildy()) {
                    peopleInWild++
                } else if (checkPlayer.inEdgeville()) {
                    peopleInEdge++
                }
            }
            npc.setText(
                "There is currently $peopleInWild player${if (peopleInWild != 1) "s" else ""} in the wild and $peopleInEdge player${if (peopleInEdge != 1) "s" else ""} in Edgeville!",
            )
        }
    }

    private fun handleServerCycles() {
        val cycle = GameCycleClock.currentCycle()
        if (cycle % 10 == 0L) {
            Server.connections.clear()
            Server.nullConnections = 0
        }
        if (cycle % 100 == 0L) {
            Server.banned.clear()
        }
    }

    private fun processPlayer(
        player: Client,
        wallClockNow: Long,
    ) {
        if (!player.initialized) {
            player.initialize()
            player.initialized = true
        }
        player.currentGameCycle = GameCycleClock.currentCycle()
        PlayerLifecycleTickService.processBeforeCombat(player)
        val startingHealth = player.currentHealth
        val startingPrayer = player.currentPrayer
        val startingX = player.position.x
        val startingY = player.position.y
        val startingZ = player.position.z
        player.processedGameCycle = player.currentGameCycle
        player.lastProcessedCycle = player.processedGameCycle
        GameTaskRuntime.cyclePlayer(player)
        DialogueService.flushIndexedIfNeeded(player)
        CombatRuntimeService.process(player, player.processedGameCycle)
        PlayerAnimationService.flush(player, player.processedGameCycle)
        GlobalObject.updateObject(player)

        if (startingHealth != player.currentHealth || startingPrayer != player.currentPrayer) {
            player.markSaveDirty(
                net.dodian.uber.game.persistence.player.PlayerSaveSegment.STATS.mask or
                    net.dodian.uber.game.persistence.player.PlayerSaveSegment.EFFECTS.mask or
                    net.dodian.uber.game.persistence.player.PlayerSaveSegment.META.mask,
            )
        }
        if (startingX != player.position.x || startingY != player.position.y || startingZ != player.position.z) {
            player.markSaveDirty(net.dodian.uber.game.persistence.player.PlayerSaveSegment.POSITION.mask)
        }

        PlayerLifecycleTickService.processAfterCombat(player, wallClockNow)
        PlayerLifecycleTickService.processEffectsPeriodicPersistence(player, wallClockNow)
        player.postProcessing()
        player.getNextPlayerMovement()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EntityProcessor::class.java)
        private val NPC_ROAM_DELTAS =
            arrayOf(
                intArrayOf(-1, -1),
                intArrayOf(-1, 0),
                intArrayOf(-1, 1),
                intArrayOf(0, -1),
                intArrayOf(0, 1),
                intArrayOf(1, -1),
                intArrayOf(1, 0),
                intArrayOf(1, 1),
            )
        @Volatile
        private var SPAWN_ALWAYS_ACTIVE: Array<Npc?>? = null
        private val READY_INBOUND_PLAYERS = ConcurrentLinkedQueue<Client>()

        @JvmStatic
        fun enqueueInboundReady(player: Client?) {
            if (player != null) {
                READY_INBOUND_PLAYERS.add(player)
            }
        }

        @JvmStatic
        fun withinWalkRadius(origin: Position, targetX: Int, targetY: Int, walkRadius: Int): Boolean {
            if (walkRadius <= 0) {
                return true
            }
            return kotlin.math.abs(targetX - origin.x) <= walkRadius && kotlin.math.abs(targetY - origin.y) <= walkRadius
        }

        @JvmStatic
        fun buildActiveNpcChunks(activeChunks: LongHashSet) {
            activeChunks.clear()
            PlayerRegistry.forEachActivePlayer { player ->
                val position = player.position ?: return@forEachActivePlayer
                val centerChunkX = position.chunkX
                val centerChunkY = position.chunkY
                for (dx in -2..2) {
                    for (dy in -2..2) {
                        activeChunks.add(packChunkKey(centerChunkX + dx, centerChunkY + dy))
                    }
                }
            }
        }

        @JvmStatic
        fun syncActivePlayerChunksForTick() {
            PlayerRegistry.forEachActivePlayer { it.syncChunkMembership() }
        }

        private fun getSpawnAlwaysActiveNpcs(): Array<Npc?> {
            val cached = SPAWN_ALWAYS_ACTIVE
            if (cached != null) {
                return cached
            }
            synchronized(EntityProcessor::class.java) {
                val current = SPAWN_ALWAYS_ACTIVE
                if (current != null) {
                    return current
                }
                val list = ArrayList<Npc?>()
                for (npc in Server.npcManager.getNpcs()) {
                    if (npc != null && npc.isSpawnAlwaysActive) {
                        list.add(npc)
                    }
                }
                val built = list.toTypedArray()
                SPAWN_ALWAYS_ACTIVE = built
                return built
            }
        }

        private fun packChunkKey(chunkX: Int, chunkY: Int): Long = (chunkX.toLong() shl 32) xor (chunkY.toLong() and 0xffffffffL)

        private fun unpackChunkX(key: Long): Int = (key shr 32).toInt()

        private fun unpackChunkY(key: Long): Int = key.toInt()

        private fun requestControlledShutdown() {
            if (!shutdownRequested.compareAndSet(false, true)) {
                return
            }
            logger.info("Update window elapsed with no active players; requesting controlled server shutdown.")
            GameLoopService.requestShutdown("update-window-complete")
        }

        private val shutdownRequested = java.util.concurrent.atomic.AtomicBoolean(false)
        private val chunkIndexMissingLogged = java.util.concurrent.atomic.AtomicBoolean(false)
    }
}
