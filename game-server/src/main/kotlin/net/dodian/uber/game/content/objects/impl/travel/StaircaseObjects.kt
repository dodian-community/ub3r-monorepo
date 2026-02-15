package net.dodian.uber.game.content.objects.impl.travel

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.utilities.Utils

object StaircaseObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(
        54, 55, 56, 57,
        1568, 1570,
        1723, 1725, 1726,
        16664,
        1992,
    )

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId == 1725) {
            client.stairs = "legendsUp".hashCode()
            client.skillX = position.x
            client.setSkillY(position.y)
            client.stairDistance = 1
            if (position.x == 2732 && position.y == 3377) {
                if (Utils.getDistance(client.position.x, client.position.y, position.x, position.y) > 2) {
                    return true
                }
                if (client.premium) {
                    client.transport(Position(2732, 3380, 1))
                }
            }
            return true
        }

        if (objectId == 1726) {
            client.stairs = "legendsDown".hashCode()
            client.skillX = position.x
            client.setSkillY(position.y)
            client.stairDistance = 1
            if (position.x == 2732 && position.y == 3378) {
                if (Utils.getDistance(client.position.x, client.position.y, position.x, position.y) > 2) {
                    return true
                }
                if (client.premium) {
                    client.transport(Position(2732, 3376, 0))
                }
            }
            return true
        }

        if (objectId == 16664) {
            if (position.x == 2724 && position.y == 3374) {
                if (!client.premium) {
                    client.resetPos()
                }
                client.transport(Position(2727, 9774, 0))
                return true
            }
            if (position.x == 2603 && position.y == 3078) {
                if (!client.checkItem(1543)) {
                    client.send(SendMessage("You need a red key to go down these stairs!"))
                    return true
                }
                client.transport(Position(2602, 9479, 0))
                return true
            }
            if (position.x == 2569 && position.y == 3122) {
                if (!client.checkItem(1545)) {
                    client.send(SendMessage("You need a yellow key to use this staircase!"))
                    return true
                }
                client.transport(Position(2570, 9525, 0))
                return true
            }
            return false
        }

        if (objectId == 1992 && position.x == 2558 && position.y == 3444) {
            client.transport(Position(2717, 9820, 0))
            return true
        }

        return when (objectId) {
            54 -> setStairs(client, position, 14, 3, 1)
            55 -> setStairs(client, position, 15, 3, 1)
            56 -> setStairs(client, position, 14, 3)
            57 -> setStairs(client, position, 15, 3)
            1568, 1570 -> {
                if (position.x == 2594 && position.y == 3085) {
                    false
                } else {
                    setStairs(client, position, 3, 1)
                }
            }
            1723 -> setStairs(client, position, 22, 2, 2)
            else -> false
        }
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
