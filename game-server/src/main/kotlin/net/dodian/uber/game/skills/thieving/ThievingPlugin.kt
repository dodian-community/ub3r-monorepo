package net.dodian.uber.game.skills.thieving

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.thieving.ThievingService

object ThievingPlugin {
    @JvmStatic
    fun attempt(client: Client, entityId: Int, position: Position) = ThievingService.attempt(client, entityId, position)
}
