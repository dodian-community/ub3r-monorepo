package net.dodian.uber.game.model.player.packets.incoming;

import static net.dodian.utilities.DotEnvKt.getServerEnv;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.player.packets.outgoing.RemoveInterfaces;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.networking.game.ByteBufPacket;

import static net.dodian.utilities.DotEnvKt.getServerEnv;


public class WalkingBuf implements ByteBufPacket {

    /* ----------------------- helper readers (Jagex variants) ----------------------- */
    private static int readSignedShortBE(ByteBuf buf) {
        int low = buf.readByte() & 0xFF;
        int high = buf.readByte() & 0xFF;
        int val = (high << 8) | low;
        return val > 32767 ? val - 0x10000 : val;
    }

    private static int readSignedShortBEA(ByteBuf buf) {
        int low = (buf.readByte() - 128) & 0xFF;
        int high = buf.readByte() & 0xFF;
        int val = (high << 8) | low;
        return val > 32767 ? val - 0x10000 : val;
    }

    private static int readSignedShortLE(ByteBuf buf) {
        int low = buf.readByte() & 0xFF;
        int high = buf.readByte() & 0xFF;
        int val = (high << 8) | low;
        return val > 32767 ? val - 0x10000 : val;
    }

    private static int readSignedShortLEA(ByteBuf buf) {
        int low = (buf.readByte() - 128) & 0xFF;
        int high = buf.readByte() & 0xFF;
        int val = (high << 8) | low;
        return val > 32767 ? val - 0x10000 : val;
    }

    /**
     * Reads a type A byte (value - 128) as used by the Runescape protocol.
     */
    private static byte readSignedByteA(ByteBuf buf) {
        return (byte) (buf.readByte() - 128);
    }

    /**
     * Reads a type C byte (negated value) as used by the Runescape protocol.
     */
    private static byte readSignedByteC(ByteBuf buf) {
        return (byte) (-buf.readByte());
    }

