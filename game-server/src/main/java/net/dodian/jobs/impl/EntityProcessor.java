package net.dodian.jobs.impl;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.party.Balloons;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Utils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class EntityProcessor implements Job {

    public void execute(JobExecutionContext context) throws JobExecutionException {
        long now = System.currentTimeMillis();

        // Process NPCs
        for (Npc npc : Server.npcManager.getNpcs()) {
            processNpc(now, npc);
        }

        // End server when update finished
        if (Server.updateRunning && now - Server.updateStartTime > (Server.updateSeconds * 1000L)) {
            if (PlayerHandler.getPlayerCount() < 1) {
                System.exit(0);
            }
        }

        // Handle server cycles
        handleServerCycles();

        // Process players
        for (int i = 0; i < Constants.maxPlayers; i++) {
            Client player = (Client) PlayerHandler.players[i];
            if (player == null || player.disconnected || !player.isActive) {
                continue;
            }
            processPlayer(player);
        }

        // After processing update
        for (int i = 0; i < Constants.maxPlayers; i++) {
            Client player = (Client) PlayerHandler.players[i];
            if (player == null || !player.isActive) {
                continue;
            }
            updatePlayer(player, i);
        }

        // Clear all update flags
        for (Npc npc : Server.npcManager.getNpcs()) {
            if (npc != null) npc.clearUpdateFlags();
        }
        for (int i = 0; i < Constants.maxPlayers; i++) {
            Client player = (Client) PlayerHandler.players[i];
            if (player != null && player.isActive) {
                player.clearUpdateFlags();
            }
        }
    }

    private void processNpc(long now, Npc npc) {
        if (!npc.isFighting() && npc.isAlive()) {
            npc.setFocus(npc.getPosition().getX() + Utils.directionDeltaX[npc.getFace()], npc.getPosition().getY() + Utils.directionDeltaY[npc.getFace()]);
        }

        if (now - npc.lastBoostedStat >= 30000) {
            npc.changeStat();
        }

        if (!npc.alive && npc.visible && (now - npc.getDeathTime() >= npc.getTimeOnFloor())) {
            handleNpcDeath(npc);
        }

        if (!npc.alive && !npc.visible && (now - (npc.getDeathTime() + npc.getTimeOnFloor()) >= (npc.getRespawn() * 1000L))) {
            npc.respawn();
        }

        if (npc.getLastAttack() > 0) {
            npc.setLastAttack(npc.getLastAttack() - 1); // Decrease attack timer manually
        }

        if (npc.alive && npc.isFighting() && npc.getLastAttack() == 0) {
            npc.attack();
            handleNpcSpecialCases(npc);
        }

        npc.effectChange();
        handleNpcRandomActions(npc);
    }

    private void handleNpcDeath(Npc npc) {
        npc.setVisible(false);
        npc.drop();
        Client p = npc.getTarget(false);
        npc.removeEnemy(p);

        if (isJadNpc(npc)) {
            handleJadLoot(npc, p);
        } else if (isNewBossNpc(npc)) {
            handleNewBossLoot(npc, p);
        }
    }

    private boolean isJadNpc(Npc npc) {
        return npc.getId() == 3127;
    }

    private boolean isNewBossNpc(Npc npc) {
        return npc.getId() == 4303 || npc.getId() == 4304 || npc.getId() == 6610;
    }

    private void handleJadLoot(Npc npc, Client p) {
        for (int i = 1; i <= 4 && !npc.getDamage().isEmpty(); i++) {
            p = npc.getTarget(false);
            if (p != null) {
                handleLootRoll(npc, p);
            }
            npc.removeEnemy(p);
        }
    }

    private void handleNewBossLoot(Npc npc, Client p) {
        p = npc.getSecondTarget(p, false);
        if (p != null) {
            handleLootRoll(npc, p);
        }
        npc.removeEnemy(p);
    }

    private void handleLootRoll(Npc npc, Client p) {
        double chance = (0.1 + (npc.getDamage().get(p) / (double) npc.getMaxHealth())) * 100;
        double rate = Misc.chance(100000) / 1000D;
        if (chance - 10 >= 5 && rate <= chance) {
            npc.drop();
            p.send(new SendMessage("You managed to roll for the loot!"));
        } else if (chance - 10 < 5) {
            p.send(new SendMessage("You were not eligible for the drop!"));
        } else {
            p.send(new SendMessage("Unlucky! Better luck next time."));
        }
    }

    private void handleNpcSpecialCases(Npc npc) {
        if (npc.getId() == 2261) {
            handleDwayneEffect(npc);
        }
    }

    private void handleDwayneEffect(Npc npc) {
        int hp = (int) (npc.getMaxHealth() * 0.40);
        if (npc.inFrenzy != -1 && !npc.enraged(20000)) {
            npc.calmedDown();
            npc.sendFightMessage(npc.npcName() + " have calmed down.");
        } else if (!npc.hadFrenzy && npc.inFrenzy == -1 && npc.getCurrentHealth() < hp) {
            npc.inFrenzy = System.currentTimeMillis();
            npc.sendFightMessage(npc.npcName() + " have become enraged!");
        }
    }

    private void handleNpcRandomActions(Npc npc) {
        int npcId = npc.getId();
        switch (npcId) {
            case 3805:
                handleJackpotAnnouncement(npc);
                break;
            case 4218:
                handlePlagueWarning(npc);
                break;
            case 2805:
                handleCowMooing(npc);
                break;
            case 5924:
                handleNpcAnimation(npc, 6549, 20);
                break;
            case 555:
                handlePlagueMessage(npc);
                break;
            case 5792:
                handlePartyAnnouncement(npc);
                break;
            case 3306:
                handlePlayerCountsAnnouncement(npc);
                break;
            default:
                break;
        }
    }

    private void handleJackpotAnnouncement(Npc npc) {
        if (Misc.chance(100) == 1) {
            int jackpot = Math.min(Server.slots.slotsJackpot + Server.slots.peteBalance, Integer.MAX_VALUE);
            npc.setText("Current Jackpot is " + jackpot + " coins!");
        }
    }

    private void handlePlagueWarning(Npc npc) {
        if (Misc.chance(8) == 1) {
            npc.setText("Watch out for the plague!!");
        }
    }

    private void handleCowMooing(Npc npc) {
        if (Misc.chance(50) == 1) {
            npc.setText(Misc.chance(2) == 1 ? "Moo" : "Moo!!");
        }
    }

    private void handleNpcAnimation(Npc npc, int animId, int chance) {
        if (Misc.chance(chance) == 1) {
            npc.requestAnim(animId, 0);
        }
    }

    private void handlePlagueMessage(Npc npc) {
        if (Misc.chance(10) == 1) {
            npc.setText(Misc.chance(2) == 1 ? "The plague is coming!" : "Watch out for the plague!!");
        }
    }

    private void handlePartyAnnouncement(Npc npc) {
        if (Balloons.eventActive()) {
            npc.requestAnim(866, 0);
            npc.setText(Balloons.spawnedBalloons() ? "A party is going on right now!" : "A party is about to Start!!!!");
        }
    }

    private void handlePlayerCountsAnnouncement(Npc npc) {
        if (Misc.chance(25) == 1) {
            int peopleInEdge = 0;
            int peopleInWild = 0;
            for (int i = 0; i < Constants.maxPlayers; i++) {
                Client checkPlayer = (Client) PlayerHandler.players[i];
                if (checkPlayer != null) {
                    if (checkPlayer.inWildy()) {
                        peopleInWild++;
                    } else if (checkPlayer.inEdgeville()) {
                        peopleInEdge++;
                    }
                }
            }
            npc.setText("There is currently " + peopleInWild + " player" + (peopleInWild != 1 ? "s" : "") + " in the wild and " + peopleInEdge + " player" + (peopleInEdge != 1 ? "s" : "") + " in Edgeville!");
        }
    }

    private void handleServerCycles() {
        if (PlayerHandler.cycle % 10 == 0) {
            Server.connections.clear();
            Server.nullConnections = 0;
        }
        if (PlayerHandler.cycle % 100 == 0) {
            Server.banned.clear();
        }
        if (PlayerHandler.cycle > 10000) {
            PlayerHandler.cycle = 0;
        }
        PlayerHandler.cycle++;
    }

    private void processPlayer(Client player) {
        if (!player.initialized) {
            player.initialize();
            player.initialized = true;
        }
        player.process();
        while (player.packetProcess()) ;
        player.postProcessing();
        player.getNextPlayerMovement();
    }

    private void updatePlayer(Client player, int playerIndex) {
        if (player.timeOutCounter >= 84) {
            player.disconnected = true;
            player.println_debug("\nRemove non-responding " + player.getPlayerName() + " after 60 seconds of disconnect! ");
        }

        if (player.disconnected) {
            player.println_debug("\nRemove disconnected player " + player.getPlayerName());
            Server.playerHandler.removePlayer(player);
            player.disconnected = false;
            PlayerHandler.players[playerIndex] = null; // Use playerIndex directly
        } else {
            player.update();
        }
    }
}
