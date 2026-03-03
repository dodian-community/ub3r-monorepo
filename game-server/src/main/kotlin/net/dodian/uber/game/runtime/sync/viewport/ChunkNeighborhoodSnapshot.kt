package net.dodian.uber.game.runtime.sync.viewport

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Player

data class ChunkNeighborhoodSnapshot(
    val players: List<Player>,
    val npcs: List<Npc>,
)
