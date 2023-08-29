package net.dodian.uber.game.sync

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Player

class ParallelClientSynchronizer : ClientSynchronizer() {
    override fun synchronize(players: Iterable<Player>, npcs: Iterable<Npc>) {

    }
}