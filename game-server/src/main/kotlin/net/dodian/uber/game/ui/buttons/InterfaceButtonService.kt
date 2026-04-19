package net.dodian.uber.game.ui.buttons

import net.dodian.uber.game.api.content.ContentErrorPolicy
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.skills.SkillInteractionDispatcher

object InterfaceButtonService {
    @JvmStatic
    fun tryHandle(client: Client, rawButtonId: Int, opIndex: Int): Boolean {
        return ContentErrorPolicy.runBoolean(client, "button.dispatch") {
            if (SkillInteractionDispatcher.tryHandleButton(client, rawButtonId, opIndex)) {
                return@runBoolean true
            }
            InterfaceButtonRegistry.tryHandle(client, rawButtonId, opIndex)
        }
    }
}
