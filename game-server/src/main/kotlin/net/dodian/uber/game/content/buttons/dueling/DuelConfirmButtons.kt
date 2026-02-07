package net.dodian.uber.game.content.buttons.dueling

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString

object DuelConfirmButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(25120, 26018)

    override fun onClick(client: Client, buttonId: Int): Boolean {
        when (buttonId) {
            25120 -> {
                if (System.currentTimeMillis() - client.lastButton < 1000) {
                    client.lastButton = System.currentTimeMillis()
                    return true
                }
                client.lastButton = System.currentTimeMillis()
                val other = client.getClient(client.duel_with)
                if (other == null || client.slot == other.slot || !client.inDuel || client.duelConfirmed2) {
                    return true
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
                return true
            }

            26018 -> {
                val other = client.getClient(client.duel_with)
                if (other == null || client.slot == other.slot || !client.inDuel || client.duelConfirmed) {
                    return true
                }

                val sendMsgToOther = client.maxHealth - client.currentHealth == 0 && other.maxHealth - other.currentHealth != 0
                if (other.maxHealth - other.currentHealth != 0 || client.maxHealth - client.currentHealth != 0) {
                    client.send(SendMessage(if (sendMsgToOther) "Your opponent is low on health!" else "You are low on health, so please heal up!"))
                    if (sendMsgToOther) {
                        other.send(SendMessage("You are low on health, so please heal up!"))
                    }
                    return true
                }

                if (System.currentTimeMillis() - client.lastButton < 1000) {
                    client.lastButton = System.currentTimeMillis()
                    return true
                }
                client.lastButton = System.currentTimeMillis()
                if (!client.validClient(client.duel_with)) {
                    client.declineDuel()
                }

                client.duelConfirmed = true
                if (other.duelConfirmed) {
                    if (client.duelRule[0] && client.duelRule[1] && client.duelRule[2]) {
                        client.declineDuel()
                        client.send(SendMessage("At least one combat style must be enabled!"))
                        other.send(SendMessage("At least one combat style must be enabled!"))
                        return true
                    }
                    if (client.hasEnoughSpace() || other.hasEnoughSpace()) {
                        client.send(SendMessage(client.failer))
                        other.send(SendMessage(client.failer))
                        client.declineDuel()
                        return true
                    }
                    client.canOffer = false
                    client.confirmDuel()
                    other.confirmDuel()
                } else {
                    client.send(SendString("Waiting for other player...", 6684))
                    other.send(SendString("Other player has accepted.", 6684))
                }
                return true
            }
        }
        return false
    }
}

