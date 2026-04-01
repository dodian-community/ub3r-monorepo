package net.dodian.uber.game.systems.skills

import net.dodian.uber.game.model.entity.player.Client

object SkillingRandomEventService {
    @JvmStatic
    fun show(client: Client) =
        net.dodian.uber.game.content.skills.core.runtime.SkillingRandomEventService.show(client)

    @JvmStatic
    fun trigger(client: Client, awardedExperience: Int) =
        net.dodian.uber.game.content.skills.core.runtime.SkillingRandomEventService.trigger(client, awardedExperience)
}
