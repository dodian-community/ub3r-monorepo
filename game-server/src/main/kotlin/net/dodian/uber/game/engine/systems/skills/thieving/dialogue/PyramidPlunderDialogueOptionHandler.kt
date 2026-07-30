package net.dodian.uber.game.engine.systems.skills.thieving.dialogue

import net.dodian.uber.game.engine.systems.dialogue.DialogueIds
import net.dodian.uber.game.engine.systems.skills.asSkillPlayer
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.skills.thieving.ThievingModule

object PyramidPlunderDialogueOptionHandler {
    @JvmStatic
    fun handle(client: Client, dialogueId: Int, button: Int): Boolean {
        if (dialogueId != DialogueIds.Misc.PYRAMID_EXIT) {
            return false
        }

        if (button == 1) {
            ThievingModule.resetPlunder(client.asSkillPlayer())
        }
        client.send(RemoveInterfaces())
        return true
    }
}
