package net.dodian.uber.game.ui

import net.dodian.uber.game.api.content.ContentInteraction
import net.dodian.uber.game.engine.systems.interaction.ui.TradeDuelSessionService
import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding

object DuelInterface : InterfaceButtonContent {
    private val offerRuleButtons = intArrayOf(6698, 6699, 6697, 6702)
    private val offerRuleIndexByButton = mapOf(6698 to 0, 6699 to 1, 6697 to 2, 6702 to 7)
    private val bodyRuleButtons = intArrayOf(13813, 13814, 13815, 13816, 13817, 13818, 13819, 13820, 13821, 13822, 13823)
    private const val CONFIRM_STAGE_TWO_BUTTON = 6520
    private const val CONFIRM_STAGE_ONE_BUTTON = 6674

    override val bindings =
        listOf(
            buttonBinding(-1, 0, "duel.offer_rule", offerRuleButtons) { client, request ->
                val ruleIndex = offerRuleIndexByButton[request.rawButtonId] ?: return@buttonBinding false
                client.toggleDuelRule(ruleIndex)
            },
            buttonBinding(-1, 1, "duel.body_rule", bodyRuleButtons) { client, request ->
                val ruleIndex = bodyRuleButtons.indexOf(request.rawButtonId)
                client.toggleDuelBodyRule(ruleIndex)
            },
            buttonBinding(-1, 2, "duel.confirm.stage_two", intArrayOf(CONFIRM_STAGE_TWO_BUTTON)) { client, _ ->
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
            buttonBinding(-1, 3, "duel.confirm.stage_one", intArrayOf(CONFIRM_STAGE_ONE_BUTTON)) { client, _ ->
                val other = client.getClient(client.duel_with)
                if (other == null || client.slot == other.slot || !client.inDuel || client.duelConfirmed) {
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
        )
}
