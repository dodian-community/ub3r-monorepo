package net.dodian.jobs.impl;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.party.Balloons;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Utils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@DisallowConcurrentExecution

public class NpcProcessor implements Job {

    public void execute(JobExecutionContext context) throws JobExecutionException {
        for (Npc npc : Server.npcManager.getNpcs()) {
            /* Clear the npc update! */
            try {
                npc.clearUpdateFlags();
            } catch (Exception e) {
                e.printStackTrace();
            }
            long now = System.currentTimeMillis();
        if(npc.alive && !npc.isFighting()) {
            npc.setFocus(npc.getPosition().getX() + Utils.directionDeltaX[npc.getFace()], npc.getPosition().getY() + Utils.directionDeltaY[npc.getFace()]);
            npc.getUpdateFlags().setRequired(UpdateFlag.FACE_COORDINATE, true);
        }
        if (!npc.alive && npc.visible && (now - npc.getDeathTime() >= npc.getTimeOnFloor())) {
             npc.setVisible(false);
             npc.drop();
             Client p = npc.getTarget(false);
            /* Jad loot table, up to 5 players */
            if(npc.getId() == 3127)
                for(int i = 0; i < 4 && p != null; i++) {
                    npc.removeEnemy(p);
                    p = npc.getTarget(false);
                    if(p != null) {
                        double chance = (npc.getDamage().get(p) / (double)npc.getMaxHealth()) * 100;
                        double rate = Misc.chance(100000) / 1000D;
                        if(rate <= chance) {
                            npc.drop();
                            p.send(new SendMessage("You managed to roll for the loot!"));
                        } else p.send(new SendMessage("Unlucky! Better luck next time."));
                    }
                }
        }
            if (!npc.alive && !npc.visible && (now - (npc.getDeathTime() + npc.getTimeOnFloor()) >= (npc.getRespawn() * 1000L))) {
                npc.respawn();
            }
            int attackTimer = npc.enraged(20000) ? 600 : npc.boss ? 1800 : 2400;
            if (npc.alive && npc.isFighting() && now - npc.getLastAttack() >= attackTimer) {
                npc.attack();
                npc.setLastAttack(System.currentTimeMillis());
                /*if (npc.getId() == 430 || npc.getId() == 1977)
                    npc.attack_new();
                else if (npc.getId() == 3200)
                    npc.bossAttack();
                else
                    npc.attack();*/
                if(npc.getId() == 2261) {
                    int hp = (int)(npc.getMaxHealth() * 0.40);
                    if(npc.enraged(20000)) {
                        npc.inFrenzy = -1;
                        npc.hadFrenzy = true;
                        npc.sendFightMessage(npc.npcName() + " have calmed down.");
                    } else if(!npc.hadFrenzy && npc.inFrenzy == -1 && npc.getCurrentHealth() < hp) {
                        npc.inFrenzy = System.currentTimeMillis();
                        npc.sendFightMessage(npc.npcName() + " have become enraged!");
                    }
                }
            }
            if (npc.getId() == 3805 && Misc.chance(100) == 1) {
                int jackpot = Math.min(Server.slots.slotsJackpot + Server.slots.peteBalance, Integer.MAX_VALUE);
                npc.setText("Current Jackpot is " + jackpot + " coins!");
                npc.setLastChatMessage();
            }
            if (npc.getId() == 4218 && Misc.chance(8) == 1) {
                npc.setText("Watch out for the plague!!");
                npc.setLastChatMessage();
            }
            if (npc.getId() == 555 && Misc.chance(10) == 1) {
                if(Misc.chance(2) == 1)
                    npc.setText("The plague is coming!");
                else
                    npc.setText("Watch out for the plague!!");
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