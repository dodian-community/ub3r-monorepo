package net.dodian.jobs.impl;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.party.Balloons;
import net.dodian.utilities.Misc;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@DisallowConcurrentExecution

public class NpcProcessor implements Job {

    public void execute(JobExecutionContext context) throws JobExecutionException {
        for (Npc npc : Server.npcManager.getNpcs()) {
            long now = System.currentTimeMillis();
            npc.clearUpdateFlags();
      /*if(npc.alive) {
        npc.setFocus(npc.getPosition().getX() + Utils.directionDeltaX[npc.getFace()], npc.getPosition().getY() + Utils.directionDeltaY[npc.getFace()]);
        npc.getUpdateFlags().setRequired(UpdateFlag.FACE_COORDINATE, true);
      }*/
            if (!npc.alive && npc.visible && (now - npc.getDeathTime() >= npc.getTimeOnFloor())) {
                npc.setVisible(false);
                npc.drop();
            }
            if (!npc.alive && !npc.visible && (now - (npc.getDeathTime() + npc.getTimeOnFloor()) >= (npc.getRespawn() * 1000))) {
                npc.respawn();
            }
            if (npc.alive && npc.isFighting() && now - npc.getLastAttack() >= 2000) {
                if (npc.getId() == 430 || npc.getId() == 1977)
                    npc.attack_new();
                else if (npc.getId() == 3200)
                    npc.bossAttack();
                else
                    npc.attack();
                npc.setLastAttack(System.currentTimeMillis());
            }
            if (npc.getId() == 3805 && Misc.random(100) <= 1) {
                int jackpot = Server.slots.slotsJackpot + Server.slots.peteBalance >= Integer.MAX_VALUE ? Integer.MAX_VALUE : Server.slots.slotsJackpot + Server.slots.peteBalance;
                npc.setText("Current Jackpot is " + jackpot + " coins!");
                npc.setLastChatMessage();
            }
            if (npc.getId() == 5792 && Balloons.eventActive()) {
                if (Misc.random(1) == 0) {
                    int[] danceEmote = {1835, 866};
                    npc.requestAnim(danceEmote[Misc.random(danceEmote.length - 1)], 0);
                } else
                    npc.setText(Balloons.spawnedBalloons() ? "A party is going on right now!" : "A party is about to Start!!!!");
            }
            if (npc.getId() == 3306 && Misc.random(19) < 1) {
                int peopleInEdge = 0;
                int peopleInWild = 0;
                for (int a = 0; a < Constants.maxPlayers; a++) {
                    Client checkPlayer = (Client) PlayerHandler.players[a];
                    if (checkPlayer != null) {
                        if (checkPlayer.inWildy())
                            peopleInWild++;
                        else if (checkPlayer.inEdgeville())
                            peopleInEdge++;
                    }
                }
                npc.setText("There is currently " + peopleInWild + " player" + (peopleInWild != 1 ? "s" : "") + " in the wild and " + peopleInEdge + " player" + (peopleInEdge != 1 ? "s" : "") + " in Edgeville!");
            }
//      if (npc.getId() == 2676) {
//        if (npc.getLastChatMessage() < System.currentTimeMillis() - 45000) {
//          npc.setText((VotingIncentiveManager.getMilestone() - VotingIncentiveManager.getVotes()) + " votes left until the next drop party!");
//          npc.setLastChatMessage();
//        } else if(System.currentTimeMillis() - npc.getLastChatMessage() < 5000) {
//          npc.setText((VotingIncentiveManager.getMilestone() - VotingIncentiveManager.getVotes()) + " votes left until the next drop party!");
//        }
//      }
        }
    }

}