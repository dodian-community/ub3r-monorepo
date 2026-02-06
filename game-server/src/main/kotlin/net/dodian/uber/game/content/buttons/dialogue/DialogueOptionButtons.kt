package net.dodian.uber.game.content.buttons.dialogue

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendFrame27
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SetInterfaceWalkable

object DialogueOptionButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        2461, 2462,
        9157, 9158,
        9167, 9168, 9169,
        9178, 9179, 9180,
        9190, 9191, 9192,
        9181, 9193, 9194,
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        when (buttonId) {
            2461, 9157 -> {
                if (client.discord) {
                    client.send(RemoveInterfaces())
                    Player.openPage(client, "https://discord.gg/WZP5mByJ8e")
                    client.discord = false
                }
                client.triggerChat(1)
                when (client.NpcDialogue) {
                    2 -> {
                        client.NpcDialogue = 0
                        client.NpcDialogueSend = false
                        client.openUpBank()
                    }
                    4 -> {
                        client.NpcDialogue = 0
                        client.NpcDialogueSend = false
                        client.openUpShop(2)
                    }
                    1001 -> client.send(SendFrame27())
                    22 -> {
                        client.NpcDialogue = 23
                        client.NpcDialogueSend = false
                    }
                    27 -> {
                        client.yellOn = true
                        client.send(SendMessage("You have enabled boss yell messages."))
                        client.send(RemoveInterfaces())
                        client.NpcDialogue = 0
                        client.NpcDialogueSend = false
                    }
                    9 -> {
                        if (client.determineCombatLevel() >= 80) {
                            client.moveTo(3105, 3933, 0)
                            client.send(RemoveInterfaces())
                        } else {
                            client.send(SendMessage("You need to be level 80 or above to enter the mage arena."))
                            client.send(SendMessage("The skeletons at the varrock castle are a good place until then."))
                        }
                    }
                }
                return true
            }

            2462, 9158 -> {
                if (client.discord) {
                    client.send(RemoveInterfaces())
                    client.discord = false
                }
                client.triggerChat(2)
                when (client.NpcDialogue) {
                    2 -> {
                        client.NpcDialogue = 0
                        client.NpcDialogueSend = false
                    }
                    4 -> {
                        client.NpcDialogue = 5
                        client.NpcDialogueSend = false
                    }
                    22 -> {
                        client.NpcDialogue = 24
                        client.NpcDialogueSend = false
                    }
                    1001 -> {
                        client.send(SetInterfaceWalkable(-1))
                        client.send(RemoveInterfaces())
                    }
                    27 -> {
                        client.yellOn = false
                        client.send(SendMessage("You have disabled boss yell messages."))
                        client.send(RemoveInterfaces())
                        client.NpcDialogue = 0
                        client.NpcDialogueSend = false
                    }
                }
                return true
            }

            9167, 9178, 9190 -> {
                client.triggerChat(1)
                return true
            }

            9168, 9179, 9191 -> {
                client.triggerChat(2)
                return true
            }

            9169, 9180, 9192 -> {
                client.triggerChat(3)
                return true
            }

            9181, 9193 -> {
                client.triggerChat(4)
                return true
            }

            9194 -> {
                client.triggerChat(5)
                return true
            }
        }
        return false
    }
}
