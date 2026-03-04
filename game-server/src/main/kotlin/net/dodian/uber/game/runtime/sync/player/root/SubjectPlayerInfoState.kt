package net.dodian.uber.game.runtime.sync.player.root

data class SubjectPlayerInfoState(
    val slot: Int,
    val movementRevision: Long,
    val blockRevision: Long,
)
