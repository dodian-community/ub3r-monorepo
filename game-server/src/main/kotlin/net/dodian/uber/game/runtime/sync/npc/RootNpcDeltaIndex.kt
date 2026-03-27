package net.dodian.uber.game.runtime.sync.npc

import java.util.IdentityHashMap
import net.dodian.uber.game.model.chunk.Chunk
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

class RootNpcDeltaIndex {
    private val subjectStates = IdentityHashMap<Npc, SubjectState>()
    private val viewerStates = IdentityHashMap<Client, ViewerNpcSyncState>()

    fun rebuild(
        npcs: Collection<Npc>,
        tick: Long,
        activityIndex: NpcChunkActivityIndex,
    ) {
        val seen = IdentityHashMap<Npc, Boolean>(npcs.size)
        npcs.forEach { npc ->
            val state = subjectStates.computeIfAbsent(npc) { SubjectState() }
            val currentX = npc.position?.x ?: Int.MIN_VALUE
            val currentY = npc.position?.y ?: Int.MIN_VALUE
            val currentChunk = npc.position?.chunk
            val currentLevel = npc.position?.z ?: 0
            val moved =
                currentChunk == null ||
                    state.lastX != currentX ||
                    state.lastY != currentY ||
                    state.lastChunkX != currentChunk.x ||
                    state.lastChunkY != currentChunk.y ||
                    state.lastLevel != currentLevel
            val updated = npc.updateFlags.isUpdateRequired
            val visibilityChanged = state.lastVisible != npc.isVisible
            if (moved) {
                state.movementRevision++
            }
            if (updated || visibilityChanged) {
                state.blockRevision++
            }
            if (state.lastChunkX != Int.MIN_VALUE && (moved || updated || visibilityChanged)) {
                activityIndex.bump(Chunk(state.lastChunkX, state.lastChunkY), state.lastLevel)
            }
            if (moved || updated || visibilityChanged) {
                activityIndex.bump(npc.position)
            }
            state.lastX = currentX
            state.lastY = currentY
            state.lastChunkX = currentChunk?.x ?: Int.MIN_VALUE
            state.lastChunkY = currentChunk?.y ?: Int.MIN_VALUE
            state.lastLevel = currentLevel
            state.lastVisible = npc.isVisible
            state.lastSeenTick = tick
            seen[npc] = true
        }

        val iterator = subjectStates.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (seen.containsKey(entry.key)) {
                continue
            }
            val state = entry.value
            if (state.lastChunkX != Int.MIN_VALUE) {
                activityIndex.bump(Chunk(state.lastChunkX, state.lastChunkY), state.lastLevel)
            }
            iterator.remove()
        }
    }

    fun localActivityStamp(viewer: Client): Long {
        var stamp = 0L
        viewer.localNpcs.forEach { npc ->
            val state = subjectStates[npc] ?: return@forEach
            stamp = maxOf(stamp, state.movementRevision, state.blockRevision)
        }
        return stamp
    }

    fun viewerState(viewer: Client): ViewerNpcSyncState = viewerStates.computeIfAbsent(viewer) { ViewerNpcSyncState() }

    private class SubjectState {
        var movementRevision: Long = 0L
        var blockRevision: Long = 0L
        var lastX: Int = Int.MIN_VALUE
        var lastY: Int = Int.MIN_VALUE
        var lastChunkX: Int = Int.MIN_VALUE
        var lastChunkY: Int = Int.MIN_VALUE
        var lastLevel: Int = Int.MIN_VALUE
        var lastVisible: Boolean = true
        var lastSeenTick: Long = 0L
    }
}
