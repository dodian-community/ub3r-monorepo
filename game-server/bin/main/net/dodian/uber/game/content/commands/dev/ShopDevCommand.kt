package net.dodian.uber.game.content.commands.dev

import net.dodian.uber.game.content.shop.plugin.ShopPluginRegistry
import net.dodian.uber.game.systems.interaction.commands.CommandContent
import net.dodian.uber.game.systems.interaction.commands.commands

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
        val shops = ShopPluginRegistry.all()
        val ids = shops.map { it.id }.sorted()
        val duplicates = ids.size - ids.toSet().size
        return "shops=${shops.size} ids=$ids duplicates=$duplicates"
    }
}
