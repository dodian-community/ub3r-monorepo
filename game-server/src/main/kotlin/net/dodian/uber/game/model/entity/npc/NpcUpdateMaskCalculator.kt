package net.dodian.uber.game.model.entity.npc

import net.dodian.uber.game.model.entity.UpdateFlag

object NpcUpdateMaskCalculator {
    @JvmStatic
    fun computeMask(npc: Npc): Int {
        var updateMask = 0
        for (flag in UpdateFlag.VALUES) {
            if (!npc.updateFlags.isRequired(flag)) continue
            try {
                updateMask = updateMask or flag.getMask(npc.type)
            } catch (_: IllegalStateException) {
                // Some player-only flags are intentionally unsupported by the NPC protocol.
                // Ignore them so malformed content cannot disconnect a viewer with an empty
                // or impossible NPC update block.
            }
        }
        return updateMask
    }
}
