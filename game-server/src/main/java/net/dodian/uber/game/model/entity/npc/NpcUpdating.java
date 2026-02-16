package net.dodian.uber.game.model.entity.npc;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.EntityUpdating;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.utilities.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Dashboard
 */
public class NpcUpdating extends EntityUpdating<Npc> {

    private static final Logger logger = LoggerFactory.getLogger(NpcUpdating.class);
    private static final boolean DEBUG_NPC_MOVEMENT_WRITES = false;
    private static final AtomicInteger DEBUG_MOVEMENT_WRITE_COUNTER = new AtomicInteger();

    private static final NpcUpdating instance = new NpcUpdating();

    public static NpcUpdating getInstance() {
        return instance;
    }

    @Override
    public void update(Player player, ByteMessage stream) {
        ByteMessage updateBlock = ByteMessage.raw(16384);
        ByteMessage buf = stream;
        int movementWrites = 0;

        stream.startBitAccess();

        stream.putBits(8, player.getLocalNpcs().size());
        for (Iterator<Npc> i = player.getLocalNpcs().iterator(); i.hasNext(); ) {
            Npc npc = i.next();
            boolean exceptions = removeNpc(player, npc);
            if (player.withinDistance(npc) && npc.isVisible() && !exceptions) {
                updateNPCMovement(npc, stream);
                movementWrites++;
                appendBlockUpdate(npc, updateBlock);
            } else {
                buf.putBits(1, 1);
                stream.putBits(2, 3); // tells client to remove this npc from list
                i.remove();
            }
        }

        for (Npc npc : Server.npcManager.getNpcs()) {
            boolean exceptions = removeNpc(player, npc);
            if (npc == null || !(player.withinDistance(npc) && npc.isVisible()) || !npc.isVisible() || exceptions) continue;
            if (player.getLocalNpcs().add(npc)) {
                if(npc.getId() == 1306 || npc.getId() == 1307) //Makeover mage!
                    npc.setId(player.getGender() == 0 ? 1306 : 1307);
                addNpc(player, npc, stream);
                appendBlockUpdate(npc, updateBlock);
            }
        }
        if (updateBlock.getBuffer().writerIndex() > 0) {
            stream.putBits(14, 16383);
            stream.endBitAccess();
            // Only copy the written bytes, not the entire buffer capacity
            byte[] updateData = new byte[updateBlock.getBuffer().writerIndex()];
            updateBlock.getBuffer().getBytes(0, updateData);
            stream.putBytes(updateData);
        } else {
            stream.endBitAccess();
        }
        // Note: endFrameVarSizeWord equivalent is handled by the outer packet wrapper

        if (DEBUG_NPC_MOVEMENT_WRITES && movementWrites > 0) {
            DEBUG_MOVEMENT_WRITE_COUNTER.addAndGet(movementWrites);
            logger.debug("npcMovementWrites viewer={} count={}", player.getPlayerName(), movementWrites);
        }
    }

    public static boolean removeNpc(Player player, Npc npc) {
        Client c = ((Client) player);
        if(c == null || npc == null) return false;
        if (!npc.canBeSeenBy(c)) {
            return true;
        }
        if(c.quests[1] > 0 && npc.getId() == 999 && npc.getPosition().getX() == 2 && npc.getPosition().getY() == 2)
            return true;
        return false;
    }


    public void addNpc(Player player, Npc npc, ByteMessage buf) {

        buf.putBits(14, npc.getSlot());
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

        buf.putBits(1, 0); // something??
        buf.putBits(14, npc.getId());
        buf.putBits(1, npc.getUpdateFlags().isUpdateRequired() ? 1 : 0);
    }

