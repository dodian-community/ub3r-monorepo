package net.dodian.uber.game.rscm

import kotlin.io.path.exists
import kotlin.io.path.readText
import java.nio.file.Path
import net.dodian.uber.game.testutil.RequiresReferenceMappings
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReferenceMappingsRuntimeIsolationTest {
    @Test
    fun `reference mappings have no runtime resolver or server startup load`() {
        RequiresReferenceMappings.assume()
        val resolver = Path.of("src/main/kotlin/net/dodian/uber/game/rscm/RSCM.kt")
        assertFalse(resolver.exists(), "RSCM must remain a generated reference format, not a runtime resolver")

        val server = Path.of("src/main/java/net/dodian/uber/game/Server.java").readText()
        assertFalse(server.contains("RSCM.load"))
        val source = Path.of("src/main/kotlin").toFile().walkTopDown()
            .filter { it.extension == "kt" && !it.path.endsWith("RscmGenerator.kt") }
            .joinToString("\n") { it.readText() }
        assertFalse(source.contains("asRscm"))
        assertFalse(source.contains("RSCM.get("))
        assertTrue(Path.of("reference/cache-rev218/README.md").readText().contains("not loaded by the server"))
    }
}
