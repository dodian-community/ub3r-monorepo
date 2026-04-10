package net.dodian.uber.game.engine.event.bootstrap

import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.item.ItemExamineEvent

/** Handles examine-item responses wired from ItemExamineEvent. */
object ItemExamineBootstrap {
    @JvmStatic
    fun bootstrap() {
        GameEventBus.on<ItemExamineEvent> { event ->
            event.client.examineItem(event.client, event.itemId, event.contextValue)
            true
        }
    }
}

