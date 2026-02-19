package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.netty.codec.ByteMessage;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlayerUpdatingMaskOrderTest {

    private static final byte FORCED_MOVEMENT_MARKER = 0x11;
    private static final byte GRAPHICS_MARKER = 0x12;
    private static final byte ANIM_MARKER = 0x13;
    private static final byte FACE_CHARACTER_MARKER = 0x14;
    private static final byte FACE_COORD_MARKER = 0x15;
    private static final byte HIT_MARKER = 0x16;
    private static final byte HIT2_MARKER = 0x17;

    private static final class MarkerClient extends Client {
        private MarkerClient(int playerId) {
            super(null, playerId);
        }

        @Override
        public void appendMask400Update(ByteMessage buf) {
            buf.put(FORCED_MOVEMENT_MARKER);
        }
    }

    private static final class MarkerPlayerUpdating extends PlayerUpdating {
        @Override
        public void appendGraphic(Player player, ByteMessage buf) {
            buf.put(GRAPHICS_MARKER);
        }

        @Override
        public void appendAnimationRequest(Player player, ByteMessage buf) {
            buf.put(ANIM_MARKER);
        }

        @Override
        public void appendFaceCharacter(Player player, ByteMessage buf) {
            buf.put(FACE_CHARACTER_MARKER);
        }

        @Override
        public void appendFaceCoordinates(Player player, ByteMessage buf) {
            buf.put(FACE_COORD_MARKER);
        }

        @Override
        public void appendPrimaryHit(Player player, ByteMessage buf) {
            buf.put(HIT_MARKER);
        }

        @Override
        public void appendPrimaryHit2(Player player, ByteMessage buf) {
            buf.put(HIT2_MARKER);
        }
    }

    @Test
    public void writesPlayerMaskBlocksInClientExpectedOrder() {
        MarkerClient player = new MarkerClient(1);
        player.getUpdateFlags().setRequired(UpdateFlag.FORCED_MOVEMENT, true);
        player.getUpdateFlags().setRequired(UpdateFlag.GRAPHICS, true);
        player.getUpdateFlags().setRequired(UpdateFlag.ANIM, true);
        player.getUpdateFlags().setRequired(UpdateFlag.FACE_CHARACTER, true);
        player.getUpdateFlags().setRequired(UpdateFlag.FACE_COORDINATE, true);
        player.getUpdateFlags().setRequired(UpdateFlag.HIT, true);
        player.getUpdateFlags().setRequired(UpdateFlag.HIT2, true);

        ByteMessage updateBlock = ByteMessage.raw(64);
        try {
            new MarkerPlayerUpdating().appendBlockUpdate(player, updateBlock, PlayerUpdating.UpdatePhase.UPDATE_LOCAL);

            int length = updateBlock.getBuffer().writerIndex();
            byte[] encoded = new byte[length];
            updateBlock.getBuffer().getBytes(0, encoded);

            assertTrue(length > 2, "Expected mask + payload bytes.");
            int payloadOffset = (encoded[0] & 0x40) != 0 ? 2 : 1;
            byte[] payload = Arrays.copyOfRange(encoded, payloadOffset, encoded.length);

            assertArrayEquals(
                    new byte[]{
                            FORCED_MOVEMENT_MARKER,
                            GRAPHICS_MARKER,
                            ANIM_MARKER,
                            FACE_CHARACTER_MARKER,
                            FACE_COORD_MARKER,
                            HIT_MARKER,
                            HIT2_MARKER
                    },
                    payload
            );
        } finally {
            updateBlock.releaseAll();
        }
    }

    @Test
    public void repeatedMixedMaskEncodingIsStable() {
        Client player = new Client(null, 1);
        player.playerName = "mask-test";
        player.isNpc = true;
        player.setPlayerNpc(1);

        UpdateFlag[] flags = {
                UpdateFlag.FORCED_MOVEMENT,
                UpdateFlag.GRAPHICS,
                UpdateFlag.ANIM,
                UpdateFlag.FORCED_CHAT,
                UpdateFlag.CHAT,
                UpdateFlag.FACE_CHARACTER,
                UpdateFlag.APPEARANCE,
                UpdateFlag.FACE_COORDINATE,
                UpdateFlag.HIT,
                UpdateFlag.HIT2
        };

        for (int iteration = 0; iteration < 200; iteration++) {
            player.clearUpdateFlags();

            int maskSeed = iteration | 1; // guarantee at least one flag
            for (int i = 0; i < flags.length; i++) {
                if ((maskSeed & (1 << (i % 10))) != 0) {
                    player.getUpdateFlags().setRequired(flags[i], true);
                }
            }

            ByteMessage updateBlock = ByteMessage.raw(512);
            try {
                PlayerUpdating.getInstance().appendBlockUpdate(player, updateBlock, PlayerUpdating.UpdatePhase.UPDATE_LOCAL);
                assertTrue(updateBlock.getBuffer().writerIndex() > 0, "Expected encoded payload for mixed player flags.");
            } finally {
                updateBlock.releaseAll();
            }
        }
    }
}
