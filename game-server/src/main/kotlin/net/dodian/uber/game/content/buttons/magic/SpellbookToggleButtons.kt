package net.dodian.uber.game.content.buttons.magic

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

object SpellbookToggleButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        74212,
        49047, // old magic on
        49046, // old magic off
        23024,
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        if (client.ancients == 1) {
            client.setSidebarInterface(6, 1151)
            client.ancients = 0
            client.send(SendMessage("Normal magic enabled"))
        } else {
            client.setSidebarInterface(6, 12855)
            client.ancients = 1
            client.send(SendMessage("Ancient magic enabled"))
        }
        return true
    }
}

