package net.dodian.uber.game.engine.config

import java.nio.file.Files
import java.nio.file.Path
import net.dodian.uber.game.testutil.RequiresReferenceMappings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ServerPathsTest {
    @Test
    fun `definitions and references resolve identically from repository and server roots`() {
        RequiresReferenceMappings.assume()
        val original = System.getProperty("user.dir")
        val initial = Path.of(original).toAbsolutePath().normalize()
        val serverRoot = if (initial.fileName.toString() == "game-server") initial else initial.resolve("game-server")
        val repositoryRoot = serverRoot.parent
        try {
            System.setProperty("user.dir", repositoryRoot.toString())
            val fromRepository = ServerPaths.definition("combat", "projectiles.toml")
            val referenceFromRepository = ServerPaths.revision218Reference("index", "loc.rscm")

            System.setProperty("user.dir", serverRoot.toString())
            assertEquals(fromRepository, ServerPaths.definition("combat", "projectiles.toml"))
            assertEquals(referenceFromRepository, ServerPaths.revision218Reference("index", "loc.rscm"))
            assertTrue(Files.isRegularFile(fromRepository))
            assertTrue(Files.isRegularFile(referenceFromRepository))
        } finally {
            System.setProperty("user.dir", original)
        }
    }
}
