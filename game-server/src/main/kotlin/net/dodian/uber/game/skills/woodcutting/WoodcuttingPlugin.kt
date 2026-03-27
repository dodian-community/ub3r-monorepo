package net.dodian.uber.game.skills.woodcutting

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.woodcutting.WoodcuttingService

object WoodcuttingPlugin {
    @JvmStatic
    fun attempt(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean =
        WoodcuttingService.startWoodcutting(client, objectId, position, obj)

    @JvmStatic
    fun stop(client: Client, fullReset: Boolean) = WoodcuttingService.stopWoodcutting(client, fullReset)
}
