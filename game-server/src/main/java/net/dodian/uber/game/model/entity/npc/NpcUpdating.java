package net.dodian.uber.game.model.entity.npc;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.entity.EntityUpdating;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.utilities.Stream;
import net.dodian.utilities.Utils;

import java.util.Iterator;

/**
 * @author Dashboard
 */
public class NpcUpdating extends EntityUpdating<Npc> {

    private static NpcUpdating instance = new NpcUpdating();

    public static NpcUpdating getInstance() {
        return instance;
    }

    @Override
    public void update(Player player, Stream stream) {
        Stream block = new Stream(new byte[10000]);
        block.currentOffset = 0;

        stream.createFrameVarSizeWord(65);
        stream.initBitAccess();

        stream.writeBits(8, player.getLocalNpcs().size());
        for (Iterator<Npc> i = player.getLocalNpcs().iterator(); i.hasNext(); ) {
            Npc npc = i.next();
            if (player.withinDistance(npc) && npc.isVisible()) {
                updateNPCMovement(npc, stream);
                if (npc.getUpdateFlags().isUpdateRequired())
                    appendBlockUpdate(npc, block);
            } else {
                stream.writeBits(1, 1);
                stream.writeBits(2, 3); // tells client to remove this npc from list
                i.remove();
            }
        }

        for (Npc npc : Server.npcManager.getNpcs()) {
            if (npc == null || !(player.withinDistance(npc) && npc.isVisible()) || !npc.isVisible()) continue;
            if (player.getLocalNpcs().add(npc)) {
                addNpc(player, npc, stream);
                npc.getUpdateFlags().setRequired(UpdateFlag.DUMMY, true);
                appendBlockUpdate(npc, block);
            }

        }

        if (block.currentOffset > 0) {
            stream.writeBits(14, 16383);
            stream.finishBitAccess();

            stream.writeBytes(block.buffer, block.currentOffset, 0);
        } else {
            stream.finishBitAccess();
        }
        stream.endFrameVarSizeWord();
    }


    public static void addNpc(Player player, Npc npc, Stream stream) {
        stream.writeBits(14, npc.getSlot());
        Position delta = Position.delta(player.getPosition(), npc.getPosition());
        stream.writeBits(5, delta.getY());
        stream.writeBits(5, delta.getX());
        stream.writeBits(1, npc.getUpdateFlags().isUpdateRequired() ? 1 : 0);
        stream.writeBits(14, npc.getId());
        stream.writeBits(1, 1);
    }

    @Override
    public void appendBlockUpdate(Npc npc, Stream stream) {
        if (!npc.getUpdateFlags().isUpdateRequired())
            return;

        int updateMask = 0x0;
        for (UpdateFlag flag : npc.getUpdateFlags().keySet()) {
            if (npc.getUpdateFlags().isRequired(flag)) {
                updateMask |= flag.getMask(npc.getType());
            }
        }
        stream.writeByte(updateMask);

        if (npc.getUpdateFlags().isRequired(UpdateFlag.FORCED_CHAT))
            appendTextUpdate(npc, stream);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.ANIM))
            appendAnimationRequest(npc, stream);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.HIT))
            appendPrimaryHit(npc, stream);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.FACE_COORDINATE))
            appendFaceCoordinates(npc, stream);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.FACE_CHARACTER))
            appendFaceCharacter(npc, stream);
    }

    public void appendTextUpdate(Npc npc, Stream stream) {
        stream.writeString(npc.getText());
    }

    @Override
    public void appendAnimationRequest(Npc npc, Stream stream) {
        stream.writeWordBigEndian(npc.getAnimationId());
        stream.writeByte(npc.getAnimationDelay());
    }

    @Override
    public void appendPrimaryHit(Npc npc, Stream stream) {
        stream.writeByteC(npc.getDamageDealt());
        if (npc.getDamageDealt() == 0) {
            stream.writeByteS(0);
        } else if (!npc.isCrit()) {
            stream.writeByteS(1);
        } else {
            stream.writeByteS(3);
        }
        stream.writeByteS(Npc.getCurrentHP(npc.getCurrentHealth(), npc.getMaxHealth(), 100));
        stream.writeByteC(100);
    }

    @Override
    public void appendFaceCoordinates(Npc npc, Stream stream) {
        stream.writeWordBigEndian(npc.getFacePosition().getX());
        stream.writeWordBigEndian(npc.getFacePosition().getY());
    }

    @Override
    public void appendFaceCharacter(Npc npc, Stream stream) {
        stream.writeWordBigEndian(npc.getViewX());
        stream.writeWordBigEndian(npc.getViewY());
    }

    public void updateNPCMovement(Npc npc, Stream stream) {
        if (!npc.isWalking() && npc.getDirection() == -1) {
            if (npc.getUpdateFlags().isUpdateRequired()) {
                stream.writeBits(1, 1);
                stream.writeBits(2, 0);
            } else {
                stream.writeBits(1, 0);
            }
        } else {
            npc.setDirection(npc.getNextWalkingDirection());
            if (npc.getDirection() == -1) {
                System.out.println("aborting walk");
                stream.writeBits(1, 1);
                stream.writeBits(2, 0);
                return;
            }
            stream.writeBits(1, 1);
            stream.writeBits(2, 1);
            stream.writeBits(3, Utils.xlateDirectionToClient[npc.getDirection()]);
            if (npc.getUpdateFlags().isUpdateRequired()) {
                stream.writeBits(1, 1);
            } else {
                stream.writeBits(1, 0);
            }
        }
    }

}
