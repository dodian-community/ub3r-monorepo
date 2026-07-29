package net.dodian.uber.game.ui

import net.dodian.uber.game.api.content.ContentInteraction
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.engine.systems.interaction.ui.TradeDuelSessionService
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.social.exchange.DuelingService
import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding

object DuelInterface : InterfaceButtonContent {
    private val offerRuleButtons = intArrayOf(31034, 31035, 31036, 31037, 31038, 31039, 31041, 31042, 31040, 31043, 31044)
    private val offerRuleIndexByButton = mapOf(
        31034 to 0, 31035 to 1, 31036 to 2, 31037 to 3, 31038 to 4,
        31039 to 5, 31041 to 6, 31042 to 7, 31040 to 8, 31043 to 9, 31044 to 10,
    )
    private val noopRuleIndices = setOf(3, 4, 5, 9, 10)
    private val bodyRuleButtons = intArrayOf(13813, 13814, 13815, 13816, 13817, 13818, 13819, 13820, 13821, 13822, 13823)
    private const val CONFIRM_STAGE_TWO_BUTTON = 31520
    private const val CONFIRM_STAGE_ONE_BUTTON = 31015

    override val bindings =
        listOf(
            buttonBinding(-1, 0, "duel.offer_rule", offerRuleButtons) { client, request ->
                val ruleIndex = offerRuleIndexByButton[request.rawButtonId] ?: return@buttonBinding false
                if (ruleIndex in noopRuleIndices) return@buttonBinding true
                DuelingService.toggleRule(client, ruleIndex)
            },
            buttonBinding(-1, 0, "duel.decline_stage_one", intArrayOf(31018, 31002)) { client, _ ->
                if (!client.inDuel || client.duelFight) return@buttonBinding true
                DuelingService.decline(client)
                true
            },
            buttonBinding(-1, 0, "duel.decline_stage_two", intArrayOf(31523, 31502)) { client, _ ->
                if (!client.inDuel || client.duelFight) return@buttonBinding true
                DuelingService.decline(client)
                true
            },
            buttonBinding(-1, 2, "duel.body_rule", bodyRuleButtons) { client, request ->
                val ruleIndex = bodyRuleButtons.indexOf(request.rawButtonId)
                DuelingService.toggleBodyRule(client, ruleIndex)
            },
            buttonBinding(-1, 3, "duel.confirm.stage_two", intArrayOf(CONFIRM_STAGE_TWO_BUTTON)) { client, _ ->
                if (!ContentInteraction.tryAcquireMs(client, ContentInteraction.DUEL_CONFIRM_STAGE_TWO, 1000L)) {
                    return@buttonBinding true
                }
                val other = client.getClient(client.duel_with)
                if (other == null || client.slot == other.slot || !client.inDuel || client.duelConfirmed2) {
                    return@buttonBinding true
                }
                if (!client.validClient(client.duel_with)) {
                    TradeDuelSessionService.closeOpenDuel(client)
                    return@buttonBinding true
                }
                TradeDuelSessionService.confirmDuelStageTwo(client, other)
                true
            },
            buttonBinding(-1, 4, "duel.confirm.stage_one", intArrayOf(CONFIRM_STAGE_ONE_BUTTON)) { client, _ ->
                val other = client.getClient(client.duel_with)
                if (other == null || client.slot == other.slot || !client.inDuel || client.duelConfirmed) {
                    return@buttonBinding true
                }
                val nowCycle = GameCycleClock.currentCycle()
                if (nowCycle - client.lastDuelItemChangeCycle < 4 || nowCycle - other.lastDuelItemChangeCycle < 4) {
                    client.sendMessage("Please wait a moment after changing the duel stakes.")
                    return@buttonBinding true
                }
                val sendMsgToOther = client.maxHealth - client.currentHealth == 0 && other.maxHealth - other.currentHealth != 0
                if (other.maxHealth - other.currentHealth != 0 || client.maxHealth - client.currentHealth != 0) {
                    client.sendMessage(if (sendMsgToOther) "Your opponent is low on health!" else "You are low on health, so please heal up!")
                    if (sendMsgToOther) {
                        other.sendMessage("You are low on health, so please heal up!")
                    }
                    return@buttonBinding true
                }
                if (!ContentInteraction.tryAcquireMs(client, ContentInteraction.DUEL_CONFIRM_STAGE_ONE, 1000L)) {
                    return@buttonBinding true
                }
                if (!client.validClient(client.duel_with)) {
                    TradeDuelSessionService.closeOpenDuel(client)
                    return@buttonBinding true
                }
                TradeDuelSessionService.confirmDuelStageOne(client, other)
                true
            },
            buttonBinding(-1, 0, "duel.close_victory", intArrayOf(31710)) { client, _ ->
                client.send(RemoveInterfaces())
                true
            },
        )
}
