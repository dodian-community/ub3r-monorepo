package net.dodian.uber.game.engine.systems.skills.farming.runtime

import com.google.gson.JsonArray
import net.dodian.uber.game.content.skills.farming.FarmingData
import net.dodian.uber.game.content.skills.farming.FarmingState
import net.dodian.uber.game.model.entity.player.Client

object FarmingPersistenceCodec {
    private const val SLOT_ITEM = 0
    private const val SLOT_STATE = 1
    private const val SLOT_COMPOST = 2
    private const val SLOT_STAGE = 3
    private const val SLOT_PROGRESS = 4
    private const val SLOT_PLANTED_BY = 5

    fun snapshot(client: Client): PlayerFarmingSnapshot {
        val state = client.farmingJson
        val patches = mutableListOf<PatchSlotSnapshot>()
        for (patch in FarmingData.patches.values()) {
            val patchArray = state.getPatchData().get(patch.name)?.asJsonArray ?: continue
            for (slot in patch.objectId.indices) {
                val offset = slot * state.PATCHAMOUNT
                patches +=
                    PatchSlotSnapshot(
                        patch = patch,
                        slot = slot,
                        itemId = patchArray.readInt(offset + SLOT_ITEM),
                        state = patchArray.readString(offset + SLOT_STATE),
                        compost = patchArray.readString(offset + SLOT_COMPOST),
                        stageOrLife = patchArray.readInt(offset + SLOT_STAGE),
                        progress = patchArray.readInt(offset + SLOT_PROGRESS),
                        plantedBy = patchArray.readInt(offset + SLOT_PLANTED_BY),
                    )
            }
        }

        val compostBins = mutableListOf<CompostBinSnapshot>()
        for (bin in FarmingData.compostBin.values()) {
            val compostArray = state.getCompostData().get(bin.name)?.asJsonArray ?: continue
            compostBins +=
                CompostBinSnapshot(
                    bin = bin,
                    compost = compostArray.readString(0),
                    state = compostArray.readString(1),
                    amount = compostArray.readInt(2),
                    progress = compostArray.readInt(3),
                )
        }

        return PlayerFarmingSnapshot(
            patchSlots = patches,
            compostBins = compostBins,
            hasPendingSaplings = hasPendingSaplings(client),
        )
    }

    fun writePatchSlot(
        farmingState: FarmingState,
        patch: FarmingData.patches,
        slot: Int,
        slotSnapshot: PatchSlotSnapshot,
    ) {
        val patchArray = farmingState.getPatchData().get(patch.name)?.asJsonArray ?: return
        val offset = slot * farmingState.PATCHAMOUNT
        patchArray.writeInt(offset + SLOT_ITEM, slotSnapshot.itemId)
        patchArray.writeString(offset + SLOT_STATE, slotSnapshot.state)
        patchArray.writeString(offset + SLOT_COMPOST, slotSnapshot.compost)
        patchArray.writeInt(offset + SLOT_STAGE, slotSnapshot.stageOrLife)
        patchArray.writeInt(offset + SLOT_PROGRESS, slotSnapshot.progress)
        patchArray.writeInt(offset + SLOT_PLANTED_BY, slotSnapshot.plantedBy)
    }

    fun writeCompostBin(
        farmingState: FarmingState,
        bin: FarmingData.compostBin,
        snapshot: CompostBinSnapshot,
    ) {
        val compostArray = farmingState.getCompostData().get(bin.name)?.asJsonArray ?: return
        compostArray.writeString(0, snapshot.compost)
        compostArray.writeString(1, snapshot.state)
        compostArray.writeInt(2, snapshot.amount)
        compostArray.writeInt(3, snapshot.progress)
    }

    private fun hasPendingSaplings(client: Client): Boolean {
        val wateredIds = FarmingData.sapling.values().mapTo(HashSet()) { it.waterId + 1 }
        for (item in client.playerItems) {
            if (item in wateredIds) return true
        }
        for (item in client.bankItems) {
            if (item in wateredIds) return true
        }
        return false
    }

    private fun JsonArray.readInt(index: Int): Int =
        if (index in 0 until size()) get(index).asInt else 0

    private fun JsonArray.readString(index: Int): String =
        if (index in 0 until size()) get(index).asString else ""

    private fun JsonArray.writeInt(index: Int, value: Int) {
        set(index, com.google.gson.JsonPrimitive(value))
    }

    private fun JsonArray.writeString(index: Int, value: String) {
        set(index, com.google.gson.JsonPrimitive(value))
    }
}
