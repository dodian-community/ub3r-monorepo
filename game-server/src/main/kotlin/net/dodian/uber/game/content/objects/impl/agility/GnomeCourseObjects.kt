package net.dodian.uber.game.content.objects.impl.agility

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.agility.Agility

object GnomeCourseObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(23134, 23135, 23138, 23139, 23145, 23557, 23559, 23560, 23561)

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        val agility = Agility(client)
        return when (objectId) {
            23145 -> {
                agility.GnomeLog()
                true
            }
            23134 -> {
                if (client.distanceToPoint(position.x, position.y) >= 2) return false
                agility.GnomeNet1()
                true
            }
            23559 -> {
                agility.GnomeTree1()
                true
            }
            23557 -> {
                agility.GnomeRope()
                true
            }
            23560, 23561 -> {
                agility.GnomeTreebranch2()
                true
            }
            23135 -> {
                if (client.distanceToPoint(position.x, position.y) >= 3) return false
                agility.GnomeNet2()
                true
            }
            23138 -> {
                if (client.position.x != 2484 || client.position.y != 3430 || client.distanceToPoint(position.x, position.y) >= 2) {
                    return false
                }
                agility.GnomePipe()
                true
            }
            23139 -> {
                if (client.position.x != 2487 || client.position.y != 3430 || client.distanceToPoint(position.x, position.y) >= 2) {
                    return false
                }
                agility.GnomePipe()
                true
            }
            else -> false
        }
    }
}
