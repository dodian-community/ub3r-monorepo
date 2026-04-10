package net.dodian.uber.game.engine.sync.cache

import java.util.IdentityHashMap
import net.dodian.uber.game.model.entity.player.Player

class SharedPlayerBlockCache {
    private val blocks = IdentityHashMap<Player, MutableMap<String, ByteArray>>()

    fun put(player: Player, phase: String, bytes: ByteArray) {
        blocks.computeIfAbsent(player) { HashMap() }[phase] = bytes
    }

    fun get(player: Player, phase: String): ByteArray? = blocks[player]?.get(phase)
}
