package net.dodian.uber.game.runtime.sync.player.root

data class LocalAdmissionBatch(
    val sentSlots: IntArray,
    val pendingCount: Int,
    val progress: AdmissionProgressState,
)
