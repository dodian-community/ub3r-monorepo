package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRegistry
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueUi
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage

/**
 * Handles rockshell smithing choice menu.
 * - Dialogue ID: 10000
 */
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