    @Override
    public void process(Client client, int opcode, int size, ByteBuf payload) {
        long now = System.currentTimeMillis();

        // Debug: dump raw bytes for minimap opcode 98
        if (opcode == 98 && "dev".equals(getServerEnv())) {
            byte[] dump = new byte[payload.readableBytes()];
            payload.getBytes(payload.readerIndex(), dump);
            StringBuilder sb = new StringBuilder();
            for (byte b : dump) sb.append(String.format("%02X ", b));
            System.out.println("[DEBUG] opcode 98 raw (" + dump.length + "): " + sb);
        }

        int packetSize = size;
        if (opcode == 248) {
            packetSize -= 14; // 14-byte anti-cheat block is at the end of the packet, not the start
        }

        /* ------------------------ pre-condition checks ------------------------- */
        if (client.deathStage > 0 || client.getCurrentHealth() < 1 || client.randomed || !client.validClient || !client.pLoaded || now < client.walkBlock) {
            return;
        }
        if (client.doingTeleport() || client.getPlunder.looting) {
            return;
        }
        if (opcode != 98) {
            client.setWalkToTask(null);
        }
        if (client.inTrade && (opcode == 164 || opcode == 248)) {
            client.declineTrade();
        } else if (client.inDuel && !client.duelFight && (opcode == 164 || opcode == 248)) {
            client.declineDuel();
        }

        if (client.genie) client.genie = false;
        if (client.antique) client.antique = false;
        if (!client.playerPotato.isEmpty()) client.playerPotato.clear();

        if (client.getStunTimer() > 0 || client.getSnareTimer() > 0) {
            client.send(new SendMessage(client.getSnareTimer() > 0 ? "You are ensnared!" : "You are currently stunned!"));
            client.resetWalkingQueue();
            return;
        }

        /* ------------------------ read path data ------------------------- */
        int steps = packetSize - 5;
        steps /= 2;
        client.newWalkCmdSteps = steps;
        if (++client.newWalkCmdSteps > Player.WALKING_QUEUE_SIZE) {
            client.newWalkCmdSteps = 0;
            return;
        }

        int firstStepX;
        int firstStepY;
        if (opcode == 213) { // minimap click uses little endian
            firstStepX = readSignedShortLEA(payload);
        } else {
            firstStepX = readSignedShortBEA(payload);
        }
        firstStepX -= client.mapRegionX * 8;

        for (int i = 1; i < client.newWalkCmdSteps; i++) {
            client.newWalkCmdX[i]  = payload.readByte();
            client.newWalkCmdY[i]  = payload.readByte();
            client.tmpNWCX[i]      = client.newWalkCmdX[i];
            client.tmpNWCY[i]      = client.newWalkCmdY[i];
        }
        client.newWalkCmdX[0] = client.newWalkCmdY[0] = client.tmpNWCX[0] = client.tmpNWCY[0] = 0;

        if (opcode == 213) {
            firstStepY = readSignedShortLE(payload);
        } else {
            firstStepY = readSignedShortBE(payload);
        }
        firstStepY -= client.mapRegionY * 8;

        client.newWalkCmdIsRunning = (readSignedByteC(payload) == 1);

        if (opcode == 213 && getServerEnv().equals("dev")) {
            System.out.println("[DEBUG] opcode 213 steps=" + client.newWalkCmdSteps + " firstX=" + firstStepX + " firstY=" + firstStepY);
        }

        for (int i = 0; i < client.newWalkCmdSteps; i++) {
            client.newWalkCmdX[i] += firstStepX;
            client.newWalkCmdY[i] += firstStepY;
        }

        if (opcode == 98 && getServerEnv().equals("dev")) {
            System.out.println("walking: " + client.newWalkCmdSteps);
        }

        /* After we have consumed the walking data, discard the trailing 14 bytes for opcode 248 */
        if (opcode == 248) {
            int remaining = payload.readableBytes();
            if (remaining >= 14) {
                payload.skipBytes(14);
            }
        }

        /* ------------------------ post-read logic (mirrors legacy) ------------------------- */
        if (client.newWalkCmdSteps > 0) {
            if (client.inDuel) {
                if (opcode != 98) {
                    client.send(new SendMessage("You cannot move during this duel!"));
                }
                client.resetWalkingQueue();
                return;
            }

            if (client.NpcWanneTalk > 0) {
                client.send(new RemoveInterfaces());
                client.NpcWanneTalk = -1;
            } else if (!client.isBusy()) {
                client.send(new RemoveInterfaces());
            }

            client.rerequestAnim();
            client.resetAction();
            client.discord = false;
            if (!client.playerSkillAction.isEmpty()) client.playerSkillAction.clear();
            if (client.checkInv) {
                client.checkInv = false;
                client.resetItems(3214);
            }
            if (opcode != 98 && (client.attackingNpc || client.attackingPlayer)) {
                client.resetAttack();
            }
            client.faceTarget(65535);
        }

        if (client.chestEventOccur && opcode != 98) client.chestEventOccur = false;
        if (client.stairs > 0) client.resetStairs();
        if (client.NpcDialogue == 1001) client.setInterfaceWalkable(-1);
        client.convoId = -1;
        if (client.NpcDialogue > 0) {
            client.NpcDialogue = 0;
            client.NpcTalkTo = 0;
            client.NpcDialogueSend = false;
            client.send(new RemoveInterfaces());
        }
        if (client.refundSlot != -1) client.refundSlot = -1;
        if (client.herbMaking != -1) client.herbMaking = -1;

        /* banking & shopping close on walk */
        if (client.IsBanking) {
            client.IsBanking = false;
            client.send(new RemoveInterfaces());
            client.checkItemUpdate();
        }
        if (client.checkBankInterface) {
            client.checkBankInterface = false;
            client.send(new RemoveInterfaces());
            client.checkItemUpdate();
        }
        if (client.isPartyInterface) {
            client.isPartyInterface = false;
            client.send(new RemoveInterfaces());
            client.checkItemUpdate();
        }
        if (client.isShopping()) {
            client.MyShopID = -1;
            client.send(new RemoveInterfaces());
            client.checkItemUpdate();
        }
    }
}
