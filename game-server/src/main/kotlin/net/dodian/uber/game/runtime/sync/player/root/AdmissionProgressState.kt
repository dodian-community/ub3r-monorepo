package net.dodian.uber.game.runtime.sync.player.root

data class AdmissionProgressState(
    val totalPending: Int,
    val sentCount: Int,
    val deferredCount: Int,
)
