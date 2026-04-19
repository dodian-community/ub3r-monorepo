package net.dodian.uber.game.engine.sync.viewport

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Player

data class ViewportSnapshot(
    val players: List<Player>,
    val npcs: List<Npc>,
)
