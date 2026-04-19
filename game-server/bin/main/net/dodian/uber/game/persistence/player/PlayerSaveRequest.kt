package net.dodian.uber.game.persistence.player

import net.dodian.uber.game.persistence.player.PlayerSaveSnapshot

data class PlayerSaveRequest(
    val envelope: PlayerSaveEnvelope,
    val shadowSnapshot: PlayerSaveSnapshot? = null,
    var attempts: Int = 0,
)
