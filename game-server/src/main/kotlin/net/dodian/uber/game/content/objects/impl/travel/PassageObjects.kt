package net.dodian.uber.game.content.objects.impl.travel

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage

object PassageObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(882, 16509, 16510, 16466, 23271)

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
            else -> false
        }
    }
}
