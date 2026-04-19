package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Path
import net.dodian.uber.game.objects.banking.BankingObjectIds
import net.dodian.uber.game.skill.thieving.ThievingObjectComponents
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BankingInteractionBoundaryTest {
    @Test
    fun `bank booth content enforces boundary-adjacent click policy`() {
        val source =
            Files.readString(
                Path.of("src/main/kotlin/net/dodian/uber/game/objects/banking/BankBoothObjectContent.kt"),
            )

        assertTrue(source.contains("override fun clickInteractionPolicy("))
        assertTrue(source.contains("ContentInteraction.nearestBoundaryCardinalPolicy()"))
    }

    @Test
    fun `bank chest content enforces boundary-adjacent click policy`() {
        val source =
            Files.readString(
                Path.of("src/main/kotlin/net/dodian/uber/game/objects/banking/BankChestObjectContent.kt"),
            )

        assertTrue(source.contains("override fun clickInteractionPolicy("))
        assertTrue(source.contains("ContentInteraction.nearestBoundaryCardinalPolicy()"))
    }

    @Test
    fun `object 20873 remains thieving-owned and excluded from banking booths`() {
        assertFalse(BankingObjectIds.boothObjects.contains(20873))
        assertTrue(ThievingObjectComponents.chestObjects.contains(20873))
    }

    @Test
    fun `object interaction processor logs early distance rejects for audit visibility`() {
        val source =
            Files.readString(
                Path.of("src/main/kotlin/net/dodian/uber/game/engine/systems/interaction/InteractionProcessor.kt"),
            )

        assertTrue(source.contains("InteractionProcessor.distance_reject"))
    }
}
