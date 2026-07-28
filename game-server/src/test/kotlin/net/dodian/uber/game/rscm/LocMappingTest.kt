package net.dodian.uber.game.rscm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class LocMappingTest {
    private val mappingFile = Path.of("content/mappings/index/loc.rscm")

    @Test
    fun `loc mapping is deterministically generated from the pinned dump`() {
        val entries = Files.readAllLines(mappingFile)
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .associate { line ->
                val (key, id) = line.split("=", limit = 2)
                key to id.toInt()
            }

        assertEquals(50_047, entries.size)
        assertEquals(1, entries["crate"])
        assertEquals(10_076, entries["staff_of_bob_the_cat"])
        assertEquals(49_935, entries["clovers"])
        assertEquals(49_555, entries["entrails"])
        assertEquals(50_046, entries["snowman_50046"])
        assertEquals(11_366, entries["coal_rocks_11366"])
        val coalRecord = Files.readString(Path.of("content/mappings/metadata/loc.jsonl"))
            .lineSequence().first { it.contains("\"namespace\":\"loc\",\"id\":11366,") }
        assertTrue(coalRecord.contains("\"name\":\"Coal rocks\""))
        assertTrue(coalRecord.contains("\"op1\":\"Mine\""))
        val richRecord = Files.readString(Path.of("content/mappings/loc.rscm"))
            .substringAfter("// 11366\n").substringBefore("\n\n")
        assertTrue(richRecord.startsWith("// alias=coal_rocks_11366\n[loc_11366]\n"))
        assertTrue(richRecord.contains("\nop1=Mine\n"))
        assertTrue(entries.values.toSet().size == entries.size)
    }
}
