package net.dodian.uber.game.engine.event.bootstrap

import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.skills.api.Skillcape
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.item.ItemDropEvent
import net.dodian.uber.game.netty.listener.out.SendMessage


object ItemDropBootstrap {
    @JvmStatic
    fun bootstrap() {
        GameEventBus.on<ItemDropEvent> { event ->
            val client = event.client
            val droppedItem = event.itemId
            val slot = event.slot

            if (client.isLoggingOut || client.randomed || client.UsingAgility) {
                return@on true
            }

            val nowCycle = GameCycleClock.currentCycle()
            if (nowCycle - client.lastDropCycle < 1) {
                client.send(SendMessage("You must wait a moment before dropping another item."))
                return@on true
            }

            if (slot < 0 || slot > 27) {
                return@on true
            }

            if (client.playerItems[slot] - 1 != droppedItem || client.playerItemsN[slot] < 1) {
                return@on true
            }

            if (droppedItem == 5733) {
                client.deleteItem(droppedItem, slot, 1)
                client.send(SendMessage("A magical force removed this item from your inventory!"))
                client.lastDropCycle = nowCycle
                return@on true
            }

            val isHood = Server.itemManager.getName(droppedItem).contains("hood")
            val skillcape = Skillcape.getSkillCape(if (isHood) droppedItem - 1 else droppedItem)
            if (skillcape != null) {
                client.send(SendMessage("You cannot drop this valuable cape!"))
                return@on true
            }

            val itemName = client.getItemName(droppedItem)
            if (itemName.contains("Max cape") || itemName.contains("Max hood")) {
                client.send(SendMessage("This cape represents mastery; you shouldn't drop it!"))
                return@on true
            }

            if (!client.wearing) {
                client.dropItem(droppedItem, slot)
                client.lastDropCycle = nowCycle
            }
            true
        }
    }
}
