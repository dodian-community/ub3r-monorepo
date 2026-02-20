package net.dodian.jobs.impl;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.EntityType;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.chunk.Chunk;
import net.dodian.uber.game.model.chunk.ChunkManager;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EntityProcessorChunkSyncRegressionTest {

    private Player[] originalPlayers;
    private ChunkManager originalChunkManager;

    @BeforeEach
    public void setUp() {
        originalPlayers = PlayerHandler.players;
        originalChunkManager = Server.chunkManager;
        PlayerHandler.players = new Player[Constants.maxPlayers + 1];
        Server.chunkManager = new ChunkManager();
    }

    @AfterEach
    public void tearDown() {
        PlayerHandler.players = originalPlayers;
        Server.chunkManager = originalChunkManager;
    }

    @Test
    public void moverCrossingChunkBoundaryIsResyncedBeforeDiscoveryPass() {
        Client mover = new Client(null, 1);
        mover.playerName = "mover";
        mover.isActive = true;

        mover.moveTo(100, 100, 0);
        mover.syncChunkMembership();
        Chunk previousChunk = mover.getCurrentChunk();

        mover.moveTo(108, 100, 0);
        PlayerHandler.players[1] = mover;

        EntityProcessor.syncActivePlayerChunksForTick();

        assertEquals(mover.getPosition().getChunk(), mover.getCurrentChunk());
        assertNotEquals(previousChunk, mover.getCurrentChunk());

        assertFalse(Server.chunkManager.find(new Position(100, 100, 0), EntityType.PLAYER, 0).contains(mover));
        assertTrue(Server.chunkManager.find(mover.getPosition(), EntityType.PLAYER, 0).contains(mover));
    }
}
