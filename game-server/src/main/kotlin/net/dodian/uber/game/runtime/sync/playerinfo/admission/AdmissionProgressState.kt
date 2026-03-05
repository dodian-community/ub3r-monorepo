package net.dodian.uber.game.runtime.sync.playerinfo.admission

data class AdmissionProgressState(
    val totalPending: Int,
    val sentCount: Int,
    val deferredCount: Int,
)
