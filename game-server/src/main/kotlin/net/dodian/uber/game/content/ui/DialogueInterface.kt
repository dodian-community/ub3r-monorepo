package net.dodian.uber.game.content.ui

import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.widget.DialogueOptionEvent
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendFrame27
import net.dodian.uber.game.systems.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.systems.ui.buttons.buttonBinding
import net.dodian.uber.game.systems.ui.dialogue.DialogueService
import net.dodian.uber.game.systems.ui.dialogue.core.DialogueIds

object DialogueInterface : InterfaceButtonContent {
    private const val INTERFACE_ID = 0

    private val optionOne = intArrayOf(2461, 9157, 9167, 9178, 9190)
    private val optionTwo = intArrayOf(2462, 9158, 9168, 9179, 9191)
    private val optionThree = intArrayOf(9169, 9180, 9192)
    private val optionFour = intArrayOf(9181, 9193)
    private val optionFive = intArrayOf(9194)
    private val toggleSpecialsButtons = intArrayOf(150)
    private val toggleBossYellButtons = intArrayOf(151)

    override val bindings =
        listOf(
            buttonBinding(INTERFACE_ID, 1, "dialogue.option.1", optionOne) { client, request ->
                handleOption(client, request.rawButtonId, 1)
            },
            buttonBinding(INTERFACE_ID, 2, "dialogue.option.2", optionTwo) { client, request ->
                handleOption(client, request.rawButtonId, 2)
            },
            buttonBinding(INTERFACE_ID, 3, "dialogue.option.3", optionThree) { client, request ->
                handleOption(client, request.rawButtonId, 3)
            },
            buttonBinding(INTERFACE_ID, 4, "dialogue.option.4", optionFour) { client, request ->
                handleOption(client, request.rawButtonId, 4)
            },
            buttonBinding(INTERFACE_ID, 5, "dialogue.option.5", optionFive) { client, request ->
                handleOption(client, request.rawButtonId, 5)
            },
            buttonBinding(-1, 6, "dialogue.state.toggle_specials", toggleSpecialsButtons) { client, _ ->
                DialogueService.setDialogueId(client, DialogueIds.Classic.TOGGLE_SPECIALS)
                DialogueService.setDialogueSent(client, false)
                true
            },
            buttonBinding(-1, 7, "dialogue.state.toggle_boss_yell", toggleBossYellButtons) { client, _ ->
                DialogueService.setDialogueId(client, DialogueIds.Classic.TOGGLE_BOSS_YELL)
                DialogueService.setDialogueSent(client, false)
                true
            },
        )

    private fun handleOption(client: net.dodian.uber.game.model.entity.player.Client, rawButtonId: Int, optionIndex: Int): Boolean {
        if (client.refundSlot != -1) {
            handleRefundMenu(client, optionIndex)
            return true
        }
        if (client.herbMaking != -1) {
            handleHerbMenu(client, optionIndex)
            return true
        }
        if (GameEventBus.postWithResult(DialogueOptionEvent(client, optionIndex))) {
            return true
        }
        if (DialogueService.onOption(client, optionIndex)) {
            return true
        }

        return when (rawButtonId) {
            2461, 9157 -> {
                if (client.discord) {
                    client.send(RemoveInterfaces())
                    Player.openPage(client, "https://discord.gg/WZP5mByJ8e")
                    client.discord = false
                }
                client.triggerChat(1)
                when (DialogueService.currentDialogueId(client)) {
                    2 -> {
                        DialogueService.setDialogueId(client, 0)
                        DialogueService.setDialogueSent(client, false)
                        client.openUpBank()
                    }
                    4 -> {
                        DialogueService.setDialogueId(client, 0)
                        DialogueService.setDialogueSent(client, false)
                        client.openUpShop(2)
                    }
                    1001 -> client.send(SendFrame27())
                    22 -> {
                        DialogueService.setDialogueId(client, 23)
                        DialogueService.setDialogueSent(client, false)
                    }
                    27 -> {
                        client.yellOn = true
                        client.sendMessage("You have enabled boss yell messages.")
                        client.send(RemoveInterfaces())
                        DialogueService.setDialogueId(client, 0)
                        DialogueService.setDialogueSent(client, false)
                    }
                    9 -> {
                        if (client.determineCombatLevel() >= 80) {
                            client.moveTo(3105, 3933, 0)
                            client.send(RemoveInterfaces())
                        } else {
                            client.sendMessage("You need to be level 80 or above to enter the mage arena.")
                            client.sendMessage("The skeletons at the varrock castle are a good place until then.")
                        }
                    }
                }
                true
            }
            2462, 9158 -> {
                if (client.discord) {
                    client.send(RemoveInterfaces())
                    client.discord = false
                }
                client.triggerChat(2)
                when (DialogueService.currentDialogueId(client)) {
                    2 -> {
                        DialogueService.setDialogueId(client, 0)
                        DialogueService.setDialogueSent(client, false)
                    }
                    4 -> {
                        DialogueService.setDialogueId(client, 5)
                        DialogueService.setDialogueSent(client, false)
                    }
                    22 -> {
                        DialogueService.setDialogueId(client, 24)
                        DialogueService.setDialogueSent(client, false)
                    }
                    1001 -> {
                        client.clearWalkableInterface()
                        client.send(RemoveInterfaces())
                    }
                    27 -> {
                        client.yellOn = false
                        client.sendMessage("You have disabled boss yell messages.")
                        client.send(RemoveInterfaces())
                        DialogueService.setDialogueId(client, 0)
                        DialogueService.setDialogueSent(client, false)
                    }
                }
                true
            }
            else -> {
                client.triggerChat(optionIndex)
                true
            }
        }
    }

    private fun handleRefundMenu(client: net.dodian.uber.game.model.entity.player.Client, optionIndex: Int) {
        val size = client.rewardList.size
        val position = size - client.refundSlot
        if (client.refundSlot == 0 && ((size > 3 && optionIndex == 5) || (size == 3 && optionIndex == 4) || (size == 1 && optionIndex == 2) || (size == 2 && optionIndex == 3))) {
            client.refundSlot = -1
            client.send(RemoveInterfaces())
        } else if ((position > 3) && optionIndex == 4) {
            client.refundSlot += 3
        } else if (client.refundSlot != 0 && ((position <= 3 && optionIndex == position + 1) || (position > 3 && optionIndex == 5))) {
            client.refundSlot -= 3
        } else {
            client.reclaim(optionIndex)
        }
        if (client.rewardList.isNotEmpty()) {
            client.setRefundOptions()
        }
    }

    private fun handleHerbMenu(client: net.dodian.uber.game.model.entity.player.Client, optionIndex: Int) {
        val size = client.herbOptions.size
        val position = size - client.herbMaking
        if (client.herbMaking == 0 && ((size > 3 && optionIndex == 5) || (size == 3 && optionIndex == 4) || (size == 1 && optionIndex == 2) || (size == 2 && optionIndex == 3))) {
            client.herbMaking = -1
            client.send(RemoveInterfaces())
        } else if ((position > 3) && optionIndex == 4) {
            client.herbMaking += 3
        } else if (client.refundSlot != 0 && ((position <= 3 && optionIndex == position + 1) || (position > 3 && optionIndex == 5))) {
            client.herbMaking -= 3
        } else if (client.herbMaking + optionIndex <= size) {
            client.send(RemoveInterfaces())
            client.XinterfaceID = 4753
            client.XremoveSlot = client.herbMaking + optionIndex
            client.herbMaking = -1
            client.send(SendFrame27())
        }
        client.setHerbOptions()
    }
}
