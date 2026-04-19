@file:Suppress("unused")

package net.dodian.uber.game.command.dev

import net.dodian.uber.game.engine.systems.interaction.commands.CommandContent
import net.dodian.uber.game.engine.systems.interaction.commands.commands
import net.dodian.uber.game.shop.ShopCatalogRegistry

object ShopDevCommand : CommandContent {
    override fun definitions() =
        commands {
            command("shopdiag", "shopdev") {
                if (!specialRights) {
                    return@command false
                }
                reply(renderSummary())
                true
            }
        }

    internal fun renderSummaryForTests(): String = renderSummary()

    private fun renderSummary(): String {
        val shops = ShopCatalogRegistry.all()
        val ids = shops.map { it.id }.sorted()
        val duplicates = ids.size - ids.toSet().size
        return "shops=${shops.size} ids=$ids duplicates=$duplicates"
    }
}
