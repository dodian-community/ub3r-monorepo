package net.dodian.uber.game.runtime.sync.player.fragments

import java.util.IdentityHashMap
import net.dodian.uber.game.model.entity.player.Player

class PlayerBlockFragmentCache {
    private val updateLocalBlocks = IdentityHashMap<Player, ByteArray>()
    private val addLocalBlocks = IdentityHashMap<Player, ByteArray>()

    fun updateLocal(player: Player): ByteArray? = updateLocalBlocks[player]

    fun addLocal(player: Player): ByteArray? = addLocalBlocks[player]

    fun putUpdateLocal(player: Player, bytes: ByteArray) {
        updateLocalBlocks[player] = bytes
    }

    fun putAddLocal(player: Player, bytes: ByteArray) {
        addLocalBlocks[player] = bytes
    }

    fun clear() {
        updateLocalBlocks.clear()
        addLocalBlocks.clear()
    }
}
