package net.dodian.uber.game.content.objects.impl.travel

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage

object LadderObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(
        492,
        1734, 1738, 1739,
        1740, 1746, 1747, 1749,
        1755, 1757, 1764,
        2113, 2147, 2148,
        2406, 2408,
        5054, 5055,
        5130, 5131,
        5488, 5946, 5947, 6434,
        9294, 9358, 9359,
    )

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        return when (objectId) {
            1747 -> setStairs(client, position, 1, 1)
            1738 -> setStairs(client, position, 1, 2)
            1734 -> setStairs(client, position, 10, 3, 1)
            1755, 5946, 1757 -> setStairs(client, position, 4, 2)
            1764 -> setStairs(client, position, 12, 1)
            2148 -> setStairs(client, position, 8, 1)
            2408 -> setStairs(client, position, 16, 1)
            5055 -> setStairs(client, position, 18, 1)
            5131 -> setStairs(client, position, 20, 1)
            9359 -> setStairs(client, position, 24, 1)
            2406 -> {
                if (client.equipment[Equipment.Slot.WEAPON.id] == 772) {
                    setStairs(client, position, 27, 1)
                } else {
                    false
                }
            }
            1746, 1749, 1740 -> setStairs(client, position, 2, 1)
            5947, 6434 -> setStairs(client, position, 3, 1)
            2113 -> {
                if (client.getLevel(Skill.MINING) >= 60) {
                    setStairs(client, position, 3, 1)
                } else {
                    client.send(SendMessage("You need 60 mining to enter the mining guild."))
                    true
                }
            }
            492 -> setStairs(client, position, 11, 2)
            2147 -> setStairs(client, position, 7, 1)
            5054 -> setStairs(client, position, 17, 1)
            5130 -> setStairs(client, position, 19, 1)
            9358 -> setStairs(client, position, 23, 1)
            5488 -> setStairs(client, position, 28, 1)
            9294 -> {
                if (position.x == 2879 && position.y == 9813) {
                    client.stairs = "trap".hashCode()
                    client.stairDistance = 1
                    client.setSkillX(position.x)
                    client.setSkillY(position.y)
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }

    override fun onThirdClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId == 1739) {
            client.moveTo(client.position.x, client.position.y, client.position.z - 1)
            return true
        }
        return false
    }

    private fun setStairs(
        client: Client,
        position: Position,
        stairs: Int,
        distance: Int,
        distanceAdd: Int = 0,
    ): Boolean {
        client.stairs = stairs
        client.skillX = position.x
        client.setSkillY(position.y)
        client.stairDistance = distance
        client.stairDistanceAdd = distanceAdd
        return true
    }
}
