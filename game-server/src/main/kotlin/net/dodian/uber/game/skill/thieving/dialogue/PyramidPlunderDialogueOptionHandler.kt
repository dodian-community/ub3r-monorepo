package net.dodian.uber.game.skill.thieving.dialogue

import net.dodian.uber.game.engine.systems.dialogue.DialogueIds
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.skill.thieving.PyramidPlunder

object PyramidPlunderDialogueOptionHandler {
    @JvmStatic
    fun handle(client: Client, dialogueId: Int, button: Int): Boolean {
        if (dialogueId != DialogueIds.Misc.PYRAMID_EXIT) {
            return false
        }

        if (button == 1) {
            PyramidPlunder.reset(client)
        }
        client.send(RemoveInterfaces())
        return true
    }
}

