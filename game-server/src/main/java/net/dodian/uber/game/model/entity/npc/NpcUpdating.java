package net.dodian.uber.game.model.entity.npc;

import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.EntityUpdating;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.engine.sync.scratch.ThreadLocalSyncScratch;
import net.dodian.uber.game.engine.sync.protocol.PackedUpdateBlock;
import net.dodian.uber.game.engine.sync.protocol.PackedBitSlice;
import net.dodian.utilities.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Dashboard
 */
public class NpcUpdating extends EntityUpdating<Npc> {

    private static final Logger logger = LoggerFactory.getLogger(NpcUpdating.class);
    public static final int NPC_SLOT_BITS = 16;
    static final int NPC_DEFINITION_BITS = 16;
    public static final int NPC_SLOT_TERMINATOR = (1 << NPC_SLOT_BITS) - 1;
    static final int MAX_CLIENT_NPC_SLOT = 16_383;
    static final int MAX_NPC_DEFINITION_ID = (1 << NPC_DEFINITION_BITS) - 1;
    /** Local-list entry type marking a slot as removed from the viewer's local set. */
    private static final int LOCAL_REMOVE_TYPE = 3;
    private static final NpcUpdateBlockSet BLOCK_SET = new NpcUpdateBlockSet();

    private static final NpcUpdating instance = new NpcUpdating();

    public static NpcUpdating getInstance() {
        return instance;
    }

    public static boolean removeNpc(Player player, Npc npc) {
        Client c = ((Client) player);
        if(c == null || npc == null) return false;
        if (!npc.canBeSeenBy(c)) {
            return true;
        }
        return c.quests[1] > 0 && npc.getId() == 999 && npc.getPosition().getX() == 2 && npc.getPosition().getY() == 2;
    }

    public void writeLocalRemoval(ByteMessage stream) {
        stream.putBits(1, 1);
        stream.putBits(2, LOCAL_REMOVE_TYPE);
    }


    public void addNpc(Player player, Npc npc, ByteMessage buf) {
        addNpc(player, npc, buf, npc.getUpdateFlags().isUpdateRequired());
    }

    public void addNpc(Player player, Npc npc, ByteMessage buf, boolean updateRequired) {

        validateNpcSlot(npc.getSlot());
        buf.putBits(NPC_SLOT_BITS, npc.getSlot());
        /* Position */
        Position npcPos = npc.getPosition(), plrPos = player.getPosition();
        int z = npcPos.getY() - plrPos.getY();
        if(z < 0)
            z += 32;
        buf.putBits(5, z); // y coordinate relative to thisPlayer
        z = npcPos.getX() - plrPos.getX();
        if(z < 0)
            z += 32;
        buf.putBits(5, z); // y coordinate relative to thisPlayer

        buf.putBits(1, 0); // Tarnish preserves the walking queue on add-local.
        int displayId = displayIdFor(player, npc);
        validateNpcDefinitionId(displayId);
        buf.putBits(NPC_DEFINITION_BITS, displayId);
        buf.putBits(1, updateRequired ? 1 : 0);
    }

    private int displayIdFor(Player player, Npc npc) {
        int id = npc.getId();
        if (id == 1306 || id == 1307) {
            return player.getGender() == 0 ? 1306 : 1307;
        }
        // appendAppearanceUpdate (below) already prefers getTransformedNpcId() when set - this
        // add-local path didn't, so a player entering range after an NPC transformed (or after it
        // respawned still transformed) would render the base id instead, while a player already
        // present would see the transformed id from their existing UPDATE_LOCAL blocks. Two
        // players standing together would disagree on what the NPC looks like.
        int transformedNpcId = npc.getTransformedNpcId();
        return transformedNpcId >= 0 ? transformedNpcId : id;
    }

    static void validateNpcSlot(int slot) {
        if (slot < 0 || slot > MAX_CLIENT_NPC_SLOT) {
            throw new IllegalArgumentException("NPC slot cannot be encoded: " + slot);
        }
    }

    static void validateNpcDefinitionId(int id) {
        if (id < 0 || id > MAX_NPC_DEFINITION_ID) {
            throw new IllegalArgumentException("NPC definition id cannot be encoded: " + id);
        }
    }

    public PackedUpdateBlock buildSharedBlock(Npc npc) {
        return buildPackedBlock(npc);
    }

    public PackedUpdateBlock buildPackedBlock(Npc npc) {
        return BLOCK_SET.encode(this, npc);
    }

