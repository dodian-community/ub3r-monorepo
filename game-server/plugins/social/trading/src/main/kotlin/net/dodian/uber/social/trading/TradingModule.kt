package net.dodian.uber.social.trading

import net.dodian.uber.game.api.plugin.social.ExchangeKind
import net.dodian.uber.game.api.plugin.social.ExchangeRoute
import net.dodian.uber.game.api.plugin.social.ExchangeRouteType
import net.dodian.uber.game.api.plugin.social.SocialPlugin
import net.dodian.uber.game.api.plugin.social.SocialPluginDefinition

object TradingModule : SocialPlugin {
    override val definition = SocialPluginDefinition(
        id = "social.trading",
        name = "Trading",
        kind = ExchangeKind.TRADE,
        routes = setOf(
            ExchangeRoute(ExchangeRouteType.PLAYER_OPTION, "trade"),
            ExchangeRoute(ExchangeRouteType.CHAT_REQUEST, "trade"),
            ExchangeRoute(ExchangeRouteType.ITEM_OFFER, "trade:3322"),
            ExchangeRoute(ExchangeRouteType.ITEM_WITHDRAW, "3415"),
            ExchangeRoute(ExchangeRouteType.BUTTON, "3420"),
            ExchangeRoute(ExchangeRouteType.BUTTON, "3546"),
            ExchangeRoute(ExchangeRouteType.LIFECYCLE, "close-move-logout"),
        ),
    )
}
