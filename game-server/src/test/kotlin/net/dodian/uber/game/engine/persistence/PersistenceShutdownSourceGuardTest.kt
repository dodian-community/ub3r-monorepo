package net.dodian.uber.game.persistence

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PersistenceShutdownSourceGuardTest {
    @Test
    fun `player save shutdown avoids sleep and runBlocking`() {
        val source = Files.readString(playerSaveServicePath())
        assertFalse(source.contains("Thread.sleep("))
        assertFalse(source.contains("runBlocking"))
        assertFalse(source.contains("catch (e: Exception)"))
        assertFalse(source.contains("catch(e: Exception)"))
    }

    @Test
    fun `player save and account shutdown paths enforce game thread blocking guard`() {
        val playerSaveSource = Files.readString(playerSaveServicePath())
        val accountSource = Files.readString(accountPersistencePath())
        assertFalse(playerSaveSource.contains("Thread.sleep("))
        assertFalse(accountSource.contains("Thread.sleep("))
        assertFalse(accountSource.contains("runBlocking"))
        assertFalse(accountSource.contains("catch (e: Exception)"))
        assertFalse(accountSource.contains("catch(e: Exception)"))
        assertTrue(playerSaveSource.contains("TickThreadBlockingGuard.requireNotGameThread(\"PlayerSaveService.shutdownAndDrain\")"))
        assertTrue(accountSource.contains("TickThreadBlockingGuard.requireNotGameThread(\"AccountPersistenceService.shutdownAndDrain\")"))
    }

    @Test
    fun `chat log worker avoids ad hoc thread sleep loops`() {
        val source = Files.readString(chatLogPath())
        assertFalse(source.contains("Thread.sleep("))
        assertFalse(source.contains("Thread(::processMessages"))
    }

    private fun playerSaveServicePath(): Path =
        Path.of("src/main/kotlin/net/dodian/uber/game/persistence/player/PlayerSaveService.kt")

    private fun chatLogPath(): Path =
        Path.of("src/main/kotlin/net/dodian/uber/game/persistence/audit/ChatLog.kt")

    private fun accountPersistencePath(): Path =
        Path.of("src/main/kotlin/net/dodian/uber/game/persistence/account/AccountPersistenceService.kt")
}
