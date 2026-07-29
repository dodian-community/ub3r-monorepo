package net.dodian.uber.social.dueling

import net.dodian.uber.game.api.plugin.social.ExchangeKind
import net.dodian.uber.game.api.plugin.social.ExchangeRoute
import net.dodian.uber.game.api.plugin.social.ExchangeRouteType
import net.dodian.uber.game.api.plugin.social.SocialPlugin
import net.dodian.uber.game.api.plugin.social.SocialPluginDefinition

object DuelingModule : SocialPlugin {
    override val definition = SocialPluginDefinition(
        id = "social.dueling",
        name = "Dueling",
        kind = ExchangeKind.DUEL,
        routes = setOf(
            ExchangeRoute(ExchangeRouteType.PLAYER_OPTION, "duel"),
            ExchangeRoute(ExchangeRouteType.CHAT_REQUEST, "duel"),
            ExchangeRoute(ExchangeRouteType.ITEM_OFFER, "duel:3322"),
            ExchangeRoute(ExchangeRouteType.ITEM_WITHDRAW, "6669"),
            ExchangeRoute(ExchangeRouteType.BUTTON, "31015-31523"),
            ExchangeRoute(ExchangeRouteType.LIFECYCLE, "close-move-logout-death"),
        ),
    )
}
