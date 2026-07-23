package net.dodian.uber.game.engine.sync.cache

import net.dodian.uber.game.engine.sync.protocol.PackedUpdateBlock
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.npc.NpcUpdating

/**
 * Slot-indexed like rsprot's NpcInfoRepository: one persistent array, sized to the protocol's
 * own NPC slot ceiling (2^NPC_SLOT_BITS), instead of a per-tick IdentityHashMap. See
 * SharedPlayerBlockCache for why the parallel tick-stamp array is required, not optional.
 */
class SharedNpcBlockCache {
    fun put(npc: Npc, block: PackedUpdateBlock, tick: Long) {
        val slot = npc.getSlot()
        blocks[slot] = block
        owners[slot] = npc
        ticks[slot] = tick
    }

    fun get(npc: Npc, tick: Long): PackedUpdateBlock? {
        val slot = npc.getSlot()
        return if (ticks[slot] == tick && owners[slot] === npc) blocks[slot] else null
    }

    private companion object {
        private val blocks = arrayOfNulls<PackedUpdateBlock>(1 shl NpcUpdating.NPC_SLOT_BITS)
        private val owners = arrayOfNulls<Npc>(1 shl NpcUpdating.NPC_SLOT_BITS)
        private val ticks = LongArray(1 shl NpcUpdating.NPC_SLOT_BITS) { -1L }
    }
}
