package net.dodian.uber.game.content.npcs.dialogue.core

import net.dodian.uber.game.content.npcs.spawns.AgilityDialogueModule
import net.dodian.uber.game.content.npcs.spawns.AppearanceDialogueModule
import net.dodian.uber.game.content.npcs.spawns.BattlestaffDialogueModule
import net.dodian.uber.game.content.npcs.spawns.BankingDialogueModule
import net.dodian.uber.game.content.npcs.spawns.BoatTravelDialogueModule
import net.dodian.uber.game.content.npcs.dialogue.modules.BrimhavenEntryDialogueModule
import net.dodian.uber.game.content.npcs.spawns.CarpetDialogueModule
import net.dodian.uber.game.content.npcs.spawns.DungeonAccessDialogueModule
import net.dodian.uber.game.content.npcs.dialogue.modules.GamblerDialogueModule
import net.dodian.uber.game.content.npcs.spawns.HerbloreDialogueModule
import net.dodian.uber.game.content.npcs.dialogue.modules.HolidayEventDialogueModule
import net.dodian.uber.game.content.npcs.dialogue.modules.MageArenaDialogueModule
import net.dodian.uber.game.content.npcs.spawns.MaxCapeDialogueModule
import net.dodian.uber.game.content.npcs.dialogue.modules.PyramidPlunderDialogueModule
import net.dodian.uber.game.content.npcs.dialogue.modules.RockshellDialogueModule
import net.dodian.uber.game.content.npcs.spawns.RuneShopDialogueModule
import net.dodian.uber.game.content.npcs.dialogue.modules.SettingsDialogueModule
import net.dodian.uber.game.content.npcs.spawns.SlayerDialogueModule
import net.dodian.uber.game.content.npcs.dialogue.modules.WatcherDialogueModule
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
        include(BankingDialogueModule)
        include(RuneShopDialogueModule)
        include(GamblerDialogueModule)
        include(SettingsDialogueModule)
        include(MageArenaDialogueModule)
        include(WatcherDialogueModule)
        include(CarpetDialogueModule)
        include(AppearanceDialogueModule)
        include(SlayerDialogueModule)
        include(AgilityDialogueModule)
        include(HerbloreDialogueModule)
        include(DungeonAccessDialogueModule)
        include(BrimhavenEntryDialogueModule)
        include(BoatTravelDialogueModule)
        include(BattlestaffDialogueModule)
        include(MaxCapeDialogueModule)
        include(HolidayEventDialogueModule)
        include(RockshellDialogueModule)
        include(PyramidPlunderDialogueModule)
    }.build()

    @JvmStatic
    fun render(client: Client): Boolean {
        val handler = handlers[client.NpcDialogue] ?: return false
        return handler.render(client)
    }
}
