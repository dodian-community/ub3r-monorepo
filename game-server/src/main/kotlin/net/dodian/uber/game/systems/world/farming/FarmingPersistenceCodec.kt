package net.dodian.uber.game.systems.world.farming

import net.dodian.uber.game.content.skills.farming.FarmingData
import net.dodian.uber.game.content.skills.farming.FarmingState
import net.dodian.uber.game.model.entity.player.Client

@Deprecated(
    message = "Use net.dodian.uber.game.systems.skills.farming.runtime.FarmingPersistenceCodec",
    replaceWith = ReplaceWith("net.dodian.uber.game.systems.skills.farming.runtime.FarmingPersistenceCodec"),
)
object FarmingPersistenceCodec {
    @JvmStatic
    fun snapshot(client: Client): PlayerFarmingSnapshot =
        net.dodian.uber.game.systems.skills.farming.runtime.FarmingPersistenceCodec.snapshot(client)

    @JvmStatic
    fun writePatchSlot(
        farmingState: FarmingState,
        patch: FarmingData.patches,
        slot: Int,
        slotSnapshot: PatchSlotSnapshot,
    ) {
        net.dodian.uber.game.systems.skills.farming.runtime.FarmingPersistenceCodec.writePatchSlot(
            farmingState = farmingState,
            patch = patch,
            slot = slot,
            slotSnapshot = slotSnapshot,
        )
    }

    @JvmStatic
    fun writeCompostBin(
        farmingState: FarmingState,
        bin: FarmingData.compostBin,
        snapshot: CompostBinSnapshot,
    ) {
        net.dodian.uber.game.systems.skills.farming.runtime.FarmingPersistenceCodec.writeCompostBin(
            farmingState = farmingState,
            bin = bin,
            snapshot = snapshot,
        )
    }
}
