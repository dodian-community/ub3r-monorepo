package net.dodian.uber.game.engine.systems.net

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PacketEventEmissionTest {
    @Test
    fun `walking service emits walk event`() {
        assertContains(
            Paths.get("src/main/kotlin/net/dodian/uber/game/engine/systems/net/PacketWalkingService.kt"),
            Regex("""GameEventBus\.post\s*\(\s*WalkEvent\s*\("""),
        )
    }

    @Test
    fun `attack player service emits player attack event`() {
        assertContains(
            Paths.get("src/main/kotlin/net/dodian/uber/game/engine/systems/net/PacketInteractionService.kt"),
            Regex("""GameEventBus\.post\s*\(\s*PlayerAttackEvent\s*\("""),
        )
    }

    @Test
    fun `attack npc service emits player attack event`() {
        val source = normalizedSource(
            Paths.get("src/main/kotlin/net/dodian/uber/game/engine/systems/net/PacketInteractionService.kt"),
        )
        assertTrue(Regex("""GameEventBus\.post\s*\(\s*PlayerAttackEvent\s*\(\s*client\s*,\s*npcIndex\s*,\s*false\s*\)""").containsMatchIn(source))
    }

    @Test
    fun `use item on player service emits item on player event`() {
        val source = normalizedSource(
            Paths.get("src/main/kotlin/net/dodian/uber/game/engine/systems/net/PacketItemActionService.kt"),
        )
        assertTrue(
            Regex("""ContentEvents\.post\s*\(\s*ItemOnPlayerEvent\s*\(""").containsMatchIn(source) ||
                Regex("""GameEventBus\.post\s*\(\s*ItemOnPlayerEvent\s*\(""").containsMatchIn(source),
        )
    }

    @Test
    fun `public chat service emits chat message event`() {
        assertContains(
            Paths.get("src/main/kotlin/net/dodian/uber/game/engine/systems/net/PacketChatService.kt"),
            Regex("""GameEventBus\.post\s*\(\s*ChatMessageEvent\s*\("""),
        )
    }

    @Test
    fun `private message listener emits private message event and bounds payload`() {
        val source = normalizedSource(
            Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/SendPrivateMessageListener.java"),
        )
        assertTrue(Regex("""GameEventBus\.post\s*\(\s*new\s+PrivateMessageEvent\s*\(""").containsMatchIn(source))
        assertTrue(Regex("""remaining <= 0 \|\| remaining > MAX_MESSAGE_BYTES""").containsMatchIn(source))
    }

    private fun assertContains(path: Path, expected: Regex) {
        assertTrue(
            expected.containsMatchIn(normalizedSource(path)),
            "Expected $path to contain pattern `${expected.pattern}`",
        )
    }

    private fun normalizedSource(path: Path): String {
        val source = Files.readString(path)
        val withoutBlockComments = source.replace(Regex("""(?s)/\*.*?\*/"""), " ")
        val withoutLineComments = withoutBlockComments.replace(Regex("""(?m)//.*$"""), " ")
        return withoutLineComments.replace(Regex("""\s+"""), " ").trim()
    }
}
