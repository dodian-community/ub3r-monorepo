package net.dodian.uber.game.model.entity.npc;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.netty.codec.ByteMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NpcUpdatingMaskOrderTest {

    private static final byte ANIM_MARKER = 0x21;
    private static final byte HIT2_MARKER = 0x22;
    private static final byte GFX_MARKER = 0x23;
    private static final byte FACE_CHARACTER_MARKER = 0x24;
    private static final byte FORCED_CHAT_MARKER = 0x25;
    private static final byte HIT_MARKER = 0x26;
    private static final byte APPEARANCE_MARKER = 0x27;
    private static final byte FACE_COORD_MARKER = 0x28;

    private NpcManager originalNpcManager;

    private static final class TestNpcManager extends NpcManager {
        @Override
        public void loadData() {
            // Prevent DB access in tests.
        }
    }

    private static final class MarkerNpcUpdating extends NpcUpdating {
        @Override
        public void appendAnimationRequest(Npc npc, ByteMessage buf) {
            buf.put(ANIM_MARKER);
        }

        @Override
        public void appendPrimaryHit2(Npc npc, ByteMessage buf) {
            buf.put(HIT2_MARKER);
        }

        @Override
        public void appendGfxUpdate(Npc npc, ByteMessage buf) {
            buf.put(GFX_MARKER);
        }

        @Override
        public void appendFaceCharacter(Npc npc, ByteMessage buf) {
            buf.put(FACE_CHARACTER_MARKER);
        }

        @Override
        public void appendTextUpdate(Npc npc, ByteMessage buf) {
            buf.put(FORCED_CHAT_MARKER);
        }

        @Override
        public void appendPrimaryHit(Npc npc, ByteMessage buf) {
            buf.put(HIT_MARKER);
        }

        @Override
        public void appendAppearanceUpdate(Npc npc, ByteMessage buf) {
            buf.put(APPEARANCE_MARKER);
        }

        @Override
        public void appendFaceCoordinates(Npc npc, ByteMessage buf) {
            buf.put(FACE_COORD_MARKER);
        }
    }

    @BeforeEach
    public void setUp() {
        originalNpcManager = Server.npcManager;
        Server.npcManager = new TestNpcManager();
    }

    @AfterEach
    public void tearDown() {
        Server.npcManager = originalNpcManager;
    }

    @Test
    public void writesNpcMaskBlocksInClientExpectedOrder() {
        Npc npc = new Npc(1, 1, new Position(3200, 3200, 0), 0);
        npc.getUpdateFlags().setRequired(UpdateFlag.ANIM, true);
        npc.getUpdateFlags().setRequired(UpdateFlag.HIT2, true);
        npc.getUpdateFlags().setRequired(UpdateFlag.GRAPHICS, true);
        npc.getUpdateFlags().setRequired(UpdateFlag.FACE_CHARACTER, true);
        npc.getUpdateFlags().setRequired(UpdateFlag.FORCED_CHAT, true);
        npc.getUpdateFlags().setRequired(UpdateFlag.HIT, true);
        npc.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
        npc.getUpdateFlags().setRequired(UpdateFlag.FACE_COORDINATE, true);

        ByteMessage updateBlock = ByteMessage.raw(64);
        try {
            new MarkerNpcUpdating().appendBlockUpdate(npc, updateBlock);

            int length = updateBlock.getBuffer().writerIndex();
            byte[] encoded = new byte[length];
            updateBlock.getBuffer().getBytes(0, encoded);

            byte[] payload = Arrays.copyOfRange(encoded, 1, encoded.length);
            assertArrayEquals(
                    new byte[]{
                            ANIM_MARKER,
                            HIT2_MARKER,
                            GFX_MARKER,
                            FACE_CHARACTER_MARKER,
                            FORCED_CHAT_MARKER,
                            HIT_MARKER,
                            APPEARANCE_MARKER,
                            FACE_COORD_MARKER
                    },
                    payload
            );
        } finally {
            updateBlock.releaseAll();
        }
    }

    @Test
    public void npcFaceCharacterBlockWritesSingleEntityShort() {
        Npc npc = new Npc(1, 1, new Position(3200, 3200, 0), 0);
        npc.getUpdateFlags().setRequired(UpdateFlag.FACE_CHARACTER, true);

        ByteMessage updateBlock = ByteMessage.raw(16);
        try {
            NpcUpdating.getInstance().appendBlockUpdate(npc, updateBlock);

            int length = updateBlock.getBuffer().writerIndex();
            byte[] encoded = new byte[length];
            updateBlock.getBuffer().getBytes(0, encoded);

            assertEquals(3, length);
            assertEquals(UpdateFlag.FACE_CHARACTER.getMask(npc.getType()), encoded[0] & 0xFF);
            assertEquals(0xFF, encoded[1] & 0xFF);
            assertEquals(0xFF, encoded[2] & 0xFF);
        } finally {
            updateBlock.releaseAll();
        }
    }
}
