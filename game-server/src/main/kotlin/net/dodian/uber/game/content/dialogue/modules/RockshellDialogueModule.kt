package net.dodian.uber.game.content.dialogue.modules

import net.dodian.uber.game.content.dialogue.legacy.core.DialogueIds
import net.dodian.uber.game.content.dialogue.legacy.core.DialogueRegistry
import net.dodian.uber.game.content.dialogue.legacy.core.DialogueRenderModule
import net.dodian.uber.game.content.dialogue.legacy.core.DialogueUi
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage

object RockshellDialogueModule : DialogueRenderModule {
    override fun register(builder: DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Misc.ROCKSHELL_MENU) { c ->
            if (c.getLevel(Skill.SMITHING) >= 60 && c.playerHasItem(2347)) {
                DialogueUi.showPlayerOption(c, arrayOf("What would you like to make?", "Head", "Body", "Legs", "Boots", "Gloves"))
            } else {
                c.send(SendMessage(if (c.getLevel(Skill.SMITHING) < 60) "You need level 60 smithing to do this." else "You need a hammer to handle this material."))
                c.NpcDialogueSend = true
            }
            true
        }
    }
}
