package net.dodian.uber.game.runtime.sync.player

import java.util.IdentityHashMap
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player

class PlayerSyncRevisionIndex {
    private val subjectStates = IdentityHashMap<Player, SubjectState>()
    private val viewerStates = IdentityHashMap<Player, ViewerPlayerSyncState>()

    fun rebuild(activePlayers: List<Client>, tick: Long, activityIndex: PlayerChunkActivityIndex) {
        val seen = IdentityHashMap<Player, Boolean>(activePlayers.size)
        activePlayers.forEach { player ->
            val state = subjectStates.computeIfAbsent(player) { SubjectState() }
            val previousChunk = state.lastChunkX to state.lastChunkY
            val previousLevel = state.lastLevel
            val currentChunk = player.position?.chunk
            val currentLevel = player.position?.z ?: 0

            val moved =
                player.primaryDirection != -1 ||
                    player.secondaryDirection != -1 ||
                    player.didTeleport() ||
                    player.didMapRegionChange() ||
                    currentChunk == null ||
                    previousChunk.first != currentChunk.x ||
                    previousChunk.second != currentChunk.y ||
                    previousLevel != currentLevel
            val updated = player.updateFlags.isUpdateRequired

            if (moved) {
                state.movementRevision++
            }
            if (updated) {
                state.blockRevision++
            }

            if (state.lastChunkX != Int.MIN_VALUE) {
                if (moved || updated) {
                    activityIndex.bump(net.dodian.uber.game.model.chunk.Chunk(state.lastChunkX, state.lastChunkY), previousLevel)
                }
            }
            if (moved || updated) {
                activityIndex.bump(player.position)
            }

            state.lastChunkX = currentChunk?.x ?: Int.MIN_VALUE
            state.lastChunkY = currentChunk?.y ?: Int.MIN_VALUE
            state.lastLevel = currentLevel
            state.lastSeenTick = tick
            seen[player] = true
        }

        val iterator = subjectStates.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (seen.containsKey(entry.key)) {
                continue
            }
            val state = entry.value
            if (state.lastChunkX != Int.MIN_VALUE) {
                activityIndex.bump(net.dodian.uber.game.model.chunk.Chunk(state.lastChunkX, state.lastChunkY), state.lastLevel)
            }
            iterator.remove()
            viewerStates.remove(entry.key)
        }
    }

    fun movementRevision(player: Player): Long = subjectStates[player]?.movementRevision ?: 0L

    fun blockRevision(player: Player): Long = subjectStates[player]?.blockRevision ?: 0L

    fun localActivityStamp(viewer: Player): Long {
        var stamp = 0L
        for (i in 0 until viewer.playerListSize) {
            val local = viewer.playerList[i] ?: continue
            val state = subjectStates[local] ?: continue
            stamp = maxOf(stamp, state.movementRevision, state.blockRevision)
        }
        return stamp
    }

    fun viewerState(viewer: Player): ViewerPlayerSyncState = viewerStates.computeIfAbsent(viewer) { ViewerPlayerSyncState() }

    private class SubjectState {
        var movementRevision: Long = 0L
        var blockRevision: Long = 0L
        var lastChunkX: Int = Int.MIN_VALUE
        var lastChunkY: Int = Int.MIN_VALUE
        var lastLevel: Int = Int.MIN_VALUE
        var lastSeenTick: Long = 0L
    }
}
