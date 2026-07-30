package net.dodian.uber.game.rscm

import net.dodian.uber.game.testutil.RequiresReferenceMappings
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class RscmMetadataTest {
    @BeforeEach
    fun requireReferenceMappings() = RequiresReferenceMappings.assume()

    @Test
    fun `npc metadata exposes right click options`() {
        val record = Files.readString(Path.of("reference/cache-rev218/metadata/npc.jsonl"))
            .lineSequence()
            .first { it.contains("\"namespace\":\"npc\",\"id\":7402,") }
        assertTrue(record.contains("\"options\":{\"op2\":\"Attack\"}"))
        assertTrue(record.contains("\"walkanim\":\"seq_6372\""))
    }

    @Test
    fun `component metadata exposes its dumped definition`() {
        val record = Files.readString(Path.of("reference/cache-rev218/metadata/component.jsonl"))
            .lineSequence()
            .first { it.contains("\"namespace\":\"component\",\"id\":4,") }
        assertTrue(record.contains("\"type\":\"model\""))
        assertTrue(record.contains("\"model\":\"model_13369\""))
        assertTrue(record.contains("\"sourceFile\":\"interface/interface_0.if3\""))
    }
}
