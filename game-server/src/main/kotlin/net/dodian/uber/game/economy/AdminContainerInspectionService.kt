package net.dodian.uber.game.economy

import net.dodian.uber.game.model.entity.player.Client

/** Staff-only online/offline container inspection boundary. */
object AdminContainerInspectionService {
    @JvmStatic fun openInventory(viewer: Client, playerName: String) = viewer.performOpenOtherInventory(playerName)
    @JvmStatic fun openBank(viewer: Client, playerName: String) = viewer.performOpenOtherBank(playerName)
}
