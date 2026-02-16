package net.dodian.jobs.impl;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.chunk.Chunk;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.entity.player.PlayerUpdating;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.party.Balloons;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class EntityProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(EntityProcessor.class);
    private static final boolean DEBUG_ADDED_LOCAL_PLAYERS = false;
    private static final boolean DEBUG_NPC_MOVEMENT_WRITES = false;

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        Set<Chunk> activeNpcChunks = buildActiveNpcChunks();
        if (DEBUG_ADDED_LOCAL_PLAYERS) {
            PlayerUpdating.resetDebugAddedLocalCounter();
        }

        // Process NPCs
        for (Npc npc : Server.npcManager.getNpcs()) {
            processNpc(now, npc, activeNpcChunks);
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

        // Keep chunk membership in-sync for all movers before visibility discovery.
        syncActivePlayerChunksForTick();

        // Consume NPC walking direction once per tick, then reuse for all viewers.
        consumeNpcDirectionsForTick();

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

        if (DEBUG_ADDED_LOCAL_PLAYERS) {
            logger.info("addedLocalPlayers={}", PlayerUpdating.consumeDebugAddedLocalCounter());
        }
        if (DEBUG_NPC_MOVEMENT_WRITES) {
            logger.info("npcMovementWrites={}", net.dodian.uber.game.model.entity.npc.NpcUpdating.consumeDebugMovementWriteCounter());
        }
    }

    private void processNpc(long now, Npc npc, Set<Chunk> activeNpcChunks) {
        if (!shouldProcessNpc(npc, activeNpcChunks)) {
            return;
        }

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

        handleNpcRoaming(npc);
        npc.effectChange();
        handleNpcRandomActions(npc);
    }

    private boolean shouldProcessNpc(Npc npc, Set<Chunk> activeNpcChunks) {
        if (npc == null) {
            return false;
        }
        if (npc.isSpawnAlwaysActive()) {
            return true;
        }
        return activeNpcChunks.contains(npc.getPosition().getChunk());
    }

    static boolean withinWalkRadius(Position origin, int targetX, int targetY, int walkRadius) {
        if (walkRadius <= 0) {
            return true;
        }
        return Math.abs(targetX - origin.getX()) <= walkRadius && Math.abs(targetY - origin.getY()) <= walkRadius;
    }

    private void handleNpcRoaming(Npc npc) {
        if (!npc.isAlive() || !npc.isVisible() || npc.isFighting()) {
            return;
        }

        int walkRadius = npc.getWalkRadius();
        if (walkRadius <= 0) {
            return;
        }

        // Keep idle roaming sparse to avoid jittery movement.
        if (Misc.chance(10) != 1) {
            return;
        }

        final int[][] deltas = {
                {-1, -1}, {-1, 0}, {-1, 1},
                {0, -1},           {0, 1},
                {1, -1},  {1, 0},  {1, 1}
        };

        for (int attempt = 0; attempt < deltas.length; attempt++) {
            int[] delta = deltas[Utils.random(deltas.length - 1)];
            int dx = delta[0];
            int dy = delta[1];

            int fromX = npc.getPosition().getX();
            int fromY = npc.getPosition().getY();
            int toX = fromX + dx;
            int toY = fromY + dy;

            if (!withinWalkRadius(npc.getOriginalPosition(), toX, toY, walkRadius)) {
                continue;
            }
            if (!npc.canMove(dx, dy)) {
                continue;
            }

            npc.moveTo(toX, toY, npc.getPosition().getZ());
            npc.markWalkStep(fromX, fromY, toX, toY);
            return;
        }
    }

    static Set<Chunk> buildActiveNpcChunks() {
        Set<Chunk> activeChunks = new HashSet<>();
        for (int i = 0; i < Constants.maxPlayers; i++) {
            Client player = (Client) PlayerHandler.players[i];
            if (player == null || player.disconnected || !player.isActive) {
                continue;
            }
            Chunk center = player.getPosition().getChunk();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    activeChunks.add(center.translate(dx, dy));
                }
            }
        }
        return activeChunks;
    }

    static void syncActivePlayerChunksForTick() {
        for (int i = 0; i < Constants.maxPlayers; i++) {
            Client player = (Client) PlayerHandler.players[i];
            if (player == null || player.disconnected || !player.isActive) {
                continue;
            }
            player.syncChunkMembership();
        }
    }

    private void consumeNpcDirectionsForTick() {
        for (Npc npc : Server.npcManager.getNpcs()) {
            if (npc == null) {
                continue;
            }
            npc.setDirection(npc.getNextWalkingDirection());
        }
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
