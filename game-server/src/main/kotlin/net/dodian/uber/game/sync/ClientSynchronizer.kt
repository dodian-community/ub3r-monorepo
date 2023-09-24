package net.dodian.uber.game.sync

import net.dodian.uber.game.modelkt.entity.Npc
import net.dodian.uber.game.modelkt.entity.player.Player

abstract class ClientSynchronizer {
    abstract fun synchronize(players: Iterable<Player>, npcs: Iterable<Npc>)
}