    @Override
    public void appendBlockUpdate(Npc npc, ByteMessage buf) {

        if(!npc.getUpdateFlags().isUpdateRequired())
            return;
        int updateMask = 0;
        for (UpdateFlag flag : npc.getUpdateFlags().keySet()) {
            if (npc.getUpdateFlags().isRequired(flag)) {
                updateMask |= flag.getMask(npc.getType());
            }
        }
        buf.put(updateMask);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.ANIM))
            appendAnimationRequest(npc, buf);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.HIT2))
            appendPrimaryHit2(npc, buf);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.GRAPHICS))
            appendGfxUpdate(npc, buf);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.FORCED_CHAT))
            appendTextUpdate(npc, buf);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.HIT))
            appendPrimaryHit(npc, buf);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.FACE_CHARACTER))
            appendFaceCharacter(npc, buf);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.FACE_COORDINATE))
            appendFaceCoordinates(npc, buf);
    }

    public void appendTextUpdate(Npc npc, ByteMessage buf) {
        buf.putString(npc.getText());
    }

    public void appendGfxUpdate(Npc npc, ByteMessage buf) {
        buf.putShort(npc.getGfxId());
        buf.putInt(npc.getGfxHeight() << 16);
    }

    @Override
    public void appendAnimationRequest(Npc npc, ByteMessage buf) {
        buf.putShort(npc.getAnimationId(), ByteOrder.LITTLE); // writeWordBigEndian
        buf.put(npc.getAnimationDelay());
    }

    @Override
    public void appendPrimaryHit(Npc npc, ByteMessage buf) {

        // Client npcUpdateMask (mask & 0x08) expects:
        // short damage, byte type, short currentHp, short maxHp
        int damage = npc.getDamageDealt();
        if (damage < Short.MIN_VALUE) damage = Short.MIN_VALUE;
        if (damage > Short.MAX_VALUE) damage = Short.MAX_VALUE;
        buf.putShort(damage);

        int type;
        if (npc.getDamageDealt() == 0) {
            type = 0; // miss
        } else if (npc.getHitType() == Entity.hitType.BURN) {
            type = 4;
        } else if (npc.getHitType() == Entity.hitType.CRIT) {
            type = 3;
        } else if (npc.getHitType() == Entity.hitType.POISON) {
            type = 2;
        } else {
            type = 1; // normal
        }
        buf.put(type);

        int current = Math.max(0, npc.getCurrentHealth());
        int max = Math.max(1, npc.getMaxHealth());
        buf.putShort(current);
        buf.putShort(max);
    }

    public void appendPrimaryHit2(Npc npc, ByteMessage buf) {
        // Client npcUpdateMask (mask & 0x40) uses the same layout as primary hit
        int damage = npc.getDamageDealt2();
        if (damage < Short.MIN_VALUE) damage = Short.MIN_VALUE;
        if (damage > Short.MAX_VALUE) damage = Short.MAX_VALUE;
        buf.putShort(damage);

        int type;
        if (npc.getDamageDealt2() == 0) {
            type = 0; // miss
        } else if (npc.getHitType2() == Entity.hitType.BURN) {
            type = 4;
        } else if (npc.getHitType2() == Entity.hitType.CRIT) {
            type = 3;
        } else if (npc.getHitType2() == Entity.hitType.POISON) {
            type = 2;
        } else {
            type = 1; // normal
        }
        buf.put(type);

        int current = Math.max(0, npc.getCurrentHealth());
        int max = Math.max(1, npc.getMaxHealth());
        buf.putShort(current);
        buf.putShort(max);
    }
    @Override
    public void appendFaceCoordinates(Npc npc, ByteMessage buf) {
        buf.putShort(npc.getFacePosition().getX(), ByteOrder.LITTLE); // writeWordBigEndian
        buf.putShort(npc.getFacePosition().getY(), ByteOrder.LITTLE); // writeWordBigEndian
    }

    @Override
    public void appendFaceCharacter(Npc npc, ByteMessage buf) {
        buf.putShort(npc.getViewX(), ByteOrder.LITTLE); // writeWordBigEndian
        buf.putShort(npc.getViewY(), ByteOrder.LITTLE); // writeWordBigEndian
    }

    public void updateNPCMovement(Npc npc, ByteMessage buf) {
        if (npc.getDirection() == -1) {
            if (npc.getUpdateFlags().isUpdateRequired()) {
                buf.putBits(1, 1);
                buf.putBits(2, 0);
            } else {
                buf.putBits(1, 0);
            }
        } else {
            buf.putBits(1, 1);
            buf.putBits(2, 1);
            buf.putBits(3, Utils.xlateDirectionToClient[npc.getDirection()]);
            if (npc.getUpdateFlags().isUpdateRequired()) {
                buf.putBits(1, 1);
            } else {
                buf.putBits(1, 0);
            }
        }
    }

    public static int consumeDebugMovementWriteCounter() {
        return DEBUG_MOVEMENT_WRITE_COUNTER.getAndSet(0);
    }

}
