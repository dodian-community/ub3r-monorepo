package net.dodian.uber.economy.pricechecker

import net.dodian.uber.game.api.plugin.economy.EconomyPlugin

object PriceCheckerModule : EconomyPlugin {
    override val id = "economy.price-checker"
    override val name = "Price checker"
    override val routes = setOf("economy:price-checker:open", "economy:price-checker:items", "economy:price-checker:search")
}
