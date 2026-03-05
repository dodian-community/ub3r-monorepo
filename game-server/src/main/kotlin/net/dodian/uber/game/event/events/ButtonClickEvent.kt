package net.dodian.uber.game.event.events

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.event.GameEvent

data class ButtonClickEvent(
    val client: Client,
    val buttonId: Int,
) : GameEvent
