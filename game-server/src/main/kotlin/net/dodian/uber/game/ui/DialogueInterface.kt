package net.dodian.uber.game.ui

import net.dodian.uber.game.api.content.ContentEvents
import net.dodian.uber.game.events.widget.DialogueOptionEvent
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding
import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.engine.systems.dialogue.DialogueIds
import net.dodian.uber.game.shop.ShopId
import net.dodian.uber.game.social.moderation.ModerationService

object DialogueInterface : InterfaceButtonContent {
    private const val INTERFACE_ID = 0

    private val optionOne = intArrayOf(2461, 9157, 9167, 9178, 9190, 2482, 2471)
    private val optionTwo = intArrayOf(2462, 9158, 9168, 9179, 9191, 2483, 2472)
    private val optionThree = intArrayOf(9169, 9180, 9192, 2484, 2473)
    private val optionFour = intArrayOf(9181, 9193, 2485)
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
        if (ModerationService.hasActiveDialogue(client)) {
            ModerationService.handleDialogue(client, optionIndex)
            return true
        }
        if (client.accountServicesDialogState != 0) {
            AccountServices.handleAccountServicesDialogue(client, optionIndex)
            return true
        }
        if (client.refundSlot != -1) {
            handleRefundMenu(client, optionIndex)
            return true
        }
        if (net.dodian.uber.game.engine.systems.skills.SkillItemListMenuService.isActive(client)) {
            net.dodian.uber.game.engine.systems.skills.SkillItemListMenuService.handleOption(client, optionIndex)
            return true
        }
        if (ContentEvents.postWithResult(DialogueOptionEvent(client, optionIndex))) {
            return true
        }
        if (DialogueService.onOption(client, optionIndex)) {
            return true
        }

        return when (rawButtonId) {
            2461, 9157 -> {
                if (client.discord) {
                    client.send(RemoveInterfaces())
                    ExternalUrlService.open(client, "https://discord.gg/WZP5mByJ8e")
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
                        client.openUpShop(ShopId.LEGACY_SHOP_2.id)
                    }
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
            net.dodian.uber.game.economy.ItemReclaimService.claim(client, optionIndex)
        }
        if (client.rewardList.isNotEmpty()) {
            net.dodian.uber.game.economy.ItemReclaimService.showOptions(client)
        }
    }

}
