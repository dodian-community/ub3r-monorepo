package net.dodian.uber.game.runtime.sync

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.runtime.sync.npc.ViewerNpcSyncState
import net.dodian.uber.game.runtime.sync.player.ViewerPlayerSyncState
import net.dodian.uber.game.runtime.sync.playerinfo.dispatch.PlayerPacketBuildReason
import net.dodian.uber.game.runtime.sync.playerinfo.dispatch.PlayerPacketMode
import net.dodian.uber.game.runtime.sync.playerinfo.dispatch.PlayerSyncRecoveryReason
import net.dodian.uber.game.runtime.sync.playerinfo.dispatch.PlayerPacketSkipReason
import net.dodian.uber.game.runtime.sync.template.PlayerSyncTemplate
import net.dodian.uber.game.runtime.sync.template.PlayerSyncTemplateKey
import net.dodian.uber.game.runtime.sync.viewport.ViewportSnapshot

object SynchronizationContext {
    private val cycleHolder = ThreadLocal<SynchronizationCycle?>()

    @JvmStatic
    fun setCurrent(cycle: SynchronizationCycle) {
        cycleHolder.set(cycle)
    }

    @JvmStatic
    fun clear() {
        cycleHolder.remove()
    }

    @JvmStatic
    fun current(): SynchronizationCycle? = cycleHolder.get()

    @JvmStatic
    fun getSharedPlayerBlock(player: Player, phase: String): ByteArray? =
        current()?.rootCache?.playerBlocks?.get(player, phase)

    @JvmStatic
    fun getSharedNpcBlock(npc: Npc): ByteArray? = current()?.rootCache?.npcBlocks?.get(npc)

    @JvmStatic
    fun getViewportSnapshot(viewer: Player): ViewportSnapshot? = current()?.viewportIndex?.snapshotFor(viewer)

    @JvmStatic
    fun getPlayerChunkActivityStamp(viewer: Player): Long =
        current()?.playerActivityIndex?.maxChunkActivityStampFor(viewer, 16) ?: 0L

    @JvmStatic
    fun getPlayerLocalActivityStamp(viewer: Player): Long =
        current()?.playerRevisionIndex?.localActivityStamp(viewer) ?: 0L

    @JvmStatic
    fun getPlayerMovementRevision(player: Player): Long = current()?.playerRevisionIndex?.movementRevision(player) ?: 0L

    @JvmStatic
    fun getPlayerBlockRevision(player: Player): Long = current()?.playerRevisionIndex?.blockRevision(player) ?: 0L

    @JvmStatic
    fun getViewerPlayerSyncState(viewer: Player): ViewerPlayerSyncState? = current()?.playerRevisionIndex?.viewerState(viewer)

    @JvmStatic
    fun getNpcChunkActivityStamp(viewer: net.dodian.uber.game.model.entity.player.Client): Long =
        current()?.npcActivityIndex?.maxChunkActivityStampFor(viewer, 16) ?: 0L

    @JvmStatic
    fun getNpcLocalActivityStamp(viewer: net.dodian.uber.game.model.entity.player.Client): Long =
        current()?.npcRevisionIndex?.localActivityStamp(viewer) ?: 0L

    @JvmStatic
    fun getViewerNpcSyncState(viewer: net.dodian.uber.game.model.entity.player.Client): ViewerNpcSyncState? =
        current()?.npcRevisionIndex?.viewerState(viewer)

    @JvmStatic
    fun getPlayerTemplate(key: PlayerSyncTemplateKey): PlayerSyncTemplate? = current()?.playerTemplateCache?.get(key)

    @JvmStatic
    fun putPlayerTemplate(key: PlayerSyncTemplateKey, template: PlayerSyncTemplate) {
        current()?.playerTemplateCache?.put(key, template)
    }

    @JvmStatic
    fun recordViewer(localPlayers: Int, localNpcs: Int) {
        current()?.recordViewer(localPlayers, localNpcs)
    }

    @JvmStatic
    fun recordPlayerAdd() {
        current()?.recordPlayerAdd()
    }

    @JvmStatic
    fun recordNpcAdd() {
        current()?.recordNpcAdd()
    }

    @JvmStatic
    fun recordPlayerBlockCacheHit(hit: Boolean) {
        current()?.recordPlayerBlockCacheHit(hit)
    }

    @JvmStatic
    fun recordNpcBlockCacheHit(hit: Boolean) {
        current()?.recordNpcBlockCacheHit(hit)
    }

    @JvmStatic
    fun recordPlayerPacketBuilt(localCount: Int) {
        current()?.recordPlayerPacketBuilt(localCount)
    }

    @JvmStatic
    fun recordPlayerPacketSkipped(localCount: Int) {
        current()?.recordPlayerPacketSkipped(localCount)
    }

    @JvmStatic
    fun recordPlayerPacketTemplated(localCount: Int) {
        current()?.recordPlayerPacketTemplated(localCount)
    }

    @JvmStatic
    fun recordPlayerPacketMode(mode: PlayerPacketMode) {
        current()?.recordPlayerPacketMode(mode)
    }

    @JvmStatic
    fun recordPlayerBuildReason(reason: PlayerPacketBuildReason) {
        current()?.recordPlayerBuildReason(reason)
    }

    @JvmStatic
    fun recordPlayerSkipReason(reason: PlayerPacketSkipReason) {
        current()?.recordPlayerSkipReason(reason)
    }

    @JvmStatic
    fun recordPlayerVisibleDiff(adds: Int, removals: Int) {
        current()?.recordPlayerVisibleDiff(adds, removals)
    }

    @JvmStatic
    fun recordPlayerRetainedLocalChanged(count: Int) {
        current()?.recordPlayerRetainedLocalChanged(count)
    }

    @JvmStatic
    fun recordPlayerPendingAdds(totalPending: Int, deferred: Int) {
        current()?.recordPlayerPendingAdds(totalPending, deferred)
    }

    @JvmStatic
    fun recordPlayerDesiredLocals(count: Int, saturated: Boolean) {
        current()?.recordPlayerDesiredLocals(count, saturated)
    }

    @JvmStatic
    fun recordPlayerLocalRemovalCount(count: Int) {
        current()?.recordPlayerLocalRemovalCount(count)
    }

    @JvmStatic
    fun recordPlayerLocalAdditionSent(count: Int) {
        current()?.recordPlayerLocalAdditionSent(count)
    }

    @JvmStatic
    fun recordPlayerLocalAdditionDeferred(count: Int) {
        current()?.recordPlayerLocalAdditionDeferred(count)
    }

    @JvmStatic
    fun recordPlayerRecovery(reason: PlayerSyncRecoveryReason) {
        current()?.recordPlayerRecovery(reason)
    }

    @JvmStatic
    fun recordPlayerScratchReuse() {
        current()?.recordPlayerScratchReuse()
    }

    @JvmStatic
    fun recordPlayerAppearanceCacheHit(hit: Boolean) {
        current()?.recordPlayerAppearanceCacheHit(hit)
    }

    @JvmStatic
    fun recordNpcPacketBuilt(localCount: Int) {
        current()?.recordNpcPacketBuilt(localCount)
    }

    @JvmStatic
    fun recordNpcPacketSkipped(localCount: Int) {
        current()?.recordNpcPacketSkipped(localCount)
    }
}
