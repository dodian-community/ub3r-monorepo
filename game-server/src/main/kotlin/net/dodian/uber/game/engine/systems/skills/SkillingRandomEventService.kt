package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.model.entity.player.Client

object SkillingRandomEventService {
    @JvmStatic
    fun show(client: Client) {
        net.dodian.uber.game.content.skills.runtime.action.SkillingRandomEventService.show(client)
    }

    @JvmStatic
    fun trigger(client: Client, awardedExperience: Int) {
        net.dodian.uber.game.content.skills.runtime.action.SkillingRandomEventService.trigger(client, awardedExperience)
    }
}

