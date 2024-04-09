package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.RemoveInterfaces;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;

import static net.dodian.utilities.DotEnvKt.getServerEnv;

public class Walking implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        long currentTime = System.currentTimeMillis();
        if (packetType == 248)
            packetSize -= 14;
        if(client.deathStage > 0 || client.getCurrentHealth() < 1 || client.randomed || !client.validClient || !client.pLoaded || currentTime < client.walkBlock) {
            return;
        }
        if(client.doingTeleport() || client.getPlunder.looting) { //In the midst of something like plunder or teleport
            return;
        }
        if (packetType != 98 && packetType != 164) {
            client.setWalkToTask(null);
        }
        if(client.chestEventOccur && packetType != 98) client.chestEventOccur = false;
        /* Auto decline when walk away from trade! */
        if(client.inTrade && packetType != 164) client.declineTrade();
        /* Auto decline when walk away from duel! */
        else if(client.inDuel && !client.duelFight && packetType != 164) client.declineDuel();
        if (client.genie) client.genie = false;
        if (client.antique) client.antique = false;
        if (!client.playerPotato.isEmpty()) client.playerPotato.clear();
        if(client.getStunTimer() > 0 || client.getSnareTimer() > 0) { //In the midst of a teleport to stop movement!
            client.send(new SendMessage(client.getSnareTimer() > 0 ? "You are ensnared!" : "You are currently stunned!"));
            client.resetWalkingQueue();
            return;
        }
        /* Combat checks! */
        if(client.attackingNpc || client.attackingPlayer) //Adding a check for reset due to walking away!
            client.resetAttack();
        client.faceTarget(-1);
        /* Check a players inventory! */
        if (client.checkInv) {
            client.checkInv = false;
            client.resetItems(3214);
        }
        /* Combat checks! */
        if(packetType != 98 && (client.getStunTimer() > 0 || client.getSnareTimer() > 0)) { //In the midst of a teleport to stop movement!
            client.send(new SendMessage(client.getSnareTimer() > 0 ? "You are ensnared!" : "You are currently stunned!"));
            client.resetWalkingQueue();
            return;
        }
        /* Code for when you trigger the walking! */
            client.newWalkCmdSteps = packetSize - 5;
            client.newWalkCmdSteps /= 2;
            if (++client.newWalkCmdSteps > Player.WALKING_QUEUE_SIZE) {
                client.newWalkCmdSteps = 0;
                return;
            }
            int firstStepX = client.getInputStream().readSignedWordBigEndianA();
            firstStepX -= client.mapRegionX * 8;
            for (int i = 1; i < client.newWalkCmdSteps; i++) {
                client.newWalkCmdX[i] = client.getInputStream().readSignedByte();
                client.newWalkCmdY[i] = client.getInputStream().readSignedByte();
                client.tmpNWCX[i] = client.newWalkCmdX[i];
                client.tmpNWCY[i] = client.newWalkCmdY[i];
            }
            client.newWalkCmdX[0] = client.newWalkCmdY[0] = client.tmpNWCX[0] = client.tmpNWCY[0] = 0;
            int firstStepY = client.getInputStream().readSignedWordBigEndian();
            firstStepY -= client.mapRegionY * 8;
            client.newWalkCmdIsRunning = client.getInputStream().readSignedByteC() == 1;
            for (int i = 0; i < client.newWalkCmdSteps; i++) {
                client.newWalkCmdX[i] += firstStepX;
                client.newWalkCmdY[i] += firstStepY;
            }
            if(packetType == 98 && getServerEnv().equals("dev"))
                System.out.println("walking: " + client.newWalkCmdSteps);
            /* Closing of stuff if we send "walk" value! */
            if(client.newWalkCmdSteps > 0) {
                if (client.inDuel/* && (duelRule[5] || duelRule[9]) */) {
                    if(packetType != 98)
                        client.send(new SendMessage("You cannot move during this duel!"));
                    client.resetWalkingQueue();
                    return;
                }
                client.send(new RemoveInterfaces());
                client.rerequestAnim();
                client.resetAction();
                client.discord = false;
                if(!client.playerSkillAction.isEmpty()) client.playerSkillAction.clear(); //Need to clear if you walk away!
            /* Check a players inventory! */
            if (client.checkInv) {
                client.checkInv = false;
                client.resetItems(3214);
            }
            if(client.attackingNpc || client.attackingPlayer) //Adding a check for reset due to walking away!
                client.resetAttack();
            client.faceTarget(-1);
            }
            // stairs check
            if (client.stairs > 0) {
                client.resetStairs();
            }
            // Npc Talking
            if (client.NpcDialogue == 1001) client.setInterfaceWalkable(-1);
            client.convoId = -1;
            if (client.NpcDialogue > 0) {
                client.NpcDialogue = 0;
                client.NpcTalkTo = 0;
                client.NpcDialogueSend = false;
                client.send(new RemoveInterfaces());
            }
            if(client.refundSlot != -1) client.refundSlot = -1;
            // banking
            if (client.IsBanking) {
                client.send(new RemoveInterfaces());
                client.IsBanking = false;
                client.checkItemUpdate();
            }
            if(client.checkBankInterface) {
                client.checkBankInterface = false;
                client.send(new RemoveInterfaces());
                client.checkItemUpdate();
            }
            if (client.isPartyInterface) {
                client.isPartyInterface = false;
                client.send(new RemoveInterfaces());
                client.checkItemUpdate();
            }
            // shopping
            if (client.isShopping()) {
                client.MyShopID = -1;
                client.checkItemUpdate();
                client.send(new RemoveInterfaces());
            }
    }

}