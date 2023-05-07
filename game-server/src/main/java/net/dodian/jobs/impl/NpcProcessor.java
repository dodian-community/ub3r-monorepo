package net.dodian.jobs.impl;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.party.Balloons;
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
            npc.clearUpdateFlags();
            if(!npc.isFighting() && npc.isAlive())
                npc.setFocus(npc.getPosition().getX() + Utils.directionDeltaX[npc.getFace()], npc.getPosition().getY() + Utils.directionDeltaY[npc.getFace()]);

            long now = System.currentTimeMillis();
        if(now - npc.lastBoostedStat >= 30000) { //Stat boost!
            npc.changeStat();
        }
        if (!npc.alive && npc.visible && (now - npc.getDeathTime() >= npc.getTimeOnFloor())) {
             npc.setVisible(false);
             npc.drop();
             Client p = npc.getTarget(false);
             npc.removeEnemy(p);
            /* Jad loot table, up to 5 players */
            if(npc.getId() == 3127)
                for(int i = 1; i <= 4 && !npc.getDamage().isEmpty(); i++) { //4 more players if damage not empty.
                    p = npc.getTarget(false);
                    if(p != null) {
                        double chance = (0.1 + (npc.getDamage().get(p) / (double)npc.getMaxHealth())) * 100; //10% + your damage profile!
                        double rate = Misc.chance(100000) / 1000D;
                        if(chance - 10 >= 5 && rate <= chance) {
                            npc.drop();
                            p.send(new SendMessage("You managed to roll for the loot!"));
                        } else if(chance - 10 < 5) p.send(new SendMessage("You were not eligible for the drop!"));
                        else p.send(new SendMessage("Unlucky! Better luck next time."));
                    }
                    npc.removeEnemy(p); //Need to remove the enemy if we done with the check values!
                }
        }
            if (!npc.alive && !npc.visible && (now - (npc.getDeathTime() + npc.getTimeOnFloor()) >= (npc.getRespawn() * 1000L))) {
                npc.respawn();
            }
            if (npc.alive && npc.isFighting() && now - npc.getLastAttack() >= npc.getAttackTimer()) {
                npc.attack();
                if(npc.getId() == 2261) { //Dwayne effect
                    int hp = (int)(npc.getMaxHealth() * 0.40);
                    if(npc.inFrenzy != -1 && !npc.enraged(20000)) {
                        npc.calmedDown();
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
            }
            if (npc.getId() == 4218 && Misc.chance(8) == 1) {
                npc.setText("Watch out for the plague!!");
            }
            if (npc.getId() == 2805 && Misc.chance(50) == 1) {
                npc.setText(Misc.chance(2) == 1 ? "Moo" : "Moo!!");
            }
            if (npc.getId() == 555 && Misc.chance(10) == 1) {
               npc.setText(Misc.chance(2) == 1 ? "The plague is coming!" : "Watch out for the plague!!");
            }
            if (npc.getId() == 6797 && Misc.chance(10) == 1) {
                npc.setText(Misc.chance(2) == 1 ? "Something is in there!" : "Keep the cave cloased!");
            }
            if (npc.getId() == 5792 && Balloons.eventActive()) {
                    npc.requestAnim(866, 0);
                    npc.setText(Balloons.spawnedBalloons() ? "A party is going on right now!" : "A party is about to Start!!!!");
            }
            if (npc.getId() == 3306 && Misc.chance(25) == 1) {
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