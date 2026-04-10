package net.dodian.uber.game.engine.sync.playerinfo.state

data class SubjectPlayerInfoState(
    val slot: Int,
    val movementRevision: Long,
    val blockRevision: Long,
)
