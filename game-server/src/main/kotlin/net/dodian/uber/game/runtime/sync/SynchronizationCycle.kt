package net.dodian.uber.game.runtime.sync

import java.util.EnumMap
import net.dodian.uber.game.runtime.sync.cache.RootSynchronizationCache
import net.dodian.uber.game.runtime.sync.npc.NpcChunkActivityIndex
import net.dodian.uber.game.runtime.sync.npc.RootNpcDeltaIndex
import net.dodian.uber.game.runtime.sync.player.PlayerChunkActivityIndex
import net.dodian.uber.game.runtime.sync.player.PlayerSyncRevisionIndex
import net.dodian.uber.game.runtime.sync.playerinfo.dispatch.PlayerPacketBuildReason
import net.dodian.uber.game.runtime.sync.playerinfo.dispatch.PlayerPacketMode
import net.dodian.uber.game.runtime.sync.playerinfo.dispatch.PlayerSyncRecoveryReason
import net.dodian.uber.game.runtime.sync.playerinfo.dispatch.PlayerPacketSkipReason
import net.dodian.uber.game.runtime.sync.viewport.ViewportIndex
import net.dodian.uber.game.runtime.sync.template.PlayerSyncTemplateCache

class SynchronizationCycle(
    val tick: Long,
    val rootCache: RootSynchronizationCache,
    val viewportIndex: ViewportIndex?,
    val playerRevisionIndex: PlayerSyncRevisionIndex? = null,
    val playerActivityIndex: PlayerChunkActivityIndex? = null,
    val npcRevisionIndex: RootNpcDeltaIndex? = null,
    val npcActivityIndex: NpcChunkActivityIndex? = null,
    val playerTemplateCache: PlayerSyncTemplateCache = PlayerSyncTemplateCache(),
) {
    private val stageDurationsNanos = EnumMap<SynchronizationStage, Long>(SynchronizationStage::class.java)
    private val playerPacketModes = EnumMap<PlayerPacketMode, Int>(PlayerPacketMode::class.java)
    private val playerBuildReasons = EnumMap<PlayerPacketBuildReason, Int>(PlayerPacketBuildReason::class.java)
    private val playerSkipReasons = EnumMap<PlayerPacketSkipReason, Int>(PlayerPacketSkipReason::class.java)

    var viewersEncoded: Int = 0
        private set
    var localPlayersWritten: Int = 0
        private set
    var localNpcsWritten: Int = 0
        private set
    var playerAddCount: Int = 0
        private set
    var npcAddCount: Int = 0
        private set
    var playerBlockCacheHits: Int = 0
        private set
    var playerBlockCacheMisses: Int = 0
        private set
    var playerBlockCacheEligible: Int = 0
        private set
    var npcBlockCacheHits: Int = 0
        private set
    var npcBlockCacheMisses: Int = 0
        private set
    var playerPacketsBuilt: Int = 0
        private set
    var playerPacketsSkipped: Int = 0
        private set
    var playerPacketsTemplated: Int = 0
        private set
    var playerScratchReuseCount: Int = 0
        private set
    var playerAppearanceCacheHits: Int = 0
        private set
    var playerAppearanceCacheMisses: Int = 0
        private set
    var playerLocalScans: Int = 0
        private set
    var playerLocalsSkipped: Int = 0
        private set
    var playerTemplatedLocalCoverage: Int = 0
        private set
    var playerSelfOnlyCount: Int = 0
        private set
    var playerIncrementalSteadyCount: Int = 0
        private set
    var playerIncrementalAdmissionCount: Int = 0
        private set
    var playerFullRebuildCount: Int = 0
        private set
    var playerVisibleDiffAdds: Int = 0
        private set
    var playerVisibleDiffRemovals: Int = 0
        private set
    var playerRetainedLocalChangedCount: Int = 0
        private set
    var playerPendingAddCount: Int = 0
        private set
    var playerPendingAddBacklogMax: Int = 0
        private set
    var playerDesiredLocalCount: Int = 0
        private set
    var playerDesiredLocalSaturatedCount: Int = 0
        private set
    var playerLocalRemovalCount: Int = 0
        private set
    var playerLocalAdditionSentCount: Int = 0
        private set
    var playerLocalAdditionDeferredCount: Int = 0
        private set
    var playerHardRebuildRecoveryCount: Int = 0
        private set
    var npcPacketsBuilt: Int = 0
        private set
    var npcPacketsSkipped: Int = 0
        private set
    var npcLocalScans: Int = 0
        private set
    var npcLocalsSkipped: Int = 0
        private set
    var npcBuildNoStateCount: Int = 0
        private set
    var npcBuildMapRegionOrTeleportCount: Int = 0
        private set
    var npcBuildLocalCountChangedCount: Int = 0
        private set
    var npcBuildPendingViewportCount: Int = 0
        private set
    var npcBuildChunkActivityChangedCount: Int = 0
        private set
    var npcBuildLocalActivityChangedCount: Int = 0
        private set

    fun recordStage(stage: SynchronizationStage, durationNanos: Long) {
        stageDurationsNanos[stage] = (stageDurationsNanos[stage] ?: 0L) + durationNanos
    }

    fun stageDurationNanos(stage: SynchronizationStage): Long = stageDurationsNanos[stage] ?: 0L

    fun recordViewer(localPlayers: Int, localNpcs: Int) {
        viewersEncoded++
        localPlayersWritten += localPlayers
        localNpcsWritten += localNpcs
    }

    fun recordPlayerAdd() {
        playerAddCount++
    }

    fun recordNpcAdd() {
        npcAddCount++
    }

    fun recordPlayerBlockCacheHit(hit: Boolean) {
        playerBlockCacheEligible++
        if (hit) {
            playerBlockCacheHits++
        } else {
            playerBlockCacheMisses++
        }
    }

    fun recordNpcBlockCacheHit(hit: Boolean) {
        if (hit) {
            npcBlockCacheHits++
        } else {
            npcBlockCacheMisses++
        }
    }

    fun recordPlayerPacketBuilt(localCount: Int) {
        playerPacketsBuilt++
        playerLocalScans += localCount
    }

    fun recordPlayerPacketSkipped(localCount: Int) {
        playerPacketsSkipped++
        playerLocalsSkipped += localCount
    }

    fun recordPlayerPacketTemplated(localCount: Int) {
        playerPacketsTemplated++
        playerTemplatedLocalCoverage += localCount
    }

    fun recordPlayerScratchReuse() {
        playerScratchReuseCount++
    }

    fun recordPlayerAppearanceCacheHit(hit: Boolean) {
        if (hit) {
            playerAppearanceCacheHits++
        } else {
            playerAppearanceCacheMisses++
        }
    }

    fun recordNpcPacketBuilt(localCount: Int) {
        npcPacketsBuilt++
        npcLocalScans += localCount
    }

    fun recordNpcPacketSkipped(localCount: Int) {
        npcPacketsSkipped++
        npcLocalsSkipped += localCount
    }

    fun recordNpcBuildNoState() {
        npcBuildNoStateCount++
    }

    fun recordNpcBuildMapRegionOrTeleport() {
        npcBuildMapRegionOrTeleportCount++
    }

    fun recordNpcBuildLocalCountChanged() {
        npcBuildLocalCountChangedCount++
    }

    fun recordNpcBuildPendingViewport() {
        npcBuildPendingViewportCount++
    }

    fun recordNpcBuildChunkActivityChanged() {
        npcBuildChunkActivityChangedCount++
    }

    fun recordNpcBuildLocalActivityChanged() {
        npcBuildLocalActivityChangedCount++
    }

    fun recordPlayerPacketMode(mode: PlayerPacketMode) {
        playerPacketModes[mode] = (playerPacketModes[mode] ?: 0) + 1
        when (mode) {
            PlayerPacketMode.SELF_ONLY -> playerSelfOnlyCount++
            PlayerPacketMode.INCREMENTAL_STEADY -> playerIncrementalSteadyCount++
            PlayerPacketMode.INCREMENTAL_ADMISSION -> playerIncrementalAdmissionCount++
            PlayerPacketMode.FULL_REBUILD -> playerFullRebuildCount++
            PlayerPacketMode.SKIP -> Unit
        }
    }

    fun recordPlayerBuildReason(reason: PlayerPacketBuildReason) {
        playerBuildReasons[reason] = (playerBuildReasons[reason] ?: 0) + 1
    }

    fun recordPlayerSkipReason(reason: PlayerPacketSkipReason) {
        playerSkipReasons[reason] = (playerSkipReasons[reason] ?: 0) + 1
    }

    fun recordPlayerVisibleDiff(adds: Int, removals: Int) {
        playerVisibleDiffAdds += adds
        playerVisibleDiffRemovals += removals
    }

    fun recordPlayerRetainedLocalChanged(count: Int) {
        playerRetainedLocalChangedCount += count
    }

    fun recordPlayerPendingAdds(totalPending: Int, deferred: Int) {
        playerPendingAddCount += totalPending
        playerPendingAddBacklogMax = maxOf(playerPendingAddBacklogMax, deferred)
    }

    fun recordPlayerDesiredLocals(count: Int, saturated: Boolean) {
        playerDesiredLocalCount += count
        if (saturated) {
            playerDesiredLocalSaturatedCount++
        }
    }

    fun recordPlayerLocalRemovalCount(count: Int) {
        playerLocalRemovalCount += count
    }

    fun recordPlayerLocalAdditionSent(count: Int) {
        playerLocalAdditionSentCount += count
    }

    fun recordPlayerLocalAdditionDeferred(count: Int) {
        playerLocalAdditionDeferredCount += count
    }

    fun recordPlayerRecovery(reason: PlayerSyncRecoveryReason) {
        if (reason == PlayerSyncRecoveryReason.CURRENT_LOCAL_MISMATCH ||
            reason == PlayerSyncRecoveryReason.LOCAL_COUNT_OUT_OF_RANGE ||
            reason == PlayerSyncRecoveryReason.DUPLICATE_LOCAL ||
            reason == PlayerSyncRecoveryReason.PENDING_ALREADY_LOCAL ||
            reason == PlayerSyncRecoveryReason.REGION_STATE_MISMATCH ||
            reason == PlayerSyncRecoveryReason.STALE_LOCAL
        ) {
            playerHardRebuildRecoveryCount++
        }
    }

    fun playerPacketModes(): Map<PlayerPacketMode, Int> = playerPacketModes

    fun playerBuildReasons(): Map<PlayerPacketBuildReason, Int> = playerBuildReasons

    fun playerSkipReasons(): Map<PlayerPacketSkipReason, Int> = playerSkipReasons
}
