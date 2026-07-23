package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.model.entity.UpdateFlag;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.engine.sync.protocol.PackedUpdateBlock;
import net.dodian.uber.game.engine.sync.scratch.ThreadLocalSyncScratch;

/**
 * Stateless Luna-style player update block encoder.
 */
final class PlayerUpdateBlockSet {

    PackedUpdateBlock encode(PlayerUpdating updating, Player player, PlayerUpdating.UpdatePhase phase) {
        boolean includeChat = phase != PlayerUpdating.UpdatePhase.UPDATE_SELF;
        boolean forceAppearance = phase == PlayerUpdating.UpdatePhase.ADD_LOCAL;
        boolean includeAddLocalFacingSnapshot = shouldIncludeAddLocalFacingSnapshot(player, phase);
        int updateMask = computeUpdateMask(player, includeChat, forceAppearance, includeAddLocalFacingSnapshot);
        if (updateMask == 0) {
            return null;
        }

        ByteMessage fixed = ThreadLocalSyncScratch.packedFixedBlock();
        fixed.startBitAccess();
        if (player.getUpdateFlags().isRequired(UpdateFlag.FORCED_MOVEMENT)) player.appendMask400Update(fixed);
        if (player.getUpdateFlags().isRequired(UpdateFlag.GRAPHICS)) updating.appendGraphic(player, fixed);
        if (player.getUpdateFlags().isRequired(UpdateFlag.ANIM)) updating.appendAnimationRequest(player, fixed);
        if (player.getUpdateFlags().isRequired(UpdateFlag.FACE_CHARACTER)) updating.appendFaceCharacter(player, fixed);
        if (player.getUpdateFlags().isRequired(UpdateFlag.FACE_COORDINATE)) {
            updating.appendFaceCoordinates(player, fixed);
        } else if (includeAddLocalFacingSnapshot) {
            appendAddLocalFacingSnapshot(player, fixed);
        }
        if (player.getUpdateFlags().isRequired(UpdateFlag.HIT)) updating.appendPrimaryHit(player, fixed);
        if (player.getUpdateFlags().isRequired(UpdateFlag.HIT2)) updating.appendPrimaryHit2(player, fixed);
        int fixedBitCount = fixed.getBitIndex();
        fixed.endBitAccess();

        ByteMessage variable = ThreadLocalSyncScratch.packedVariableBlock();
        if (player.getUpdateFlags().isRequired(UpdateFlag.FORCED_CHAT)) {
            PlayerUpdating.appendForcedChatText(player, variable);
        }
        if (includeChat && player.getUpdateFlags().isRequired(UpdateFlag.CHAT)) {
            PlayerUpdating.appendPlayerChatText(player, variable);
        }
        if (forceAppearance || player.getUpdateFlags().isRequired(UpdateFlag.APPEARANCE)) {
            PlayerUpdating.appendPlayerAppearance(player, variable);
        }
        return new PackedUpdateBlock(
                updateMask,
                fixed.toByteArray(),
                fixedBitCount,
                variable.toByteArray()
        );
    }

    private int computeUpdateMask(Player player,
                                  boolean includeChat,
                                  boolean forceAppearance,
                                  boolean includeAddLocalFacingSnapshot) {
        int updateMask = 0;
        if (player.getUpdateFlags().isRequired(UpdateFlag.FORCED_MOVEMENT)) updateMask |= UpdateFlag.FORCED_MOVEMENT.getMask(player.getType());
        if (player.getUpdateFlags().isRequired(UpdateFlag.GRAPHICS)) updateMask |= UpdateFlag.GRAPHICS.getMask(player.getType());
        if (player.getUpdateFlags().isRequired(UpdateFlag.ANIM)) updateMask |= UpdateFlag.ANIM.getMask(player.getType());
        if (player.getUpdateFlags().isRequired(UpdateFlag.FORCED_CHAT)) updateMask |= UpdateFlag.FORCED_CHAT.getMask(player.getType());
        if (includeChat && player.getUpdateFlags().isRequired(UpdateFlag.CHAT)) updateMask |= UpdateFlag.CHAT.getMask(player.getType());
        if (player.getUpdateFlags().isRequired(UpdateFlag.FACE_CHARACTER)) updateMask |= UpdateFlag.FACE_CHARACTER.getMask(player.getType());
        if (forceAppearance || player.getUpdateFlags().isRequired(UpdateFlag.APPEARANCE)) updateMask |= UpdateFlag.APPEARANCE.getMask(player.getType());
        if (player.getUpdateFlags().isRequired(UpdateFlag.FACE_COORDINATE) || includeAddLocalFacingSnapshot) {
            updateMask |= UpdateFlag.FACE_COORDINATE.getMask(player.getType());
        }
        if (player.getUpdateFlags().isRequired(UpdateFlag.HIT)) updateMask |= UpdateFlag.HIT.getMask(player.getType());
        if (player.getUpdateFlags().isRequired(UpdateFlag.HIT2)) updateMask |= UpdateFlag.HIT2.getMask(player.getType());
        return updateMask;
    }

    private boolean shouldIncludeAddLocalFacingSnapshot(Player player, PlayerUpdating.UpdatePhase phase) {
        if (phase != PlayerUpdating.UpdatePhase.ADD_LOCAL) {
            return false;
        }
        if (player.getUpdateFlags().isRequired(UpdateFlag.FACE_CHARACTER)
                || player.getUpdateFlags().isRequired(UpdateFlag.FACE_COORDINATE)) {
            return false;
        }
        int deltaX = normalizeDelta(player.getLastWalkDeltaX());
        int deltaY = normalizeDelta(player.getLastWalkDeltaY());
        return deltaX != 0 || deltaY != 0;
    }

    private int normalizeDelta(int delta) {
        if (delta < -1) {
            return -1;
        }
        if (delta > 1) {
            return 1;
        }
        return delta;
    }

    private void appendAddLocalFacingSnapshot(Player player, ByteMessage blockBuf) {
        int deltaX = normalizeDelta(player.getLastWalkDeltaX());
        int deltaY = normalizeDelta(player.getLastWalkDeltaY());
        int focusX = player.getPosition().getX() + deltaX;
        int focusY = player.getPosition().getY() + deltaY;
        int encodedX = (focusX * 2) + 1;
        int encodedY = (focusY * 2) + 1;
        blockBuf.putBits(16, encodedX);
        blockBuf.putBits(16, encodedY);
    }
}
