package net.dodian.uber.game.model.entity.npc;

import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.netty.codec.ByteMessage;

/**
 * Stateless Luna-style NPC update block encoder.
 */
final class NpcUpdateBlockSet {

    void encode(NpcUpdating updating, Npc npc, ByteMessage out) {
        int mask = computeMask(npc);
        if (mask == 0) {
            return;
        }

        out.put(mask);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.ANIM)) updating.appendAnimationRequest(npc, out);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.GRAPHICS)) updating.appendGfxUpdate(npc, out);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.HIT2)) updating.appendPrimaryHit2(npc, out);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.FACE_CHARACTER)) updating.appendFaceCharacter(npc, out);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.FORCED_CHAT)) updating.appendTextUpdate(npc, out);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.HIT)) updating.appendPrimaryHit(npc, out);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.APPEARANCE)) updating.appendAppearanceUpdate(npc, out);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.FACE_COORDINATE)) updating.appendFaceCoordinates(npc, out);
    }

    private int computeMask(Npc npc) {
        int updateMask = 0;
        if (npc.getUpdateFlags().isRequired(UpdateFlag.ANIM)) updateMask |= UpdateFlag.ANIM.getMask(npc.getType());
        if (npc.getUpdateFlags().isRequired(UpdateFlag.GRAPHICS)) updateMask |= UpdateFlag.GRAPHICS.getMask(npc.getType());
        if (npc.getUpdateFlags().isRequired(UpdateFlag.HIT2)) updateMask |= UpdateFlag.HIT2.getMask(npc.getType());
        if (npc.getUpdateFlags().isRequired(UpdateFlag.FACE_CHARACTER)) updateMask |= UpdateFlag.FACE_CHARACTER.getMask(npc.getType());
        if (npc.getUpdateFlags().isRequired(UpdateFlag.FORCED_CHAT)) updateMask |= UpdateFlag.FORCED_CHAT.getMask(npc.getType());
        if (npc.getUpdateFlags().isRequired(UpdateFlag.HIT)) updateMask |= UpdateFlag.HIT.getMask(npc.getType());
        if (npc.getUpdateFlags().isRequired(UpdateFlag.APPEARANCE)) updateMask |= UpdateFlag.APPEARANCE.getMask(npc.getType());
        if (npc.getUpdateFlags().isRequired(UpdateFlag.FACE_COORDINATE)) updateMask |= UpdateFlag.FACE_COORDINATE.getMask(npc.getType());
        return updateMask;
    }
}
