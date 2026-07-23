package net.dodian.uber.game.engine.systems.zone

import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.config.gameMaxPlayers
import net.dodian.uber.game.engine.sync.util.SlotBitSet
import net.dodian.uber.game.model.EntityType
import net.dodian.uber.game.model.entity.player.Client

/**
 * Game-thread-only: called exclusively from [ZoneUpdateBus.flush] inside
 * [net.dodian.uber.game.engine.sync.WorldSynchronizationService]'s SYNC_FLUSH stage, never from
 * the parallel per-viewer encode fan-out, so the reused scratch fields below need no locking.
 */
class ZoneSubscriberIndex {
    private val seen = SlotBitSet(gameMaxPlayers + 1)
    private val candidates = ArrayList<Client>()

    /** Candidate viewers for [delta], deduped by chunk membership but not yet filtered by [ZoneDelta.appliesTo]. */
    fun candidatesFor(
        delta: ZoneDelta,
        activePlayers: List<Client>,
    ): List<Client> {
        val chunkManager = Server.chunkManager ?: return activePlayers
        seen.clear()
        candidates.clear()
        for (chunkX in delta.minChunkX..delta.maxChunkX) {
            for (chunkY in delta.minChunkY..delta.maxChunkY) {
                val repo = chunkManager.getLoaded(chunkX, chunkY) ?: continue
                for (client in repo.getAll<Client>(EntityType.PLAYER)) {
                    if (seen.add(client.slot)) {
                        candidates.add(client)
                    }
                }
            }
        }
        return candidates
    }
}