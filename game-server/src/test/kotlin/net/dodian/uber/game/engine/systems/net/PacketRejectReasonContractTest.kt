package net.dodian.uber.game.engine.systems.net

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PacketRejectReasonContractTest {
    @Test
    fun `reject reasons use stable lowercase snake case wire names`() {
        val invalid = PacketRejectReason.values().filterNot { it.wire.matches(Regex("[a-z0-9_]+")) }
        assertTrue(invalid.isEmpty(), "Invalid reject reason wire keys: $invalid")
    }
}
