package net.dodian.uber.game.content.commands.dev

import net.dodian.uber.game.command.dev.ShopDevCommand as CanonicalShopDevCommand
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShopDevCommandTest {
    @Test
    fun `shop dev command prints plugin load diagnostics`() {
        val output = CanonicalShopDevCommand.renderSummaryForTests()
        assertTrue(output.contains("shops="))
        assertTrue(output.contains("duplicates=0"))
    }
}
