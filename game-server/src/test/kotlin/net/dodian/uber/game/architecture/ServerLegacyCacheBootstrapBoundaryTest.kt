package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class ServerLegacyCacheBootstrapBoundaryTest {
    @Test
    fun `server bootstrap no longer calls removed cache loaders`() {
        val serverPath = Paths.get("src/main/java/net/dodian/uber/game/Server.java")
        val source = Files.readString(serverPath)
        assertFalse(source.contains("Cache.load()"))
        assertFalse(source.contains("ObjectDef.loadConfig()"))
        assertFalse(source.contains("new ObjectLoader()"))
    }
}
