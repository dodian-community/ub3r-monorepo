package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.netty.codec.ByteMessage;

/**
 * Stateless Luna-style player update block encoder.
 */
final class PlayerUpdateBlockSet {

    void encode(PlayerUpdating updating, Player player, ByteMessage out, PlayerUpdating.UpdatePhase phase) {
        boolean cacheablePhase = phase == PlayerUpdating.UpdatePhase.UPDATE_LOCAL;
        boolean includeChat = phase != PlayerUpdating.UpdatePhase.UPDATE_SELF;
        boolean forceAppearance = phase == PlayerUpdating.UpdatePhase.ADD_LOCAL;

        if (cacheablePhase && player.isCachedUpdateBlockValid()) {
            player.writeCachedUpdateBlock(out);
            return;
        }

        ByteMessage blockBuf = cacheablePhase ? ByteMessage.raw(256) : out;
        try {
            int updateMask = computeUpdateMask(player, includeChat, forceAppearance);
            if (updateMask == 0) {
                return;
            }

            writeMask(blockBuf, updateMask);
            encodeBlocks(updating, player, blockBuf, includeChat, forceAppearance);

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

    private int computeUpdateMask(Player player, boolean includeChat, boolean forceAppearance) {
        int updateMask = 0;
        if (player.getUpdateFlags().isRequired(UpdateFlag.FORCED_MOVEMENT)) updateMask |= UpdateFlag.FORCED_MOVEMENT.getMask(player.getType());
        if (player.getUpdateFlags().isRequired(UpdateFlag.GRAPHICS)) updateMask |= UpdateFlag.GRAPHICS.getMask(player.getType());
        if (player.getUpdateFlags().isRequired(UpdateFlag.ANIM)) updateMask |= UpdateFlag.ANIM.getMask(player.getType());
        if (player.getUpdateFlags().isRequired(UpdateFlag.FORCED_CHAT)) updateMask |= UpdateFlag.FORCED_CHAT.getMask(player.getType());
        if (includeChat && player.getUpdateFlags().isRequired(UpdateFlag.CHAT)) updateMask |= UpdateFlag.CHAT.getMask(player.getType());
        if (player.getUpdateFlags().isRequired(UpdateFlag.FACE_CHARACTER)) updateMask |= UpdateFlag.FACE_CHARACTER.getMask(player.getType());
        if (forceAppearance || player.getUpdateFlags().isRequired(UpdateFlag.APPEARANCE)) updateMask |= UpdateFlag.APPEARANCE.getMask(player.getType());
        if (player.getUpdateFlags().isRequired(UpdateFlag.FACE_COORDINATE)) updateMask |= UpdateFlag.FACE_COORDINATE.getMask(player.getType());
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
                              boolean forceAppearance) {
        if (player.getUpdateFlags().isRequired(UpdateFlag.FORCED_MOVEMENT)) player.appendMask400Update(blockBuf);
        if (player.getUpdateFlags().isRequired(UpdateFlag.GRAPHICS)) updating.appendGraphic(player, blockBuf);
        if (player.getUpdateFlags().isRequired(UpdateFlag.ANIM)) updating.appendAnimationRequest(player, blockBuf);
        if (player.getUpdateFlags().isRequired(UpdateFlag.FORCED_CHAT)) PlayerUpdating.appendForcedChatText(player, blockBuf);
        if (includeChat && player.getUpdateFlags().isRequired(UpdateFlag.CHAT)) PlayerUpdating.appendPlayerChatText(player, blockBuf);
        if (player.getUpdateFlags().isRequired(UpdateFlag.FACE_CHARACTER)) updating.appendFaceCharacter(player, blockBuf);
        if (forceAppearance || player.getUpdateFlags().isRequired(UpdateFlag.APPEARANCE)) PlayerUpdating.appendPlayerAppearance(player, blockBuf);
        if (player.getUpdateFlags().isRequired(UpdateFlag.FACE_COORDINATE)) updating.appendFaceCoordinates(player, blockBuf);
        if (player.getUpdateFlags().isRequired(UpdateFlag.HIT)) updating.appendPrimaryHit(player, blockBuf);
        if (player.getUpdateFlags().isRequired(UpdateFlag.HIT2)) updating.appendPrimaryHit2(player, blockBuf);
    }
}
