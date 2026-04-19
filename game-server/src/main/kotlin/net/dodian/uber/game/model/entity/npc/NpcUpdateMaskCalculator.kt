package net.dodian.uber.game.model.entity.npc

import net.dodian.uber.game.model.entity.UpdateFlag

object NpcUpdateMaskCalculator {
    @JvmStatic
    fun computeMask(npc: Npc): Int {
        var updateMask = 0
        if (npc.updateFlags.isRequired(UpdateFlag.ANIM)) updateMask = updateMask or UpdateFlag.ANIM.getMask(npc.type)
        if (npc.updateFlags.isRequired(UpdateFlag.GRAPHICS)) updateMask = updateMask or UpdateFlag.GRAPHICS.getMask(npc.type)
        if (npc.updateFlags.isRequired(UpdateFlag.HIT2)) updateMask = updateMask or UpdateFlag.HIT2.getMask(npc.type)
        if (npc.updateFlags.isRequired(UpdateFlag.FACE_CHARACTER)) updateMask = updateMask or UpdateFlag.FACE_CHARACTER.getMask(npc.type)
        if (npc.updateFlags.isRequired(UpdateFlag.FORCED_CHAT)) updateMask = updateMask or UpdateFlag.FORCED_CHAT.getMask(npc.type)
        if (npc.updateFlags.isRequired(UpdateFlag.HIT)) updateMask = updateMask or UpdateFlag.HIT.getMask(npc.type)
        if (npc.updateFlags.isRequired(UpdateFlag.APPEARANCE)) updateMask = updateMask or UpdateFlag.APPEARANCE.getMask(npc.type)
        if (npc.updateFlags.isRequired(UpdateFlag.FACE_COORDINATE)) updateMask = updateMask or UpdateFlag.FACE_COORDINATE.getMask(npc.type)
        return updateMask
    }
}

