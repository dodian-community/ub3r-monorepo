package net.dodian.uber.game.engine.event.bootstrap

import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.widget.DialogueContinueEvent
import net.dodian.uber.game.systems.ui.dialogue.DialogueService

/** Handles dialogue-continue clicks wired from DialogueContinueEvent. */
object DialogueContinueBootstrap {
    @JvmStatic
    fun bootstrap() {
        GameEventBus.on<DialogueContinueEvent> { event ->
            if (DialogueService.onContinue(event.client)) {
                return@on true
            }
            DialogueService.onIndexedContinue(event.client)
        }
    }
}

