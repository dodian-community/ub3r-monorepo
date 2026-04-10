package net.dodian.uber.game.events.widget

import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.ui.buttons.ButtonClickRequest

/** Fired when a player clicks a button on any interface. */
data class ButtonClickEvent(
    val request: ButtonClickRequest,
) : GameEvent {
    val client: Client
        get() = request.client

    val buttonId: Int
        get() = request.rawButtonId
}

