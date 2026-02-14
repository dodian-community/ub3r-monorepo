package net.dodian.uber.game.content.dialogue.legacy.core

import net.dodian.uber.game.content.dialogue.modules.BrimhavenEntryDialogueModule
import net.dodian.uber.game.content.dialogue.modules.PyramidPlunderDialogueModule
import net.dodian.uber.game.content.dialogue.modules.RockshellDialogueModule
import net.dodian.uber.game.content.dialogue.modules.SettingsDialogueModule
import net.dodian.uber.game.content.npcs.spawns.DukeHoracio
import net.dodian.uber.game.content.npcs.spawns.PartyPete
import net.dodian.uber.game.content.npcs.spawns.UnknownNpc1597
import net.dodian.uber.game.content.npcs.spawns.Watcher
import net.dodian.uber.game.model.entity.player.Client

object DialogueRegistry {

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
        PartyPete.registerLegacyDialogues(this)
        include(SettingsDialogueModule)
        UnknownNpc1597.registerLegacyDialogues(this)
        Watcher.registerLegacyDialogues(this)
        include(BrimhavenEntryDialogueModule)
        DukeHoracio.registerLegacyDialogues(this)
        include(RockshellDialogueModule)
        include(PyramidPlunderDialogueModule)
    }.build()

    @JvmStatic
    fun render(client: Client): Boolean {
        val handler = handlers[client.NpcDialogue] ?: return false
        return handler.render(client)
    }
}
