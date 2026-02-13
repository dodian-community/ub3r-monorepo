package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.content.npcs.spawns.NpcSpawnDef
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NpcContentRegistryTest {

    @BeforeEach
    fun setUp() {
        NpcContentRegistry.clearForTests()
    }

    @AfterEach
    fun tearDown() {
        NpcContentRegistry.resetForTests()
    }

    @Test
    fun `duplicate npcId registration throws`() {
        val first = functionContent(
            npcIds = intArrayOf(10),
            spawns = listOf(NpcSpawnDef(npcId = 10, x = 3200, y = 3200)),
        )
        val duplicate = functionContent(
            npcIds = intArrayOf(10),
            spawns = listOf(NpcSpawnDef(npcId = 10, x = 3201, y = 3201)),
        )

        NpcContentRegistry.register(first)
        assertThrows(IllegalArgumentException::class.java) {
            NpcContentRegistry.register(duplicate)
        }
    }

    @Test
    fun `allSpawns flattens registered function spawns`() {
        val spawn1 = NpcSpawnDef(npcId = 1, x = 3000, y = 3001)
        val spawn2 = NpcSpawnDef(npcId = 1, x = 3002, y = 3003)
        val spawn3 = NpcSpawnDef(npcId = 2, x = 3004, y = 3005)

        NpcContentRegistry.register(functionContent(intArrayOf(1), listOf(spawn1, spawn2)))
        NpcContentRegistry.register(functionContent(intArrayOf(2), listOf(spawn3)))

        assertEquals(listOf(spawn1, spawn2, spawn3), NpcContentRegistry.allSpawns())
    }

    @Test
    fun `spawn source ids include only owning functions`() {
        NpcContentRegistry.register(functionContent(intArrayOf(10), emptyList(), ownsSpawnDefinitions = false))
        NpcContentRegistry.register(functionContent(intArrayOf(20, 21), emptyList(), ownsSpawnDefinitions = true))

        assertEquals(setOf(20, 21), NpcContentRegistry.spawnSourceNpcIds())
        assertTrue(10 !in NpcContentRegistry.spawnSourceNpcIds())
    }

    private fun functionContent(
        npcIds: IntArray,
        spawns: List<NpcSpawnDef>,
        ownsSpawnDefinitions: Boolean = false,
    ): NpcContentDefinition {
        return NpcContentDefinition(
            name = "TestContent",
            npcIds = npcIds,
            ownsSpawnDefinitions = ownsSpawnDefinitions,
            entries = spawns,
        )
    }
}
