package net.dodian.uber.game.skill.smithing.rockshell

import net.dodian.uber.game.engine.systems.dialogue.DialogueIds
import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.model.entity.player.Client

object RockshellDialogueOptionHandler {
    private const val PLAYER_CHAT_EMOTE = 614

    @JvmStatic
    fun handle(client: Client, dialogueId: Int, button: Int): Boolean {
        if (dialogueId != DialogueIds.Misc.ROCKSHELL_MENU) {
            return false
        }

        when (button) {
            1 -> {
                if (client.playerHasItem(6161) && client.playerHasItem(6159)) {
                    client.deleteItem(6159, 1)
                    client.deleteItem(6161, 1)
                    client.addItem(6128, 1)
                    client.checkItemUpdate()
                    client.showPlayerChat(arrayOf("I just made Rock-shell head."), PLAYER_CHAT_EMOTE)
                } else {
                    client.showPlayerChat(arrayOf("I need the following items:", client.getItemName(6161) + " and " + client.getItemName(6159)), PLAYER_CHAT_EMOTE)
                }
            }

            2 -> {
                if (client.playerHasItem(6157) && client.playerHasItem(6159) && client.playerHasItem(6161)) {
                    client.deleteItem(6157, 1)
                    client.deleteItem(6159, 1)
                    client.deleteItem(6161, 1)
                    client.addItem(6129, 1)
                    client.checkItemUpdate()
                    client.showPlayerChat(arrayOf("I just made Rock-shell body."), PLAYER_CHAT_EMOTE)
                } else {
                    client.showPlayerChat(arrayOf("I need the following items:", client.getItemName(6161) + ", " + client.getItemName(6159) + " and " + client.getItemName(6157)), PLAYER_CHAT_EMOTE)
                }
            }

            3 -> {
                if (client.playerHasItem(6159) && client.playerHasItem(6157)) {
                    client.deleteItem(6157, 1)
                    client.deleteItem(6159, 1)
                    client.addItem(6130, 1)
                    client.checkItemUpdate()
                    client.showPlayerChat(arrayOf("I just made Rock-shell legs."), PLAYER_CHAT_EMOTE)
                } else {
                    client.showPlayerChat(arrayOf("I need the following items:", client.getItemName(6159) + " and " + client.getItemName(6157)), PLAYER_CHAT_EMOTE)
                }
            }

            4 -> {
                if (client.playerHasItem(6161) && client.playerHasItem(6159)) {
                    client.deleteItem(6159, 1)
                    client.deleteItem(6161, 1)
                    client.addItem(6145, 1)
                    client.checkItemUpdate()
                    client.showPlayerChat(arrayOf("I just made Rock-shell boots."), PLAYER_CHAT_EMOTE)
                } else {
                    client.showPlayerChat(arrayOf("I need the following items:", client.getItemName(6161) + " and " + client.getItemName(6159)), PLAYER_CHAT_EMOTE)
                }
            }

            5 -> {
                if (client.playerHasItem(6161, 2)) {
                    client.deleteItem(6161, 1)
                    client.deleteItem(6161, 1)
                    client.addItem(6151, 1)
                    client.checkItemUpdate()
                    client.showPlayerChat(arrayOf("I just made Rock-shell gloves."), PLAYER_CHAT_EMOTE)
                } else {
                    client.showPlayerChat(arrayOf("I need two of " + client.getItemName(6161)), PLAYER_CHAT_EMOTE)
                }
            }
        }

        DialogueService.setDialogueSent(client, true)
        return true
    }
}


