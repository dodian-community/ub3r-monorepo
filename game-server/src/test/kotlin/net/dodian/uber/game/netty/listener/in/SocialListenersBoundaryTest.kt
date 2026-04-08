package net.dodian.uber.game.netty.listener.`in`

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class SocialListenersBoundaryTest {
    @Test
    fun `social listeners delegate gameplay mutations to kotlin service`() {
        val listenerAssertions = mapOf(
            "CommandsListener.java" to listOf("CommandDispatcher.dispatch("),
            "AddFriendListener.java" to listOf("client.addFriend("),
            "RemoveFriendListener.java" to listOf("client.removeFriend("),
            "AddIgnoreListener.java" to listOf("client.addIgnore("),
            "RemoveIgnoreListener.java" to listOf("client.removeIgnore("),
        )

        listenerAssertions.forEach { (fileName, forbiddenPatterns) ->
            val source = Files.readString(
                Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/$fileName"),
            )
            forbiddenPatterns.forEach { pattern ->
                assertFalse(source.contains(pattern), "$fileName still contains $pattern")
            }
        }
    }
}

