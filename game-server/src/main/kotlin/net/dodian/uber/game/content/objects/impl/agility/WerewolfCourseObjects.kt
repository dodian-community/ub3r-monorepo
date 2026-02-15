package net.dodian.uber.game.content.objects.impl.agility

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

object WerewolfCourseObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf()

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean = false
}
