package net.dodian.uber.game.persistence.v2

import net.dodian.uber.game.persistence.PlayerSaveSnapshot

data class PlayerSaveRequest(
    val envelope: PlayerSaveEnvelope,
    val shadowSnapshot: PlayerSaveSnapshot? = null,
    var attempts: Int = 0,
)
