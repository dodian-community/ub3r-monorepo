package net.dodian.uber.game.runtime.sync.cache

import java.util.IdentityHashMap
import net.dodian.uber.game.model.entity.npc.Npc

class SharedNpcBlockCache {
    private val blocks = IdentityHashMap<Npc, ByteArray>()

    fun put(npc: Npc, bytes: ByteArray) {
        blocks[npc] = bytes
    }

    fun get(npc: Npc): ByteArray? = blocks[npc]
}
