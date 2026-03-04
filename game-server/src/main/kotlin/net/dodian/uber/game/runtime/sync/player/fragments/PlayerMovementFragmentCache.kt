package net.dodian.uber.game.runtime.sync.player.fragments

import java.util.IdentityHashMap
import net.dodian.uber.game.model.entity.player.Player

class PlayerMovementFragmentCache {
    private val localFragments = IdentityHashMap<Player, PlayerLocalMovementFragment>()
    private val selfFragments = IdentityHashMap<Player, PlayerSelfMovementFragment>()

    fun local(player: Player): PlayerLocalMovementFragment? = localFragments[player]

    fun self(player: Player): PlayerSelfMovementFragment? = selfFragments[player]

    fun putLocal(player: Player, fragment: PlayerLocalMovementFragment) {
        localFragments[player] = fragment
    }

    fun putSelf(player: Player, fragment: PlayerSelfMovementFragment) {
        selfFragments[player] = fragment
    }

    fun clear() {
        localFragments.clear()
        selfFragments.clear()
    }
}
