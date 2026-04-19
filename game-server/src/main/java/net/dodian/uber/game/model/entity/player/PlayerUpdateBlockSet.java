package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.model.entity.UpdateFlag;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.engine.sync.SynchronizationContext;

/**
 * Stateless Luna-style player update block encoder.
 */
final class PlayerUpdateBlockSet {

    void encode(PlayerUpdating updating, Player player, ByteMessage out, PlayerUpdating.UpdatePhase phase) {
        boolean cacheablePhase = phase == PlayerUpdating.UpdatePhase.UPDATE_LOCAL;
        boolean includeChat = phase != PlayerUpdating.UpdatePhase.UPDATE_SELF;
        boolean forceAppearance = phase == PlayerUpdating.UpdatePhase.ADD_LOCAL;
        boolean includeAddLocalFacingSnapshot = shouldIncludeAddLocalFacingSnapshot(player, phase);
        boolean sharedCacheablePhase = phase != PlayerUpdating.UpdatePhase.UPDATE_SELF;
        int updateMask = computeUpdateMask(player, includeChat, forceAppearance, includeAddLocalFacingSnapshot);

        if (sharedCacheablePhase && updateMask != 0) {
            byte[] sharedBlock = SynchronizationContext.getSharedPlayerBlock(player, phase.name());
            if (sharedBlock != null) {
                out.putBytes(sharedBlock);
                SynchronizationContext.recordPlayerBlockCacheHit(true);
                return;
            }
            SynchronizationContext.recordPlayerBlockCacheHit(false);
        }

        if (cacheablePhase && player.isCachedUpdateBlockValid()) {
            player.writeCachedUpdateBlock(out);
            return;
        }

        ByteMessage blockBuf = cacheablePhase ? ByteMessage.raw(256) : out;
        try {
            if (updateMask == 0) {
                return;
            }

            writeMask(blockBuf, updateMask);
            encodeBlocks(updating, player, blockBuf, includeChat, forceAppearance, includeAddLocalFacingSnapshot);

            if (cacheablePhase) {
                out.putBytes(blockBuf);
                player.cacheUpdateBlock(blockBuf);
            }
        } finally {
            if (cacheablePhase) {
                blockBuf.release();
            }
        }
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

    private void writeMask(ByteMessage blockBuf, int updateMask) {
        if (updateMask >= 0x100) {
            int overflowMask = updateMask | 0x40;
            blockBuf.put(overflowMask & 0xFF);
            blockBuf.put(overflowMask >> 8);
            return;
        }
        blockBuf.put(updateMask);
    }

    private void encodeBlocks(PlayerUpdating updating,
                              Player player,
                              ByteMessage blockBuf,
                              boolean includeChat,
                              boolean forceAppearance,
                              boolean includeAddLocalFacingSnapshot) {
        if (player.getUpdateFlags().isRequired(UpdateFlag.FORCED_MOVEMENT)) player.appendMask400Update(blockBuf);
        if (player.getUpdateFlags().isRequired(UpdateFlag.GRAPHICS)) updating.appendGraphic(player, blockBuf);
        if (player.getUpdateFlags().isRequired(UpdateFlag.ANIM)) updating.appendAnimationRequest(player, blockBuf);
        if (player.getUpdateFlags().isRequired(UpdateFlag.FORCED_CHAT)) PlayerUpdating.appendForcedChatText(player, blockBuf);
        if (includeChat && player.getUpdateFlags().isRequired(UpdateFlag.CHAT)) PlayerUpdating.appendPlayerChatText(player, blockBuf);
        if (player.getUpdateFlags().isRequired(UpdateFlag.FACE_CHARACTER)) updating.appendFaceCharacter(player, blockBuf);
        if (forceAppearance || player.getUpdateFlags().isRequired(UpdateFlag.APPEARANCE)) PlayerUpdating.appendPlayerAppearance(player, blockBuf);
        if (player.getUpdateFlags().isRequired(UpdateFlag.FACE_COORDINATE)) {
            updating.appendFaceCoordinates(player, blockBuf);
        } else if (includeAddLocalFacingSnapshot) {
            appendAddLocalFacingSnapshot(player, blockBuf);
        }
        if (player.getUpdateFlags().isRequired(UpdateFlag.HIT)) updating.appendPrimaryHit(player, blockBuf);
        if (player.getUpdateFlags().isRequired(UpdateFlag.HIT2)) updating.appendPrimaryHit2(player, blockBuf);
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
        blockBuf.putShort(encodedX, net.dodian.uber.game.netty.codec.ByteOrder.LITTLE, net.dodian.uber.game.netty.codec.ValueType.ADD);
        blockBuf.putShort(encodedY, net.dodian.uber.game.netty.codec.ByteOrder.LITTLE);
    }
}
