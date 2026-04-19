package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AgilityPassageOverlayBoundaryTest {
    @Test
    fun `agility movement calls are routed through passage overlay helper`() {
        val agilityFiles =
            listOf(
                Path.of("src/main/kotlin/net/dodian/uber/game/skill/agility/Agility.kt"),
                Path.of("src/main/kotlin/net/dodian/uber/game/skill/agility/AgilityTravel.kt"),
                Path.of("src/main/kotlin/net/dodian/uber/game/skill/agility/AgilityWerewolf.kt"),
            )

        agilityFiles.forEach { file ->
            val source = Files.readString(file)
            val directCalls = Regex("""\bc\.AddToWalkCords\(""").findAll(source).count()
            assertEquals(
                1,
                directCalls,
                "$file should only call c.AddToWalkCords(...) inside queueAgilityWalk helper.",
            )
        }
    }
}
