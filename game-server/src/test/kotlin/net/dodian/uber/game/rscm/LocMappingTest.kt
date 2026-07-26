package net.dodian.uber.game.rscm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class LocMappingTest {
    private val mappingFile = Path.of("content/mappings/loc.rscm")

    @Test
    fun `loc mapping preserves cache ids and applies Alter names`() {
        val entries = Files.readAllLines(mappingFile)
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .associate { line ->
                val (key, id) = line.split("=", limit = 2)
                key to id.toInt()
            }

        assertEquals(50_047, entries.size)
        assertEquals(1, entries["crate_1"])
        assertEquals(10_076, entries["staff_of_bob_the_cat_10076"])
        assertEquals(47_481, entries["clovers"])
        assertEquals(49_935, entries["clovers_49935"])
        assertEquals(47_578, entries["entrails"])
        assertEquals(49_555, entries["entrails_49555"])
        assertEquals(50_046, entries["snowman_50046"])
        assertTrue(entries.values.toSet().size == entries.size)
    }
}
