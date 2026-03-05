package net.dodian.uber.game.runtime.sync.playerinfo.admission

data class LocalAdmissionBatch(
    val sentSlots: IntArray,
    val pendingCount: Int,
    val progress: AdmissionProgressState,
)
