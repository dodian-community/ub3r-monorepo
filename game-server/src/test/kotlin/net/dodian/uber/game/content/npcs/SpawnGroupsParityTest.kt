package net.dodian.uber.game.content.npcs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class SpawnGroupsParityTest {

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
    fun `generated grouped spawns match sql seed exactly`() {
        val sqlRows = parseSqlSpawnRows(Path.of("database/2_dodian_default_data.sql"))
        val sqlPresets = parseSqlNpcPresets(Path.of("database/2_dodian_default_data.sql"))
        val generated = net.dodian.uber.game.content.npcs.spawns.SpawnGroups.all()

        assertEquals(sqlRows.size, generated.size, "Generated spawn count differs from SQL seed")

        sqlRows.zip(generated).forEachIndexed { index, (expected, actual) ->
            assertEquals(expected.npcId, actual.npcId, "npcId mismatch at row $index")
            assertEquals(expected.x, actual.x, "x mismatch at row $index")
            assertEquals(expected.y, actual.y, "y mismatch at row $index")
            assertEquals(expected.z, actual.z, "z mismatch at row $index")
            assertEquals(expected.face, actual.face, "face mismatch at row $index")
            assertEquals(expected.hitpoints ?: -1, actual.hitpoints, "hitpoints mismatch at row $index")

            val expectedPreset = sqlPresets[expected.npcId]
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

    private fun parseSqlNpcPresets(path: Path): Map<Int, PresetRow> {
        val lines = Files.readAllLines(path)
        val presets = mutableMapOf<Int, PresetRow>()
        var inBlock = false

        val rowPattern = Regex(
            """\(\s*(\d+)\s*,\s*'([^']*)'\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*\)"""
        )

        for (raw in lines) {
            val line = raw.trim()
            if (!inBlock && line.startsWith("INSERT IGNORE INTO uber3_npcs")) {
                inBlock = true
                continue
            }
            if (!inBlock) {
                continue
            }
            if (line.isEmpty()) {
                continue
            }
            if (!line.startsWith("(") && !line.startsWith(",")) {
                continue
            }

            val row = line.removePrefix(",").trim().removeSuffix(";").trim()
            val match = rowPattern.find(row) ?: continue

            val npcId = match.groupValues[1].toInt()
            presets[npcId] = PresetRow(
                hitpoints = match.groupValues[6].toInt(),
                respawnTicks = match.groupValues[7].toInt(),
                attack = match.groupValues[9].toInt(),
                strength = match.groupValues[10].toInt(),
                defence = match.groupValues[11].toInt(),
                ranged = match.groupValues[12].toInt(),
                magic = match.groupValues[13].toInt(),
            )

            if (line.endsWith(";")) {
                break
            }
        }

        return presets
    }

    private fun parseSqlSpawnRows(path: Path): List<SpawnRow> {
        val lines = Files.readAllLines(path)
        val rows = mutableListOf<SpawnRow>()
        var inBlock = false

        for (raw in lines) {
            val line = raw.trim()
            if (!inBlock && line.startsWith("INSERT IGNORE INTO uber3_spawn")) {
                inBlock = true
                continue
            }
            if (!inBlock) {
                continue
            }
            if (line.isEmpty()) {
                continue
            }
            if (!line.startsWith("(") && !line.startsWith(",")) {
                continue
            }

            var row = line
                .removePrefix(",")
                .trim()
                .removeSuffix(";")
                .trim()

            if (!row.startsWith("(") || !row.endsWith(")")) {
                if (line.endsWith(";")) {
                    break
                }
                continue
            }

            row = row.substring(1, row.length - 1).replace("'", "")
            val parts = row.split(",").map { it.trim() }
            if (parts.size < 12) {
                continue
            }

            val hp = parts[9].let { if (it.equals("null", ignoreCase = true)) null else it.toInt() }

            rows += SpawnRow(
                npcId = parts[0].toInt(),
                x = parts[1].toInt(),
                y = parts[2].toInt(),
                z = parts[3].toInt(),
                face = parts[11].toInt(),
                hitpoints = hp,
            )

            if (line.endsWith(";")) {
                break
            }
        }

        return rows
    }
}
