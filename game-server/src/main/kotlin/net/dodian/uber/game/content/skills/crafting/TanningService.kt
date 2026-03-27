package net.dodian.uber.game.content.skills.crafting

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString

object TanningService {
    private val titleByType = mapOf(2 to "Green", 3 to "Blue", 4 to "Red", 5 to "Black")
    private val costByType = mapOf(2 to "1,000gp", 3 to "2,000gp", 4 to "5,000gp", 5 to "10,000gp")

    @JvmStatic
    fun open(client: Client) {
        client.sendString("Regular Leather", 14777)
        client.sendString("50gp", 14785)
        client.sendString("", 14781)
        client.sendString("", 14789)
        client.sendString("", 14778)
        client.sendString("", 14786)
        client.sendString("", 14782)
        client.sendString("", 14790)

        val soon = intArrayOf(14779, 14787, 14783, 14791, 14780, 14788, 14784, 14792)
        var type = 2
        for (i in soon.indices) {
            val label =
                if (i % 2 == 0) {
                    titleByType[type].orEmpty()
                } else {
                    costByType[type].orEmpty().also { type++ }
                }
            client.sendString(label, soon[i])
        }

        client.sendInterfaceModel(14769, 250, 1741)
        client.sendInterfaceModel(14773, 250, -1)
        client.sendInterfaceModel(14771, 250, 1753)
        client.sendInterfaceModel(14772, 250, 1751)
        client.sendInterfaceModel(14775, 250, 1749)
        client.sendInterfaceModel(14776, 250, 1747)
        client.openInterface(14670)
    }

    @JvmStatic
    fun start(client: Client, request: TanningRequest): Boolean {
        val definition = TanningDefinitions.find(request.hideType) ?: return false
        if (!client.playerHasItem(995, definition.coinCost)) {
            client.sendMessage("You need atleast ${definition.coinCost} coins to do this!")
            return true
        }
        var amount = request.amount
        amount = if (client.getInvAmt(995) > amount * definition.coinCost) client.getInvAmt(995) / definition.coinCost else amount
        amount = minOf(amount, client.getInvAmt(definition.hideId))
        repeat(amount.coerceAtLeast(0)) {
            client.deleteItem(definition.hideId, 1)
            client.deleteItem(995, definition.coinCost)
            client.addItem(definition.leatherId, 1)
            client.checkItemUpdate()
        }
        return true
    }
}
