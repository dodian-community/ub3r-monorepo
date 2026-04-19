package net.dodian.uber.game.engine.sync.playerinfo.admission

data class LocalAdmissionBatch(
    val sentSlots: IntArray,
    val pendingCount: Int,
    val progress: AdmissionProgressState,
)
