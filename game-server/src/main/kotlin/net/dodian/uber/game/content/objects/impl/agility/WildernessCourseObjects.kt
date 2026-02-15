package net.dodian.uber.game.content.objects.impl.agility

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.agility.Agility

object WildernessCourseObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(23132, 23137, 23542, 23556, 23640)

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        val agility = Agility(client)
        return when (objectId) {
            23137 -> {
                agility.WildyPipe()
                true
            }
            23132 -> {
                agility.WildyRope()
                true
            }
            23556 -> {
                agility.WildyStones()
                true
            }
            23542 -> {
                agility.WildyLog()
                true
            }
            23640 -> {
                agility.WildyClimb()
                true
            }
            else -> false
        }
    }
}
