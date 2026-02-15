package net.dodian.uber.game.content.objects.impl.travel

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage

object PassageObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(882, 1521, 1524, 2309, 23271, 2391, 2392, 2623, 2624, 2625, 2634, 16466, 16509, 16510)

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        return when (objectId) {
            23271 -> {
                client.transport(Position(position.x, position.y + if (client.position.y == 3523) -1 else 2, position.z))
                true
            }
            16466 -> {
                if (client.getLevel(Skill.AGILITY) < 75) {
                    client.send(SendMessage("You need level 75 agility to use this shortcut!"))
                } else {
                    client.transport(Position(2863, if (client.position.y == 2971) 2976 else 2971, 0))
                }
                true
            }
            882 -> {
                when {
                    position.x == 2899 && position.y == 9728 -> {
                        if (client.getLevel(Skill.AGILITY) < 85) {
                            client.send(SendMessage("You need level 85 agility to use this shortcut!"))
                        } else {
                            client.transport(Position(2885, 9795, 0))
                        }
                    }
                    position.x == 2885 && position.y == 9794 -> {
                        if (client.getLevel(Skill.AGILITY) < 85) {
                            client.send(SendMessage("You need level 85 agility to use this shortcut!"))
                        } else {
                            client.transport(Position(2899, 9729, 0))
                        }
                    }
                    else -> return false
                }
                true
            }
            16509 -> {
                if (!client.checkItem(989) || client.getLevel(Skill.AGILITY) < 70) {
                    client.send(SendMessage("You need a crystal key and 70 agility to use this shortcut!"))
                } else if (client.position.x == 2886 && client.position.y == 9799) {
                    client.transport(Position(2892, 9799, 0))
                } else if (client.position.x == 2892 && client.position.y == 9799) {
                    client.transport(Position(2886, 9799, 0))
                }
                true
            }
            16510 -> {
                if (!client.checkItem(989) || client.getLevel(Skill.AGILITY) < 70) {
                    client.send(SendMessage("You need a crystal key and 70 agility to use this shortcut!"))
                } else if (client.position.x == 2880 && client.position.y == 9813) {
                    client.transport(Position(2878, 9813, 0))
                } else if (client.position.x == 2878 && client.position.y == 9813) {
                    client.transport(Position(2880, 9813, 0))
                }
                true
            }
            2391, 2392 -> {
                if (client.premium) {
                    client.ReplaceObject(2728, 3349, 2391, 0, 0)
                    client.ReplaceObject(2729, 3349, 2392, -2, 0)
                }
                true
            }
            2309 -> {
                if (position.x == 2998 && position.y == 3917) {
                    if (client.getLevel(Skill.AGILITY) < 75) {
                        client.send(SendMessage("You need at least 75 agility to enter!"))
                    } else {
                        client.ReplaceObject(2998, 3917, 2309, 2, 0)
                    }
                    true
                } else {
                    false
                }
            }
            2624, 2625 -> {
                client.ReplaceObject(2901, 3510, 2624, -1, 0)
                client.ReplaceObject(2901, 3511, 2625, -3, 0)
                client.ReplaceObject(2902, 3510, -1, -1, 0)
                client.ReplaceObject(2902, 3511, -1, -3, 0)
                true
            }
            1521, 1524 -> {
                if ((position.x == 2908 || position.x == 2907) && position.y == 9698) {
                    if (!client.checkItem(989)) {
                        client.send(SendMessage("You need a crystal key to open this door."))
                        return true
                    }
                    if (client.getLevel(Skill.SLAYER) < 120) {
                        client.send(SendMessage("You need at least 120 slayer to enter!"))
                        return true
                    }
                    client.ReplaceObject(2908, 9698, -1, 0, 0)
                    client.ReplaceObject(2907, 9698, -1, 0, 0)
                    client.ReplaceObject(2908, 9697, 1516, 2, 0)
                    client.ReplaceObject(2907, 9697, 1516, 0, 0)
                    true
                } else {
                    false
                }
            }
            2623 -> {
                if (client.checkItem(989)) {
                    client.ReplaceObject(2924, 9803, 2623, -3, 0)
                } else {
                    client.send(SendMessage("You need the crystal key to enter"))
                    client.send(SendMessage("The crystal key is made from 2 crystal pieces"))
                }
                true
            }
            2634 -> {
                if (position.x == 2838 && position.y == 3517) {
                    client.send(SendMessage("You jump to the other side of the rubble"))
                    client.transport(Position(2840, 3517, 0))
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }
}
