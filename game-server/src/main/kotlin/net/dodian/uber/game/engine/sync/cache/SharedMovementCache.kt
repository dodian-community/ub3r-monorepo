package net.dodian.uber.game.engine.sync.cache

import net.dodian.uber.game.engine.config.gameMaxPlayers
import net.dodian.uber.game.engine.sync.protocol.PackedBitSlice
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.npc.NpcUpdating
import net.dodian.uber.game.model.entity.player.Player

class SharedPlayerMovementCache {
    fun put(player: Player, movement: PackedBitSlice, tick: Long) {
        val slot = player.slot
        blocks[slot] = movement
        owners[slot] = player
        ticks[slot] = tick
    }

    fun get(player: Player, tick: Long): PackedBitSlice? {
        val slot = player.slot
        return if (ticks[slot] == tick && owners[slot] === player) blocks[slot] else null
    }

    private companion object {
        private val blocks = arrayOfNulls<PackedBitSlice>(gameMaxPlayers + 1)
        private val owners = arrayOfNulls<Player>(gameMaxPlayers + 1)
        private val ticks = LongArray(gameMaxPlayers + 1) { -1L }
    }
}

class SharedNpcMovementCache {
    fun put(npc: Npc, movement: PackedBitSlice, tick: Long) {
        val slot = npc.slot
        blocks[slot] = movement
        owners[slot] = npc
        ticks[slot] = tick
    }

    fun get(npc: Npc, tick: Long): PackedBitSlice? {
        val slot = npc.slot
        return if (ticks[slot] == tick && owners[slot] === npc) blocks[slot] else null
    }

    private companion object {
        private val capacity = 1 shl NpcUpdating.NPC_SLOT_BITS
        private val blocks = arrayOfNulls<PackedBitSlice>(capacity)
        private val owners = arrayOfNulls<Npc>(capacity)
        private val ticks = LongArray(capacity) { -1L }
    }
}
