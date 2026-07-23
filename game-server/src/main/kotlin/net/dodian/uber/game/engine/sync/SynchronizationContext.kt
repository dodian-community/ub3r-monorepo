package net.dodian.uber.game.engine.sync

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.engine.sync.viewport.ViewportSnapshot
import net.dodian.uber.game.engine.sync.protocol.PackedUpdateBlock
import net.dodian.uber.game.engine.sync.protocol.PackedBitSlice

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
    fun getSharedPlayerBlock(player: Player, phase: String): PackedUpdateBlock? =
        current()?.let { cycle -> cycle.rootCache.playerBlocks.get(player, phase, cycle.tick) }

    @JvmStatic
    fun getSharedNpcBlock(npc: Npc): PackedUpdateBlock? =
        current()?.let { cycle -> cycle.rootCache.npcBlocks.get(npc, cycle.tick) }

    @JvmStatic
    fun getSharedPlayerMovement(player: Player): PackedBitSlice? =
        current()?.let { cycle -> cycle.rootCache.playerMovements.get(player, cycle.tick) }

    @JvmStatic
    fun getSharedNpcMovement(npc: Npc): PackedBitSlice? =
        current()?.let { cycle -> cycle.rootCache.npcMovements.get(npc, cycle.tick) }

    @JvmStatic
    fun getViewportSnapshot(viewer: Player): ViewportSnapshot? = current()?.viewportIndex?.snapshotFor(viewer)

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
}
