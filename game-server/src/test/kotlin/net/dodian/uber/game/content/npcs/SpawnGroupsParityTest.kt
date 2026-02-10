package net.dodian.uber.game.content.npcs

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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
        val hitpoints: Int?,
    )

    private data class PresetRow(
        val respawnTicks: Int,
        val attack: Int,
        val defence: Int,
        val strength: Int,
        val hitpoints: Int,
        val ranged: Int,
        val magic: Int,
    )

    @Test
    fun `generated grouped spawns match json seed exactly`() {
        val sourceRows = parseJsonSpawnRows(Path.of("scripts/npc_Spawn.json"))
        val sourcePresets = parseJsonNpcPresets(Path.of("scripts/npc_Def.json"))
        val generated = net.dodian.uber.game.content.npcs.spawns.SpawnGroups.all()

        assertEquals(sourceRows.size, generated.size, "Generated spawn count differs from JSON seed")

        sourceRows.zip(generated).forEachIndexed { index, (expected, actual) ->
            assertEquals(expected.npcId, actual.npcId, "npcId mismatch at row $index")
            assertEquals(expected.x, actual.x, "x mismatch at row $index")
            assertEquals(expected.y, actual.y, "y mismatch at row $index")
            assertEquals(expected.z, actual.z, "z mismatch at row $index")
            assertEquals(expected.face, actual.face, "face mismatch at row $index")
            assertEquals(expected.hitpoints ?: -1, actual.hitpoints, "hitpoints mismatch at row $index")

            val expectedPreset = sourcePresets[expected.npcId]
            if (expectedPreset == null) {
                assertNull(actual.preset, "Unexpected preset at row $index")
            } else {
                val preset = actual.preset
                assertNotNull(preset, "Missing preset at row $index")
                assertEquals(expectedPreset.respawnTicks, preset!!.respawnTicks, "respawn mismatch at row $index")
                assertEquals(expectedPreset.attack, preset.attack, "attack mismatch at row $index")
                assertEquals(expectedPreset.defence, preset.defence, "defence mismatch at row $index")
                assertEquals(expectedPreset.strength, preset.strength, "strength mismatch at row $index")
                assertEquals(expectedPreset.hitpoints, preset.hitpoints, "preset hitpoints mismatch at row $index")
                assertEquals(expectedPreset.ranged, preset.ranged, "ranged mismatch at row $index")
                assertEquals(expectedPreset.magic, preset.magic, "magic mismatch at row $index")
            }
        }

        assertTrue(generated.isNotEmpty(), "Generated spawns should not be empty")
    }

    private fun parseJsonNpcPresets(path: Path): Map<Int, PresetRow> {
        val root = mapper.readTree(path.toFile())
        val presets = mutableMapOf<Int, PresetRow>()

        for (node in root) {
            val npcId = node.intField("id") ?: continue
            presets[npcId] = PresetRow(
                hitpoints = node.intField("hitpoints") ?: 0,
                respawnTicks = node.intField("respawn") ?: 60,
                attack = node.intField("attack") ?: 0,
                strength = node.intField("strength") ?: 0,
                defence = node.intField("defence") ?: 0,
                ranged = node.intField("ranged") ?: 0,
                magic = node.intField("magic") ?: 0,
            )
        }

        return presets
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
            val hp = node.intField("hitpoints")

            rows += SpawnRow(
                npcId = npcId,
                x = x,
                y = y,
                z = z,
                face = face,
                hitpoints = hp,
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
