package net.dodian.uber.game.runtime.sync

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Player
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
}
