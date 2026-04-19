package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NonBlockingRuntimeBoundaryTest {
    private val runtimeSensitiveRoots =
        listOf(
            Path.of("src/main/java/net/dodian/uber/game/netty"),
            Path.of("src/main/kotlin/net/dodian/uber/game/engine"),
            Path.of("src/main/kotlin/net/dodian/uber/game/skill"),
            Path.of("src/main/java/net/dodian/uber/game/model/entity/player/Client.java"),
        )

    @Test
    fun `runtime sensitive paths avoid direct jdbc and blocking sleep`() {
        val forbidden = listOf("prepareStatement(", "executeQuery(", "executeUpdate(", "dbConnection", "Thread.sleep(")

        val violations = mutableListOf<String>()
        runtimeSensitiveRoots.filter(Files::exists).forEach { root ->
            val files =
                if (Files.isDirectory(root)) {
                    val collected = mutableListOf<Path>()
                    Files.walk(root).use { walk ->
                        walk
                            .filter { Files.isRegularFile(it) && (it.toString().endsWith(".kt") || it.toString().endsWith(".java")) }
                            .forEach { collected.add(it) }
                    }
                    collected
                } else {
                    listOf(root)
                }

            files.forEach { file ->
                Files.readAllLines(file).forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                        return@forEachIndexed
                    }
                    forbidden.forEach { token ->
                        if (trimmed.contains(token)) {
                            violations += "$file:${index + 1} contains forbidden runtime token `$token`"
                        }
                    }
                }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Runtime-sensitive code must not block or run direct JDBC.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `synchronous persistence entry points enforce not game thread guard`() {
        val playerSaveSource =
            Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/persistence/player/PlayerSaveService.kt"))
        val accountSource =
            Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/persistence/account/AccountPersistenceService.kt"))

        assertTrue(playerSaveSource.contains("TickThreadBlockingGuard.requireNotGameThread(\"PlayerSaveService.saveSynchronously\")"))
        assertTrue(playerSaveSource.contains("TickThreadBlockingGuard.requireNotGameThread(\"PlayerSaveService.shutdownAndDrain\")"))
        assertTrue(accountSource.contains("TickThreadBlockingGuard.requireNotGameThread(\"AccountPersistenceService.saveSynchronously\")"))
        assertTrue(accountSource.contains("TickThreadBlockingGuard.requireNotGameThread(\"AccountPersistenceService.shutdownAndDrain\")"))
    }
}
