package net.dodian.uber.game.model.entity.npc;

import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.engine.sync.protocol.PackedUpdateBlock;
import net.dodian.uber.game.engine.sync.scratch.ThreadLocalSyncScratch;
import net.dodian.uber.game.model.entity.UpdateFlag;

/**
 * Stateless Luna-style NPC update block encoder.
 */
final class NpcUpdateBlockSet {

    PackedUpdateBlock encode(NpcUpdating updating, Npc npc) {
        int mask = NpcUpdateMaskCalculator.computeMask(npc);
        if (mask == 0) {
            // Nothing pending this tick: skip the shared-block cache entirely rather than
            // risk replaying a stale hit from a slot that isn't re-written every tick (the
            // slot-indexed cache is persistent across ticks, unlike the old per-tick map).
            return null;
        }

        ByteMessage fixed = ThreadLocalSyncScratch.packedFixedBlock();
        fixed.startBitAccess();
        if (npc.getUpdateFlags().isRequired(UpdateFlag.ANIM)) updating.appendAnimationRequest(npc, fixed);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.GRAPHICS)) updating.appendGfxUpdate(npc, fixed);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.FACE_CHARACTER)) updating.appendFaceCharacter(npc, fixed);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.HIT)) updating.appendPrimaryHit(npc, fixed);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.HIT2)) updating.appendPrimaryHit2(npc, fixed);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.APPEARANCE)) updating.appendAppearanceUpdate(npc, fixed);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.FACE_COORDINATE)) updating.appendFaceCoordinates(npc, fixed);
        int fixedBitCount = fixed.getBitIndex();
        fixed.endBitAccess();

        ByteMessage variable = ThreadLocalSyncScratch.packedVariableBlock();
        if (npc.getUpdateFlags().isRequired(UpdateFlag.FORCED_CHAT)) updating.appendTextUpdate(npc, variable);
        return new PackedUpdateBlock(mask, fixed.toByteArray(), fixedBitCount, variable.toByteArray());
    }

}
