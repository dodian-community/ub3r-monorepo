package net.dodian.uber.economy.shops

import net.dodian.uber.game.api.plugin.economy.EconomyPlugin

object ShopsModule : EconomyPlugin {
    override val id = "economy.shops"
    override val name = "Shops"
    override val routes = setOf("economy:shop:buy", "economy:shop:sell", "economy:shop:inspect")
}
