package net.dodian.uber.game.skill.agility

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.engine.systems.skills.ProgressionService
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage

object AgilityTicketExchangeService {
    private const val AGILITY_TICKET_ID = 2996
    private const val MINIMUM_TICKET_EXCHANGE = 10
    private const val XP_PER_TICKET = 700

    @JvmStatic
    fun spendTickets(client: Client) {
        client.send(RemoveInterfaces())

        var ticketSlot = -1
        var slot = 0
        while (slot < client.playerItems.size) {
            if (client.playerItems[slot] - 1 == AGILITY_TICKET_ID) {
                ticketSlot = slot
                break
            }
            slot++
        }

        if (ticketSlot == -1) {
            client.send(SendMessage("You have no agility tickets!"))
            return
        }

        val amount = client.playerItemsN[ticketSlot]
        if (amount < MINIMUM_TICKET_EXCHANGE) {
            client.send(SendMessage("You must hand in at least 10 tickets at once"))
            return
        }

        ProgressionService.addXp(client, amount * XP_PER_TICKET, Skill.AGILITY)
        client.send(SendMessage("You exchange your $amount agility tickets"))
        client.deleteItem(AGILITY_TICKET_ID, amount)
        client.checkItemUpdate()
    }
}
