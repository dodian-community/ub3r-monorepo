package net.dodian.uber.game.engine.systems.dialogue.core

import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.objects.travel.dialogue.BrimhavenEntryDialogueModule
import net.dodian.uber.game.engine.systems.skills.thieving.dialogue.PyramidPlunderDialogueModule
import net.dodian.uber.game.engine.systems.skills.smithing.rockshell.RockshellDialogueModule
import net.dodian.uber.game.ui.dialogue.modules.SettingsDialogueModule
import net.dodian.uber.game.model.entity.player.Client

object DialogueRenderRegistry {

    class Builder {
        private val handlers = mutableMapOf<Int, DialogueRenderHandler>()

        fun handle(dialogueId: Int, handler: DialogueRenderHandler) {
            require(!handlers.containsKey(dialogueId)) {
                "Duplicate dialogue render handler registered for dialogueId=$dialogueId"
            }
            handlers[dialogueId] = handler
        }

        fun include(module: DialogueRenderModule) {
            module.register(this)
        }

        internal fun build(): Map<Int, DialogueRenderHandler> = handlers.toMap()
    }

    private val handlers: Map<Int, DialogueRenderHandler> = Builder().apply {
        include(SettingsDialogueModule)
        include(BrimhavenEntryDialogueModule)
        include(RockshellDialogueModule)
        include(PyramidPlunderDialogueModule)
    }.build()

    @JvmStatic
    fun render(client: Client): Boolean {
        val handler = handlers[DialogueService.currentDialogueId(client)] ?: return false
        return handler.render(client)
    }
}