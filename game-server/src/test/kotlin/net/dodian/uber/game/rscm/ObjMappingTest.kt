package net.dodian.uber.game.rscm

import net.dodian.uber.game.testutil.RequiresReferenceMappings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class ObjMappingTest {
    @Test
    fun `revision 218 item variants have readable verified names`() {
        RequiresReferenceMappings.assume()
        val entries = Files.readAllLines(Path.of("reference/cache-rev218/index/obj.rscm"))
            .associate { line -> line.substringBefore('=') to line.substringAfter('=').toInt() }
        assertEquals(12_764, entries["white_dark_bow_paint_noted"])
        assertEquals(entries.size, entries.values.toSet().size)

        val record = Files.readString(Path.of("reference/cache-rev218/metadata/obj.jsonl"))
            .lineSequence()
            .first { it.contains("\"namespace\":\"obj\",\"id\":12764,") }
        assertTrue(record.contains("\"displayName\":\"White dark bow paint (noted)\""))
        assertTrue(record.contains("\"nameSource\":\"linked_cert\""))
        assertTrue(record.contains("\"linkedSourceId\":12763"))
    }
}