    public PackedBitSlice buildLocalMovement(Npc npc) {
        ByteMessage movement = ThreadLocalSyncScratch.packedFixedBlock();
        movement.startBitAccess();
        updateNPCMovement(npc, movement, NpcUpdateMaskCalculator.computeMask(npc) != 0);
        int bitCount = movement.getBitIndex();
        movement.endBitAccess();
        return new PackedBitSlice(movement.toByteArray(), bitCount);
    }

    private static void releaseScratch(ByteMessage message) {
        // Scratch buffers are reused from thread-local storage.
    }

    public void appendTextUpdate(Npc npc, ByteMessage buf) {
        buf.putString(npc.getText());
    }

    public void appendGfxUpdate(Npc npc, ByteMessage buf) {
        buf.putBits(16, npc.getGfxId());
        buf.putBits(32, npc.getGfxHeight() << 16);
    }

    @Override
    public void appendAnimationRequest(Npc npc, ByteMessage buf) {
        buf.putBits(16, npc.getAnimationId());
        buf.putBits(8, npc.getAnimationDelay());
    }

    @Override
    public void appendPrimaryHit(Npc npc, ByteMessage buf) {
        appendTarnishNpcHit(buf, npc.getDamageDealt(), npc.getHitType(), npc);
    }

    public void appendPrimaryHit2(Npc npc, ByteMessage buf) {
        appendTarnishNpcHit(buf, npc.getDamageDealt2(), npc.getHitType2(), npc);
    }
    @Override
    public void appendFaceCoordinates(Npc npc, ByteMessage buf) {
        buf.putBits(16, npc.getFaceCoordinateX());
        buf.putBits(16, npc.getFaceCoordinateY());
    }

    @Override
    public void appendFaceCharacter(Npc npc, ByteMessage buf) {
        int faceTarget = npc.getFaceTarget();
        if (faceTarget < 0 || faceTarget > 0xFFFF) {
            faceTarget = 0xFFFF;
        }
        buf.putBits(16, faceTarget);
    }

    public void appendAppearanceUpdate(Npc npc, ByteMessage buf) {
        int transformedNpcId = npc.getTransformedNpcId();
        int definitionId = transformedNpcId >= 0 ? transformedNpcId : npc.getId();
        validateNpcDefinitionId(definitionId);
        buf.putBits(16, definitionId);
    }

    private static void appendTarnishNpcHit(ByteMessage buf, int damage, Entity.hitType hitType, Npc npc) {
        int maximum = npc.getMaxHealth() >= 500 ? 200 : 100;
        int health = Math.max(0, Math.min(maximum,
                npc.getCurrentHealth() * maximum / Math.max(1, npc.getMaxHealth())));
        buf.putBits(8, Math.max(0, Math.min(255, damage)));
        buf.putBits(3, tarnishHitType(damage, hitType));
        buf.putBits(8, health);
        buf.putBits(1, maximum == 200 ? 1 : 0);
    }

    private static int tarnishHitType(int damage, Entity.hitType hitType) {
        if (damage == 0) return 0;
        if (hitType == Entity.hitType.BURN) return 4;
        if (hitType == Entity.hitType.CRIT) return 3;
        if (hitType == Entity.hitType.POISON) return 2;
        return 1;
    }

    public void updateNPCMovement(Npc npc, ByteMessage buf) {
        updateNPCMovement(npc, buf, npc.getUpdateFlags().isUpdateRequired());
    }

    public void updateNPCMovement(Npc npc, ByteMessage buf, boolean updateRequired) {
        if (npc.getDirection() == -1) {
            if (updateRequired) {
                buf.putBits(1, 1);
                buf.putBits(2, 0);
            } else {
                buf.putBits(1, 0);
            }
        } else {
            int translatedDirection = translateDirectionToClient(npc);
            if (translatedDirection == -1) {
                if (npc.getUpdateFlags().isUpdateRequired()) {
                    buf.putBits(1, 1);
                    buf.putBits(2, 0);
                } else {
                    buf.putBits(1, 0);
                }
                return;
            }
            buf.putBits(1, 1);
            buf.putBits(2, 1);
            buf.putBits(3, translatedDirection);
            if (updateRequired) {
                buf.putBits(1, 1);
            } else {
                buf.putBits(1, 0);
            }
        }
    }

    private int translateDirectionToClient(Npc npc) {
        int direction = npc.getDirection();
        if (direction < 0 || direction >= Utils.xlateDirectionToClient.length) {
            logger.warn("Invalid npc direction {} for slot={} id={}", direction, npc.getSlot(), npc.getId());
            return -1;
        }
        return Utils.xlateDirectionToClient[direction];
    }

}
