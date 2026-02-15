package net.dodian.uber.game.content.objects.impl.agility

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.agility.Agility

object BarbarianCourseObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(16682, 1948, 20211, 23131, 23144, 23547, 23548, 23567)

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        val agility = Agility(client)
        return when (objectId) {
            23131 -> {
                agility.BarbRope()
                true
            }
            23144 -> {
                agility.BarbLog()
                true
            }
            20211 -> {
                agility.BarbNet()
                true
            }
            23547 -> {
                agility.BarbLedge()
                true
            }
            16682 -> {
                agility.BarbStairs()
                true
            }
            1948 -> {
                when {
                    position.x == 2536 && position.y == 3553 -> agility.BarbFirstWall()
                    position.x == 2539 && position.y == 3553 -> agility.BarbSecondWall()
                    position.x == 2542 && position.y == 3553 -> agility.BarbFinishWall()
                    else -> return false
                }
                true
            }
            23567 -> {
                agility.orangeBar()
                true
            }
            23548 -> {
                agility.yellowLedge()
                true
            }
            else -> false
        }
    }
}
