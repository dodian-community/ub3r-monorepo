package net.dodian.uber.game.integration.skills.support

import net.dodian.uber.game.engine.loop.GameThreadContext
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.objects.WorldObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag

/**
 * Base class for real-world (real cache/game-loop) skill integration tests. See
 * [RealWorldHarness] for what gets booted. Requires game-server/data/cache locally;
 * skips gracefully (via [RealWorldHarness.assumeCacheAvailable]) when it's absent, e.g.
 * in CI.
 */
@Tag("real-world")
abstract class RealWorldSkillTest {
    private val spawnedPlayers = mutableListOf<Pair<Client, Int>>()
    private val spawnedObjects = mutableListOf<WorldObject>()
    private val spawnedNpcs = mutableListOf<Npc>()

    @BeforeEach
    fun bootRealWorld() {
        RealWorldHarness.assumeCacheAvailable()
        RealWorldHarness.ensureWorldBooted()
        GameThreadContext.bindCurrentThread()
    }

    protected fun spawnPlayer(name: String, x: Int, y: Int, z: Int = 0): Client {
        val client = RealWorldHarness.createPlayer(name, x, y, z)
        spawnedPlayers += client to client.slot
        return client
    }

    /** [GlobalObject][net.dodian.uber.game.model.objects.GlobalObject] is a JVM-wide
     * singleton - objects spawned through this helper are automatically removed in
     * [cleanupRealWorld] so they don't leak into other tests running later in the same
     * Gradle test-worker fork. */
    protected fun spawnObject(objectId: Int, x: Int, y: Int, z: Int = 0, durationMs: Int = 60_000): WorldObject {
        val worldObject = RealWorldHarness.spawnObject(objectId, x, y, z, durationMs)
        spawnedObjects += worldObject
        return worldObject
    }

    /** Server.npcManager.npcMap is a JVM-wide singleton, like GlobalObject - npcs
     * spawned through this helper are automatically removed in [cleanupRealWorld]. */
    protected fun spawnNpc(id: Int, x: Int, y: Int, z: Int = 0, face: Int = 0): Npc {
        val npc = RealWorldHarness.spawnNpc(id, x, y, z, face)
        spawnedNpcs += npc
        return npc
    }

    @AfterEach
    fun cleanupRealWorld() {
        spawnedNpcs.forEach { RealWorldHarness.despawnNpc(it) }
        spawnedNpcs.clear()
        spawnedObjects.forEach { RealWorldHarness.despawnObject(it) }
        spawnedObjects.clear()
        spawnedPlayers.forEach { (client, slot) -> RealWorldHarness.despawnPlayer(client, slot) }
        spawnedPlayers.clear()
        GameThreadContext.clearBindingForTests()
        GameThreadContext.resetDiagnosticsForTests()
    }
}
