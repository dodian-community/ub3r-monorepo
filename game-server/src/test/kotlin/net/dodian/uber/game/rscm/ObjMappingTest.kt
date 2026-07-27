package net.dodian.uber.game.rscm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class ObjMappingTest {
    @Test
    fun `revision 218 item variants have readable verified names`() {
        val entries = Files.readAllLines(Path.of("content/mappings/obj.rscm"))
            .associate { line -> line.substringBefore('=') to line.substringAfter('=').toInt() }
        assertEquals(12_764, entries["white_dark_bow_paint_noted"])
        assertEquals(entries.size, entries.values.toSet().size)

        val record = Files.readString(Path.of("content/mappings/metadata/obj.jsonl"))
            .lineSequence()
            .first { it.contains("\"id\":12764") }
        assertTrue(record.contains("\"displayName\":\"White dark bow paint (noted)\""))
        assertTrue(record.contains("\"nameSource\":\"linked_cert\""))
        assertTrue(record.contains("\"linkedSourceId\":12763"))
    }
}
