package net.dodian.uber.game.architecture

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension

class NettyListenerBoundaryTest {
    private val nettyListenerRoot: Path = Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in")
    private val forbiddenMethodPatterns = listOf(
        "CommandDispatcher.dispatch(",
        "client.send(",
        "client.reset",
        "client.face",
        "client.decline",
        "client.addFriend(",
        "client.removeFriend(",
        "client.addIgnore(",
        "client.removeIgnore(",
        "client.bank",
        "client.trade",
        "client.stake",
        "client.fromBank(",
        "client.fromTrade(",
        "client.fromDuel(",
        "client.addItem(",
        "client.deleteItem(",
        "client.dropItem(",
        "client.pickUpItem(",
        "client.wear(",
        "client.remove(",
        "client.checkItemUpdate(",
        "client.showNPCChat(",
        "client.tradeReq(",
        "client.close",
        "client.open",
        "client.clearBankStyleView(",
        "client.setLook(",
        "client.setChatText",
        "client.setWindowFocused(",
        "client.invalidate",
        "PlayerActionCancellationService.cancel(",
        "DialogueService.closeBlockingDialogue(",
    )
    private val forbiddenMutationRegex =
        Regex("""\bclient\.[A-Za-z_][A-Za-z0-9_]*\s*(=|\+=|-=|\*=|/=)""")

    @Test
    fun `netty inbound listeners only decode validate and delegate`() {
        val violations = Files.walk(nettyListenerRoot).use { paths ->
            paths.iterator().asSequence()
                .filter { Files.isRegularFile(it) }
                .filter { it.extension == "java" }
                .flatMap { file ->
                    Files.readAllLines(file)
                        .mapIndexedNotNull { lineNumber, line ->
                            val trimmed = line.trim()
                            if (trimmed.isEmpty() ||
                                trimmed.startsWith("//") ||
                                trimmed.startsWith("/*") ||
                                trimmed.startsWith("*") ||
                                trimmed.startsWith("package ") ||
                                trimmed.startsWith("import ")
                            ) {
                                return@mapIndexedNotNull null
                            }
                            if (forbiddenMethodPatterns.none { pattern -> trimmed.contains(pattern) } &&
                                !forbiddenMutationRegex.containsMatchIn(trimmed)
                            ) {
                                return@mapIndexedNotNull null
                            }
                            "${file}:${lineNumber + 1} -> $trimmed"
                        }
                        .asSequence()
                }
                .toList()
        }

        assertTrue(
            violations.isEmpty(),
            "Gameplay mutations still exist in netty listeners.\n${violations.joinToString("\n")}",
        )
    }
}
