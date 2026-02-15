package net.dodian.uber.game.content.objects.impl.thieving

import net.dodian.uber.game.Server
import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.utilities.Misc

object PlunderObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(
        20275, 20277, 20931, 20932, 26580,
        26600, 26601, 26602, 26603, 26604, 26605, 26606, 26607, 26608, 26609, 26610, 26611, 26612, 26613,
        26616, 26618, 26619, 26620, 26621, 26622, 26623, 26624, 26625, 26626,
    )

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        return when {
            objectId in 26622..26625 -> {
                if (client.getLevel(Skill.THIEVING) < 21 || client.stunTimer > 0) {
                    client.send(
                        SendMessage(
                            if (client.getLevel(Skill.THIEVING) < 21) {
                                "You need level 21 thieving to enter."
                            } else {
                                "You are stunned!"
                            },
                        ),
                    )
                    return true
                }
                if (Server.entryObject.getEntryDoor(position)) {
                    val chance = Misc.random(255)
                    if (chance <= (client.getLevel(Skill.THIEVING) * 2.5).toInt()) {
                        client.transport(Position(1934, 4450, 2))
                    } else {
                        client.dealDamage(null, Misc.random(3), Entity.hitType.STANDARD)
                        client.stunTimer = 4
                    }
                } else {
                    client.transport(Position(1968, 4420, 2))
                }
                true
            }
            objectId in 26618..26621 -> {
                if (client.getPlunder.roomNr + 1 == 8) {
                    return true
                }
                if (Server.entryObject.nextRoom[client.getPlunder.roomNr] + 26618 == objectId && client.getPlunder.openDoor(objectId)) {
                    client.getPlunder.nextRoom()
                } else if (client.getPlunder.openDoor(objectId)) {
                    client.send(SendMessage("This tomb door lead nowhere."))
                } else {
                    client.getPlunder.toggleObstacles(objectId)
                }
                true
            }
            objectId == 20932 -> {
                client.transport(client.getPlunder.end)
                true
            }
            objectId == 20931 -> {
                client.NpcDialogue = 20931
                true
            }
            objectId == 26616 || objectId == 26626 -> {
                client.getPlunder.toggleObstacles(objectId)
                true
            }
            objectId == 26580 || objectId in 26600..26613 -> {
                client.getPlunder.toggleObstacles(objectId)
                true
            }
            objectId == 20275 -> {
                client.transport(Position(2799, 5160, 0))
                client.setFocus(2799, 5159)
                true
            }
            objectId == 20277 -> {
                client.transport(Position(3315, 2796, 0))
                client.setFocus(3315, 2797)
                true
            }
            else -> false
        }
    }

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId == 20931) {
            client.getPlunder.resetPlunder()
            return true
        }
        return false
    }
}
