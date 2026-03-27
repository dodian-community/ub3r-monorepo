package net.dodian.uber.game.content.interfaces.duel

import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.systems.api.content.ContentInteraction
import net.dodian.uber.game.systems.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.systems.ui.buttons.buttonBinding

object DuelInterfaceButtons : InterfaceButtonContent {
    override val bindings =
        listOf(
            buttonBinding(-1, 0, "duel.offer_rule", DuelComponents.offerRuleButtons) { client, request ->
                val ruleIndex = DuelComponents.offerRuleIndexByButton[request.rawButtonId] ?: return@buttonBinding false
                client.toggleDuelRule(ruleIndex)
            },
            buttonBinding(-1, 1, "duel.body_rule", DuelComponents.bodyRuleButtons) { client, request ->
                val ruleIndex = DuelComponents.bodyRuleButtons.indexOf(request.rawButtonId)
                client.toggleDuelBodyRule(ruleIndex)
            },
            buttonBinding(-1, 2, "duel.confirm.stage_two", intArrayOf(DuelComponents.CONFIRM_STAGE_TWO_BUTTON)) { client, _ ->
                if (!ContentInteraction.tryAcquireMs(client, ContentInteraction.DUEL_CONFIRM_STAGE_TWO, 1000L)) {
                    return@buttonBinding true
                }
                val other = client.getClient(client.duel_with)
                if (other == null || client.slot == other.slot || !client.inDuel || client.duelConfirmed2) {
                    return@buttonBinding true
                }
                if (!client.validClient(client.duel_with)) {
                    client.declineDuel()
                } else {
                    client.canOffer = false
                }
                client.duelConfirmed2 = true
                if (other.duelConfirmed2) {
                    client.removeEquipment()
                    other.removeEquipment()
                    client.startDuel()
                    other.startDuel()
                } else {
                    client.send(SendString("Waiting for other player...", 6571))
                    other.send(SendString("Other player has accepted", 6571))
                }
                true
            },
            buttonBinding(-1, 3, "duel.confirm.stage_one", intArrayOf(DuelComponents.CONFIRM_STAGE_ONE_BUTTON)) { client, _ ->
                val other = client.getClient(client.duel_with)
                if (other == null || client.slot == other.slot || !client.inDuel || client.duelConfirmed) {
                    return@buttonBinding true
                }
                val sendMsgToOther = client.maxHealth - client.currentHealth == 0 && other.maxHealth - other.currentHealth != 0
                if (other.maxHealth - other.currentHealth != 0 || client.maxHealth - client.currentHealth != 0) {
                    client.send(SendMessage(if (sendMsgToOther) "Your opponent is low on health!" else "You are low on health, so please heal up!"))
                    if (sendMsgToOther) {
                        other.send(SendMessage("You are low on health, so please heal up!"))
                    }
                    return@buttonBinding true
                }
                if (!ContentInteraction.tryAcquireMs(client, ContentInteraction.DUEL_CONFIRM_STAGE_ONE, 1000L)) {
                    return@buttonBinding true
                }
                if (!client.validClient(client.duel_with)) {
                    client.declineDuel()
                }
                client.duelConfirmed = true
                if (other.duelConfirmed) {
                    if (client.duelRule[0] && client.duelRule[1] && client.duelRule[2]) {
                        client.declineDuel()
                        client.send(SendMessage("At least one combat style must be enabled!"))
                        other.send(SendMessage("At least one combat style must be enabled!"))
                        return@buttonBinding true
                    }
                    if (client.hasEnoughSpace() || other.hasEnoughSpace()) {
                        client.send(SendMessage(client.failer))
                        other.send(SendMessage(client.failer))
                        client.declineDuel()
                        return@buttonBinding true
                    }
                    client.canOffer = false
                    client.confirmDuel()
                    other.confirmDuel()
                } else {
                    client.send(SendString("Waiting for other player...", 6684))
                    other.send(SendString("Other player has accepted.", 6684))
                }
                true
            },
        )
}
