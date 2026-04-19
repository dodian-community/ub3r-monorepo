package net.dodian.uber.game.systems.zone

import java.util.LinkedHashSet
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.EntityType
import net.dodian.uber.game.model.entity.player.Client

class ZoneSubscriberIndex {
    fun viewersFor(
        delta: ZoneDelta,
        activePlayers: List<Client>,
    ): List<Client> {
        val chunkManager = Server.chunkManager ?: return activePlayers.filter(delta::appliesTo)
        val candidates = LinkedHashSet<Client>()
        delta.candidateChunkKeys().forEach { key ->
            val chunkX = (key shr 32).toInt()
            val chunkY = key.toInt()
            val repo = chunkManager.getLoaded(chunkX, chunkY) ?: return@forEach
            repo.getAll<Client>(EntityType.PLAYER).forEach(candidates::add)
        }
        return candidates.filter(delta::appliesTo)
    }
}
