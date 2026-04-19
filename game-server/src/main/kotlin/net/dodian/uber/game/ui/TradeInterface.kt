package net.dodian.uber.game.ui

import net.dodian.uber.game.api.content.ContentInteraction
import net.dodian.uber.game.engine.systems.interaction.ui.TradeDuelSessionService
import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding
import org.slf4j.LoggerFactory

object TradeInterface : InterfaceButtonContent {
    private val logger = LoggerFactory.getLogger(TradeInterface::class.java)

    private const val CONFIRM_STAGE_ONE_BUTTON = 3420
    private const val CONFIRM_STAGE_TWO_BUTTON = 3546

    override val bindings =
        listOf(
            buttonBinding(-1, 0, "trade.confirm.stage_one", intArrayOf(CONFIRM_STAGE_ONE_BUTTON)) { client, _ ->
                try {
                    val other = client.getClient(client.trade_reqId)
                    if (other == null || !client.validClient(client.trade_reqId) || !client.inTrade ||
                        !ContentInteraction.tryAcquireMs(client, ContentInteraction.TRADE_CONFIRM_STAGE_ONE, 600L)
                    ) {
                        return@buttonBinding true
                    }
                    TradeDuelSessionService.confirmTradeStageOne(client, other)
                } catch (e: Exception) {
                    logger.warn("Trade button issue!", e)
                }
                true
            },
            buttonBinding(-1, 1, "trade.confirm.stage_two", intArrayOf(CONFIRM_STAGE_TWO_BUTTON)) { client, _ ->
                try {
                    val other = client.getClient(client.trade_reqId)
                    if (other == null || !client.validClient(client.trade_reqId) || !client.inTrade ||
                        !ContentInteraction.tryAcquireMs(client, ContentInteraction.TRADE_CONFIRM_STAGE_TWO, 600L)
                    ) {
                        return@buttonBinding true
                    }
                    TradeDuelSessionService.confirmTradeStageTwo(client, other)
                } catch (e: Exception) {
                    logger.warn("Trade button issue!", e)
                }
                true
            },
        )
}
