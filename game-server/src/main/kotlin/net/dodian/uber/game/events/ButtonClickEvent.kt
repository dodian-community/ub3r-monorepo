package net.dodian.uber.game.events

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.event.GameEvent
import net.dodian.uber.game.systems.ui.buttons.ButtonClickRequest

data class ButtonClickEvent(
    val request: ButtonClickRequest,
) : GameEvent {
    val client: Client
        get() = request.client

    val buttonId: Int
        get() = request.rawButtonId
}
