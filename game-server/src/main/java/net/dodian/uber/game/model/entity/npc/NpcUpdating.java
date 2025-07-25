package net.dodian.uber.game.model.entity.npc;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.EntityUpdating;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.utilities.Misc;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.utilities.Utils;

import java.util.Iterator;

/**
 * @author Dashboard
 */
public class NpcUpdating extends EntityUpdating<Npc> {

    private static final NpcUpdating instance = new NpcUpdating();

    public static NpcUpdating getInstance() {
        return instance;
    }

    @Override
    public void update(Player player, ByteMessage stream) {
        ByteMessage updateBlock = ByteMessage.raw(16384);
        ByteMessage buf = stream;

        stream.startBitAccess();

        stream.putBits(8, player.getLocalNpcs().size());
        for (Iterator<Npc> i = player.getLocalNpcs().iterator(); i.hasNext(); ) {
            Npc npc = i.next();
            boolean exceptions = removeNpc(player, npc);
            if (player.withinDistance(npc) && npc.isVisible() && !exceptions) {
                updateNPCMovement(npc, stream);
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
    }

    public static boolean removeNpc(Player player, Npc npc) {
        Client c = ((Client) player);
        if(c == null || npc == null) return false;
        boolean check = c.quests[0] > 0 && npc.getId() == 555 && npc.getPosition().getX() == 2604 && npc.getPosition().getY() == 3092;
        if(c.quests[1] > 0 && npc.getId() == 999 && npc.getPosition().getX() == 2 && npc.getPosition().getY() == 2)
            check = true;
        return check;
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
        buf.put(Math.min(npc.getDamageDealt(), 255), ValueType.NEGATE); // writeByteC = -value
        if (npc.getDamageDealt() == 0)
            buf.put(0, ValueType.SUBTRACT); // writeByteS = 128-value
        else if (npc.getHitType() == Entity.hitType.BURN)
            buf.put(4, ValueType.SUBTRACT);
        else if (npc.getHitType() == Entity.hitType.CRIT)
            buf.put(3, ValueType.SUBTRACT);
        else if (npc.getHitType() == Entity.hitType.POISON)
            buf.put(2, ValueType.SUBTRACT);
        else
            buf.put(1, ValueType.SUBTRACT);
        double hp = Misc.getCurrentHP(npc.getCurrentHealth(), npc.getMaxHealth());
        int value = hp > 4.00 ? (int) hp : hp != 0.0 ? 4 : 0;
        buf.put(value, ValueType.SUBTRACT); // writeByteS = 128-value
        buf.put(100, ValueType.NEGATE); // writeByteC = -value
    }

    public void appendPrimaryHit2(Npc npc, ByteMessage buf) {
        buf.put(Math.min(npc.getDamageDealt2(), 255), ValueType.ADD); // writeByteA = value+128
        if (npc.getDamageDealt2() == 0)
            buf.put(0); // writeByte (normal)
        else if (npc.getHitType2() == Entity.hitType.BURN)
            buf.put(-4); // writeByte (normal, already negative)
        else if (npc.getHitType2() == Entity.hitType.CRIT)
            buf.put(-3); // writeByte (normal, already negative)
        else if (npc.getHitType2() == Entity.hitType.POISON)
            buf.put(-2); // writeByte (normal, already negative)
        else
            buf.put(-1); // writeByte (normal, already negative)
        double hp = Misc.getCurrentHP(npc.getCurrentHealth(), npc.getMaxHealth());
        int value = hp > 4.00 ? (int) hp : hp != 0.0 ? 4 : 0;
        buf.put(value, ValueType.SUBTRACT); // writeByteS = 128-value
        buf.put(100, ValueType.NEGATE); // writeByteC = -value
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
        if (!npc.isWalking() && npc.getDirection() == -1) {
            if (npc.getUpdateFlags().isUpdateRequired()) {
                buf.putBits(1, 1);
                buf.putBits(2, 0);
            } else {
                buf.putBits(1, 0);
            }
        } else {
            npc.setDirection(npc.getNextWalkingDirection());
            if (npc.getDirection() == -1) {
                buf.putBits(1, 1);
                buf.putBits(2, 0);
                return;
            }
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

}
