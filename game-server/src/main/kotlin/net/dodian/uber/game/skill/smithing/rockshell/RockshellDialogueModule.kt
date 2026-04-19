package net.dodian.uber.game.skill.smithing.rockshell

import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.engine.systems.dialogue.DialogueIds
import net.dodian.uber.game.engine.systems.dialogue.core.DialogueRenderRegistry
import net.dodian.uber.game.engine.systems.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.ui.dialogue.DialogueUi
import net.dodian.uber.game.model.player.skills.Skill

object RockshellDialogueModule : DialogueRenderModule {
    override fun register(builder: DialogueRenderRegistry.Builder) {
        builder.handle(DialogueIds.Misc.ROCKSHELL_MENU) { c ->
            if (c.getLevel(Skill.SMITHING) >= 60 && c.playerHasItem(2347)) {
                DialogueUi.showPlayerOption(c, arrayOf("What would you like to make?", "Head", "Body", "Legs", "Boots", "Gloves"))
            } else {
                c.sendMessage(if (c.getLevel(Skill.SMITHING) < 60) "You need level 60 smithing to do this." else "You need a hammer to handle this material.")
                DialogueService.setDialogueSent(c, true)
            }
            true
        }
    }
}

