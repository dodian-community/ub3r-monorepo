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
        if (client.refundSlot != -1) {
            handleRefundMenu(client, buttonId)
            return true
        }
        if (client.herbMaking != -1) {
            handleHerbMenu(client, buttonId)
            return true
        }

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

    private fun optionSlot(buttonId: Int): Int {
        return when (buttonId) {
            9158, 9168, 9179, 9191 -> 2
            9169, 9180, 9192 -> 3
            9181, 9193 -> 4
            9194 -> 5
            else -> 1
        }
    }

    private fun handleRefundMenu(client: Client, buttonId: Int) {
        val size = client.rewardList.size
        val checkSlot = optionSlot(buttonId)
        val position = size - client.refundSlot
        if (client.refundSlot == 0 && ((size > 3 && checkSlot == 5) || (size == 3 && checkSlot == 4) || (size == 1 && checkSlot == 2) || (size == 2 && checkSlot == 3))) {
            client.refundSlot = -1
            client.send(RemoveInterfaces())
        } else if ((position > 3) && checkSlot == 4) {
            client.refundSlot += 3
        } else if (client.refundSlot != 0 && ((position <= 3 && checkSlot == position + 1) || (position > 3 && checkSlot == 5))) {
            client.refundSlot -= 3
        } else {
            client.reclaim(checkSlot)
        }
        if (!client.rewardList.isEmpty()) {
            client.setRefundOptions()
        }
    }

    private fun handleHerbMenu(client: Client, buttonId: Int) {
        val size = client.herbOptions.size
        val checkSlot = optionSlot(buttonId)
        val position = size - client.herbMaking
        if (client.herbMaking == 0 && ((size > 3 && checkSlot == 5) || (size == 3 && checkSlot == 4) || (size == 1 && checkSlot == 2) || (size == 2 && checkSlot == 3))) {
            client.herbMaking = -1
            client.send(RemoveInterfaces())
        } else if ((position > 3) && checkSlot == 4) {
            client.herbMaking += 3
        } else if (client.refundSlot != 0 && ((position <= 3 && checkSlot == position + 1) || (position > 3 && checkSlot == 5))) {
            client.herbMaking -= 3
        } else if (client.herbMaking + checkSlot <= size) {
            client.send(RemoveInterfaces())
            client.XinterfaceID = 4753
            client.XremoveSlot = client.herbMaking + checkSlot
            client.herbMaking = -1
            client.send(SendFrame27())
        }
        client.setHerbOptions()
    }
}
