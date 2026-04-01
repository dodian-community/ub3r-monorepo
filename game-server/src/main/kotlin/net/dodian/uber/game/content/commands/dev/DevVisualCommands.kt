package net.dodian.uber.game.content.commands.dev

import net.dodian.cache.`object`.GameObjectDef
import net.dodian.uber.game.content.commands.CommandContent
import net.dodian.uber.game.content.commands.CommandContext
import net.dodian.uber.game.content.commands.commands
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.UpdateFlag
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.netty.listener.out.ObjectAnimation
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.config.gameWorldId

object DevVisualCommands : CommandContent {
    override fun definitions() =
        commands {
            command("obja", "tobj", "varbit", "forcemove", "face", "if", "emote", "heat", "gfx", "head", "skull", "sound") {
                handleDevVisual(this)
            }
        }
}

private fun handleDevVisual(context: CommandContext): Boolean {
    val client = context.client
    val cmd = context.parts
    if (!context.specialRights) {
        return false
    }
    when (context.alias) {
        "obja" -> {
            return try {
                val id = cmd[1].toInt()
                val animation = cmd[2].toInt()
                client.send(ObjectAnimation(GameObjectDef(id, 10, 2, Position(client.position.x, client.position.y + 1, client.position.z)), animation))
                context.reply("Object $id showing animation as $animation")
                true
            } catch (_: Exception) {
                context.usage("Wrong usage.. ::${cmd[0]} objectId animationId")
            }
        }
        "tobj" -> {
            return try {
                val id = cmd[1].toInt()
                val pos = client.position.copy()
                client.ReplaceObject(pos.x, pos.y, id, 0, 10)
                context.reply("Object temporary spawned = $id, at x = ${pos.x} y = ${pos.y} with height ${pos.z}")
                true
            } catch (_: Exception) {
                context.usage("Wrong usage.. ::${cmd[0]} objectId")
            }
        }
        "varbit" -> {
            return try {
                val id = cmd[1].toInt()
                val value = cmd[2].toInt()
                client.varbit(id, value)
                context.reply("You set varbit $id with value $value")
                true
            } catch (_: Exception) {
                context.usage("Wrong usage.. ::${cmd[0]} id value")
            }
        }
        "forcemove" -> {
            context.reply("force move!")
            client.appendForcemovement(client.position, Position(client.position.x, client.position.y + 10), 5, 5, 3)
        }
        "face" -> {
            return try {
                val face = cmd[1].toInt()
                var found: Npc? = null
                for (npc in Server.npcManager.npcMap.values) {
                    if (client.position == npc.position) {
                        found = npc
                    }
                }
                if (found == null) {
                    context.reply("Could not find a npc on this spot!")
                } else {
                    val faceCheck = found.face
                    if (faceCheck != face) {
                        found.face = face
                        context.reply("You set the face of the npc from $faceCheck to $face!")
                        context.reply("Spawn DB persistence is disabled; update Kotlin spawn files for permanent changes.")
                    } else {
                        context.reply("'${found.npcName()}' is already facing the way you want it!")
                    }
                }
                true
            } catch (_: Exception) {
                context.usage("Wrong usage.. ::${cmd[0]} face")
            }
        }
        "if" -> {
            val id = cmd[1].toInt()
            client.openInterface(id)
            context.reply("You open interface $id")
        }
        "emote" -> {
            val id = cmd[1].toInt()
            client.performAnimation(id, 0)
            context.reply("You set animation to: $id")
        }
        "heat" -> {
            client.UsingAgility = !client.UsingAgility
            client.walkBlock = System.currentTimeMillis() + (600 * 30)
            context.reply("You set agility to: ${client.UsingAgility}")
        }
        "gfx" -> {
            val id = cmd[1].toInt()
            client.animation(id, client.position)
            context.reply("You set gfx to: $id")
        }
        "head" -> {
            val icon = cmd[1].toInt()
            client.headIcon = icon
            context.reply("Head : $icon")
            client.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
        }
        "skull" -> {
            if (client.playerRights > 1) {
                val icon = cmd[1].toInt()
                client.skullIcon = icon
                context.reply("Skull : $icon")
                client.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
            }
        }
        "sound" -> {
            if (client.playerRights > 1) {
                val id = cmd[1].toInt()
                client.send(net.dodian.uber.game.netty.listener.out.Sound(id))
                context.reply("Sound playing...$id")
            }
        }
        else -> return false
    }
    return true
}
