package net.dodian.uber.game.engine.sync.cache

import net.dodian.uber.game.engine.config.gameMaxPlayers
import net.dodian.uber.game.engine.sync.protocol.PackedUpdateBlock
import net.dodian.uber.game.model.entity.player.Player

/**
 * Slot-indexed like rsprot's PlayerInfoRepository: one persistent array per phase instead of a
 * per-tick IdentityHashMap. The array is never cleared, so a parallel tick-stamp guards against
 * staleness — encode() is called both to build a slot (during PREP) and to reuse it (during
 * per-viewer ENCODE later in the same tick), and the PREP call must never treat a leftover value
 * from an earlier tick as already-built. A slot only counts as a hit when its stamp matches the
 * tick that's asking.
 */
class SharedPlayerBlockCache {
    fun put(player: Player, phase: String, block: PackedUpdateBlock, tick: Long) {
        val slot = player.getSlot()
        when (phase) {
            "ADD_LOCAL" -> {
                addLocalBlocks[slot] = block
                addLocalOwners[slot] = player
                addLocalTicks[slot] = tick
            }
            "UPDATE_LOCAL" -> {
                updateLocalBlocks[slot] = block
                updateLocalOwners[slot] = player
                updateLocalTicks[slot] = tick
            }
            else -> throw IllegalArgumentException("Unsupported shared player block phase: $phase")
        }
    }

    fun get(player: Player, phase: String, tick: Long): PackedUpdateBlock? {
        val slot = player.getSlot()
        return when (phase) {
            "ADD_LOCAL" ->
                if (addLocalTicks[slot] == tick && addLocalOwners[slot] === player) addLocalBlocks[slot] else null
            "UPDATE_LOCAL" ->
                if (updateLocalTicks[slot] == tick && updateLocalOwners[slot] === player) updateLocalBlocks[slot] else null
            else -> throw IllegalArgumentException("Unsupported shared player block phase: $phase")
        }
    }

    private companion object {
        private val addLocalBlocks = arrayOfNulls<PackedUpdateBlock>(gameMaxPlayers + 1)
        private val updateLocalBlocks = arrayOfNulls<PackedUpdateBlock>(gameMaxPlayers + 1)
        private val addLocalOwners = arrayOfNulls<Player>(gameMaxPlayers + 1)
        private val updateLocalOwners = arrayOfNulls<Player>(gameMaxPlayers + 1)
        private val addLocalTicks = LongArray(gameMaxPlayers + 1) { -1L }
        private val updateLocalTicks = LongArray(gameMaxPlayers + 1) { -1L }
    }
}
