package net.dodian.uber.game.content.interfaces.dialogue

import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.content.dialogue.core.DialogueIds
import net.dodian.uber.game.event.GameEventBus
import net.dodian.uber.game.event.events.DialogueOptionEvent
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendFrame27
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding

object DialogueInterfaceButtons : InterfaceButtonContent {
    override val bindings =
        listOf(
            buttonBinding(DialogueComponents.INTERFACE_ID, 1, "dialogue.option.1", DialogueComponents.optionOne) { client, request ->
                handleOption(client, request.rawButtonId, 1)
            },
            buttonBinding(DialogueComponents.INTERFACE_ID, 2, "dialogue.option.2", DialogueComponents.optionTwo) { client, request ->
                handleOption(client, request.rawButtonId, 2)
            },
            buttonBinding(DialogueComponents.INTERFACE_ID, 3, "dialogue.option.3", DialogueComponents.optionThree) { client, request ->
                handleOption(client, request.rawButtonId, 3)
            },
            buttonBinding(DialogueComponents.INTERFACE_ID, 4, "dialogue.option.4", DialogueComponents.optionFour) { client, request ->
                handleOption(client, request.rawButtonId, 4)
            },
            buttonBinding(DialogueComponents.INTERFACE_ID, 5, "dialogue.option.5", DialogueComponents.optionFive) { client, request ->
                handleOption(client, request.rawButtonId, 5)
            },
            buttonBinding(-1, 6, "dialogue.state.toggle_specials", DialogueComponents.toggleSpecialsButtons) { client, _ ->
                DialogueService.setDialogueId(client, DialogueIds.Classic.TOGGLE_SPECIALS)
                DialogueService.setDialogueSent(client, false)
                true
            },
            buttonBinding(-1, 7, "dialogue.state.toggle_boss_yell", DialogueComponents.toggleBossYellButtons) { client, _ ->
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
                        client.send(SendMessage("You have enabled boss yell messages."))
                        client.send(RemoveInterfaces())
                        DialogueService.setDialogueId(client, 0)
                        DialogueService.setDialogueSent(client, false)
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
                        client.send(SendMessage("You have disabled boss yell messages."))
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
