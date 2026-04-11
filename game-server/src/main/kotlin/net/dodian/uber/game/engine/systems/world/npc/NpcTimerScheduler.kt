package net.dodian.uber.game.engine.systems.world.npc

import java.util.IdentityHashMap
import java.util.PriorityQueue
import net.dodian.uber.game.model.entity.npc.Npc

/**
 * Keeps important NPC timers progressing without scanning all NPCs every tick.
 *
 * This is intentionally minimal: it covers boosted-stat decay and the
 * death-floor/respawn lifecycle so that offscreen NPCs still progress on time.
 */
object NpcTimerScheduler {
    private enum class Kind {
        BOOSTED_STAT,
        DEATH_FLOOR,
        RESPAWN_CHECK,
    }

    private data class Entry(
        val dueAtMs: Long,
        val id: Long,
        val kind: Kind,
        val npc: Npc,
        val version: Int,
    )

    private class Versions {
        var statVersion: Int = 0
        var deathVersion: Int = 0
        var respawnVersion: Int = 0
    }

    private val versions = IdentityHashMap<Npc, Versions>()
    private val queue =
        PriorityQueue<Entry>(compareBy<Entry> { it.dueAtMs }.thenBy { it.id })
    private var nextId = 0L

    @JvmStatic
    fun initialize(npcs: Array<Npc?>) {
        val now = System.currentTimeMillis()
        for (npc in npcs) {
            if (npc != null) {
                scheduleStatTick(npc, now)
            }
        }
    }

    @JvmStatic
    fun initialize(npcs: Collection<Npc>) {
        val now = System.currentTimeMillis()
        for (npc in npcs) {
            scheduleStatTick(npc, now)
        }
    }

    @JvmStatic
    fun onNpcDied(npc: Npc) {
        val now = System.currentTimeMillis()
        val state = versions.computeIfAbsent(npc) { Versions() }
        state.deathVersion++
        val floorDueAt = npc.deathTime + npc.timeOnFloor.toLong()
        queue += Entry(floorDueAt.coerceAtLeast(now + 1L), ++nextId, Kind.DEATH_FLOOR, npc, state.deathVersion)
    }

    @JvmStatic
    fun runDue(nowMs: Long) {
        while (true) {
            val next = queue.peek() ?: break
            if (next.dueAtMs > nowMs) {
                break
            }
            queue.poll()
            val state = versions[next.npc] ?: continue
            when (next.kind) {
                Kind.BOOSTED_STAT -> {
                    if (next.version != state.statVersion) continue
                    // Legacy semantics: decay runs every ~30s per npc regardless of visibility.
                    next.npc.changeStat()
                    scheduleStatTick(next.npc, nowMs)
                }

                Kind.DEATH_FLOOR -> {
                    if (next.version != state.deathVersion) continue
                    val npc = next.npc
                    if (!npc.alive && npc.isVisible && nowMs - npc.deathTime >= npc.timeOnFloor.toLong()) {
                        NpcLifecycle.processDeathFloor(npc)
                        scheduleRespawnCheck(npc, state, nowMs)
                    }
                }

                Kind.RESPAWN_CHECK -> {
                    if (next.version != state.respawnVersion) continue
                    val npc = next.npc
                    if (npc.alive || npc.isVisible) {
                        continue
                    }
                    val targetAt =
                        npc.deathTime + npc.timeOnFloor.toLong() + (npc.respawn.toLong() * 1000L)
                    if (nowMs >= targetAt) {
                        npc.respawn()
                        npc.syncChunkMembership()
                        scheduleStatTick(npc, nowMs)
                    } else {
                        // Recheck periodically to respect dynamic respawn adjustments.
                        val nextDue = minOf(targetAt, nowMs + 1000L)
                        queue += Entry(nextDue, ++nextId, Kind.RESPAWN_CHECK, npc, next.version)
                    }
                }
            }
        }
    }

    private fun scheduleRespawnCheck(npc: Npc, state: Versions, nowMs: Long) {
        state.respawnVersion++
        queue += Entry(nowMs + 1000L, ++nextId, Kind.RESPAWN_CHECK, npc, state.respawnVersion)
    }

    private fun scheduleStatTick(npc: Npc, nowMs: Long) {
        val state = versions.computeIfAbsent(npc) { Versions() }
        state.statVersion++
        queue += Entry(nowMs + 30_000L, ++nextId, Kind.BOOSTED_STAT, npc, state.statVersion)
    }
}
