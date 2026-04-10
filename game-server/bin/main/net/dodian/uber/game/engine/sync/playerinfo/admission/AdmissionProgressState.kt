package net.dodian.uber.game.engine.sync.playerinfo.admission

data class AdmissionProgressState(
    val totalPending: Int,
    val sentCount: Int,
    val deferredCount: Int,
)
