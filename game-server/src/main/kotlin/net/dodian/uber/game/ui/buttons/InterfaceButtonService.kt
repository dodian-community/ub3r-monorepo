package net.dodian.uber.game.ui.buttons

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.skills.SkillInteractionDispatcher

object InterfaceButtonService {
    @JvmStatic
    fun tryHandle(client: Client, rawButtonId: Int, opIndex: Int): Boolean {
        if (SkillInteractionDispatcher.tryHandleButton(client, rawButtonId, opIndex)) {
            return true
        }
        return InterfaceButtonRegistry.tryHandle(client, rawButtonId, opIndex)
    }
}
