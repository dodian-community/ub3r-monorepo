package net.dodian.uber.game.engine.systems.skills.farming.runtime

import net.dodian.uber.game.content.skills.farming.FarmingData

data class PatchSlotSnapshot(
    val patch: FarmingData.patches,
    val slot: Int,
    val itemId: Int,
    val state: String,
    val compost: String,
    val stageOrLife: Int,
    val progress: Int,
    val plantedBy: Int,
)

data class CompostBinSnapshot(
    val bin: FarmingData.compostBin,
    val compost: String,
    val state: String,
    val amount: Int,
    val progress: Int,
)

data class PlayerFarmingSnapshot(
    val patchSlots: List<PatchSlotSnapshot>,
    val compostBins: List<CompostBinSnapshot>,
    val hasPendingSaplings: Boolean,
)

data class PatchSlotRuntimeState(
    var nextDueAtMillis: Long,
    var lastAppliedPulseAtMillis: Long,
    var state: String,
    var dirty: Boolean,
)

data class CompostBinRuntimeState(
    var nextDueAtMillis: Long,
    var lastAppliedPulseAtMillis: Long,
    var state: String,
    var dirty: Boolean,
)

data class SaplingRuntimeState(
    var nextDueAtMillis: Long,
    var lastAppliedPulseAtMillis: Long,
    var state: String,
    var dirty: Boolean,
)

data class PlayerFarmingRuntimeState(
    var nextDueAtMillis: Long,
    var lastAppliedPulseAtMillis: Long,
    var dirty: Boolean,
    val patchStates: MutableMap<String, PatchSlotRuntimeState>,
    val compostStates: MutableMap<String, CompostBinRuntimeState>,
    var saplingState: SaplingRuntimeState?,
)
