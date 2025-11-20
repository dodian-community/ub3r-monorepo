package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.dodian.uber.game.netty.listener.out.SetInterfaceWalkable;



/**
 * Netty implementation of the walking packet handler (opcodes 248, 164, 98).
 * Closely mirrors legacy logic in model/player/packets/incoming/Walking.java.
 */
@net.dodian.uber.game.netty.listener.PacketHandler(opcode = 248)
public final class WalkingListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(WalkingListener.class);

    static {
        WalkingListener handler = new WalkingListener();
        PacketListenerManager.register(248, handler);
        PacketListenerManager.register(164, handler);
        PacketListenerManager.register(98, handler);
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        int opcode = packet.getOpcode();
        int size   = packet.getSize();
        long now   = System.currentTimeMillis();

        if (client.deathStage > 0 || client.getCurrentHealth() < 1 || client.randomed || !client.validClient
                || !client.pLoaded || now < client.walkBlock) {
            return;
        }
        if (client.doingTeleport() || client.getPlunder.looting) return;
        if (opcode != 98) client.setWalkToTask(null);

        // Auto-decline trade/duel when walking
        if (client.inTrade && (opcode == 164 || opcode == 248)) client.declineTrade();
        else if (client.inDuel && !client.duelFight && (opcode == 164 || opcode == 248)) client.declineDuel();

        if (client.genie) client.genie = false;
        if (client.antique) client.antique = false;
        client.playerPotato.clear();

        if ((client.getStunTimer() > 0 || client.getSnareTimer() > 0) && opcode != 98) {
            client.send(new SendMessage(client.getSnareTimer() > 0 ? "You are ensnared!" : "You are currently stunned!"));
            client.resetWalkingQueue();
            return;
        }

        if (client.morph) client.unMorph();
        if (client.checkInv) {
            client.checkInv = false;
            client.resetItems(3214);
        }
        if (client.pickupWanted) {
            client.pickupWanted = false;
            client.attemptGround = null;
        }

        ByteBuf buf = packet.getPayload();
        
        // DEBUG: Buffer validation
        logger.debug("Buffer readable bytes: {}, expected size: {}", buf.readableBytes(), size);
        if (buf.readableBytes() < 5) {
            logger.warn("Insufficient buffer data for walking packet - readable: {}, size: {}", buf.readableBytes(), size);
            return;
        }
        
        // Steps calculation
        int steps = (size - 5) / 2;
        client.newWalkCmdSteps = steps;
        if (++client.newWalkCmdSteps > Player.WALKING_QUEUE_SIZE) {
            client.newWalkCmdSteps = 0;
            return;
        }

        int firstStepX = ByteBufReader.readShort(buf, ValueType.ADD, ByteOrder.BIG) - client.mapRegionX * 8;

        for (int i = 1; i < client.newWalkCmdSteps; i++) {
            client.newWalkCmdX[i] = readSignedByte(buf);
            client.newWalkCmdY[i] = readSignedByte(buf);
            client.tmpNWCX[i] = client.newWalkCmdX[i];
            client.tmpNWCY[i] = client.newWalkCmdY[i];
        }

        int firstStepY = ByteBufReader.readShort(buf, ValueType.NORMAL, ByteOrder.BIG) - client.mapRegionY * 8;
        client.newWalkCmdIsRunning = (readSignedByteC(buf) == 1);

        client.newWalkCmdX[0] = client.newWalkCmdY[0] = client.tmpNWCX[0] = client.tmpNWCY[0] = 0;

        logger.debug("Walk steps {} firstX {} firstY {} running {}", client.newWalkCmdSteps, firstStepX, firstStepY, client.newWalkCmdIsRunning);
        for (int i = 0; i < client.newWalkCmdSteps; i++) {
            client.newWalkCmdX[i] += firstStepX;
            client.newWalkCmdY[i] += firstStepY;
        }



        if (client.newWalkCmdSteps > 0) {
            if (client.inDuel) {
                if (opcode != 98) client.send(new SendMessage("You cannot move during this duel!"));
                client.resetWalkingQueue();
                return;
            }
            // Reset interfaces/actions similar to legacy
            if (client.NpcWanneTalk > 0) {
                client.send(new RemoveInterfaces());
                client.NpcWanneTalk = -1;
            } else if (!client.isBusy()) {
                client.send(new RemoveInterfaces());
            }
            client.rerequestAnim();
            client.resetAction();
            client.discord = false;
            client.playerSkillAction.clear();
            if (client.checkInv) { client.checkInv = false; client.resetItems(3214);}            
            if (opcode != 98 && (client.attackingNpc || client.attackingPlayer)) client.resetAttack();
            client.faceTarget(65535);
        }

        if (client.chestEventOccur && opcode != 98) client.chestEventOccur = false;
        if (client.stairs > 0) client.resetStairs();
        if (client.NpcDialogue == 1001) client.send(new SetInterfaceWalkable(-1));
        client.convoId = -1;
        if (client.NpcDialogue > 0) {
            client.NpcDialogue = 0;
            client.NpcTalkTo = 0;
            client.NpcDialogueSend = false;
            client.send(new RemoveInterfaces());
        }
        if (client.refundSlot != -1) client.refundSlot = -1;
        if (client.herbMaking != -1) client.herbMaking = -1;
        if (client.IsBanking) { client.IsBanking = false; client.send(new RemoveInterfaces()); client.checkItemUpdate(); }
        if (client.checkBankInterface) { client.checkBankInterface = false; client.send(new RemoveInterfaces()); client.checkItemUpdate(); }
        if (client.isPartyInterface) { client.isPartyInterface = false; client.send(new RemoveInterfaces()); client.checkItemUpdate(); }
        if (client.isShopping()) { client.MyShopID = -1; client.send(new RemoveInterfaces()); client.checkItemUpdate(); }

        // done – movement queue will be processed in Client.process()
    }

    /* ------------- Helpers replicating Stream methods ------------- */
    private static int readSignedWordBigEndianA(ByteBuf buf) {
        int low  = (buf.readUnsignedByte() - 128) & 0xFF;
        int high = buf.readUnsignedByte();
        int val  = (high << 8) | low;
        if (val > 32767) val -= 65536;
        return val;
    }

    private static int readSignedWordBigEndian(ByteBuf buf) {
        int low  = buf.readUnsignedByte();
        int high = buf.readUnsignedByte();
        int val  = (high << 8) | low;
        if (val > 32767) val -= 65536;
        return val;
    }

    private static int readSignedByte(ByteBuf buf) {
        return buf.readByte();
    }

    private static int readSignedByteC(ByteBuf buf) {
        return -buf.readByte();
    }
}
