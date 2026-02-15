package net.dodian.uber.game.content.objects.impl.thieving

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

object StallObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(4877)

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        client.skillX = position.x
        client.setSkillY(position.y)
        client.WanneThieve = 4877
        return true
    }
}
