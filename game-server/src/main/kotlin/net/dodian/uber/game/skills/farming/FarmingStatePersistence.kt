package net.dodian.uber.game.skills.farming

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.persistence.player.PlayerSaveSegment

fun Client.markFarmingDirty() {
    farmingJson.refreshSaveSnapshot()
    markSaveDirty(PlayerSaveSegment.FARMING.mask)
}
