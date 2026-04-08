package net.dodian.uber.game.content.commands.dev

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShopDevCommandTest {
    @Test
    fun `shop dev command prints plugin load diagnostics`() {
        val output = ShopDevCommand.renderSummaryForTests()
        assertTrue(output.contains("shops="))
        assertTrue(output.contains("duplicates=0"))
    }
}
