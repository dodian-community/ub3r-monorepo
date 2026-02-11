package net.dodian.uber.game.content.npcs

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

class SpawnGroupsParityTest {

    private val mapper = ObjectMapper()

    private data class SpawnRow(
        val npcId: Int,
        val x: Int,
        val y: Int,
        val z: Int,
        val face: Int,
    )

    @Test
    fun `generated grouped spawns match json seed coordinates and ordering`() {
        val sourceRows = parseJsonSpawnRows(Path.of("scripts/npc_Spawn.json"))
        val generated = net.dodian.uber.game.content.npcs.spawns.SpawnGroups.all()

        assertEquals(sourceRows.size, generated.size, "Generated spawn count differs from JSON seed")

        sourceRows.zip(generated).forEachIndexed { index, (expected, actual) ->
            assertEquals(expected.npcId, actual.npcId, "npcId mismatch at row $index")
            assertEquals(expected.x, actual.x, "x mismatch at row $index")
            assertEquals(expected.y, actual.y, "y mismatch at row $index")
            assertEquals(expected.z, actual.z, "z mismatch at row $index")
            assertEquals(expected.face, actual.face, "face mismatch at row $index")
            assertEquals(-1, actual.hitpoints, "spawn hitpoints override should be unset at row $index")
        }

        assertTrue(generated.isNotEmpty(), "Generated spawns should not be empty")
    }

    private fun parseJsonSpawnRows(path: Path): List<SpawnRow> {
        val root = mapper.readTree(path.toFile())
        val rows = mutableListOf<SpawnRow>()

        for (node in root) {
            val npcId = node.intField("id") ?: continue
            val x = node.intField("x") ?: continue
            val y = node.intField("y") ?: continue
            val z = node.intField("height") ?: 0
            val face = node.intField("face") ?: 0

            rows += SpawnRow(
                npcId = npcId,
                x = x,
                y = y,
                z = z,
                face = face,
            )
        }

        return rows
    }

    private fun JsonNode.intField(field: String): Int? {
        val node = get(field) ?: return null
        if (node.isNull) return null
        return when {
            node.isInt || node.isLong || node.isShort -> node.asInt()
            node.isTextual -> node.asText().trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
            else -> null
        }
    }
}
