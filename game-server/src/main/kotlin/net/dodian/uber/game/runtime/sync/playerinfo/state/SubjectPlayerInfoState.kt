package net.dodian.uber.game.runtime.sync.playerinfo.state

data class SubjectPlayerInfoState(
    val slot: Int,
    val movementRevision: Long,
    val blockRevision: Long,
)
