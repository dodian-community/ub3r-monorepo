package net.dodian.uber.game.model.entity.npc;

import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.engine.sync.SynchronizationContext;
import net.dodian.uber.game.model.UpdateFlag;

/**
 * Stateless Luna-style NPC update block encoder.
 */
final class NpcUpdateBlockSet {

    void encode(NpcUpdating updating, Npc npc, ByteMessage out) {
        byte[] sharedBlock = SynchronizationContext.getSharedNpcBlock(npc);
        if (sharedBlock != null) {
            out.putBytes(sharedBlock);
            SynchronizationContext.recordNpcBlockCacheHit(true);
            return;
        }
        SynchronizationContext.recordNpcBlockCacheHit(false);

        int mask = NpcUpdateMaskService.computeMask(npc);
        if (mask == 0) {
            return;
        }

        out.put(mask);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.ANIM)) updating.appendAnimationRequest(npc, out);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.GRAPHICS)) updating.appendGfxUpdate(npc, out);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.FACE_CHARACTER)) updating.appendFaceCharacter(npc, out);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.FORCED_CHAT)) updating.appendTextUpdate(npc, out);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.HIT)) updating.appendPrimaryHit(npc, out);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.HIT2)) updating.appendPrimaryHit2(npc, out);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.APPEARANCE)) updating.appendAppearanceUpdate(npc, out);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.FACE_COORDINATE)) updating.appendFaceCoordinates(npc, out);
    }

}
