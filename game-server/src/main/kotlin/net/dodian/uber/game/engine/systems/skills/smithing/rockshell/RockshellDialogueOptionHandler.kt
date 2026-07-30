package net.dodian.uber.game.engine.systems.skills.smithing.rockshell

import net.dodian.uber.game.engine.systems.dialogue.DialogueIds
import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.inventory.inventoryTransaction

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
                    if (client.inventoryTransaction { remove(6159); remove(6161); add(6128) }) {
                        client.showPlayerChat(arrayOf("I just made Rock-shell head."), PLAYER_CHAT_EMOTE)
                    }
                } else {
                    client.showPlayerChat(arrayOf("I need the following items:", client.getItemName(6161) + " and " + client.getItemName(6159)), PLAYER_CHAT_EMOTE)
                }
            }

            2 -> {
                if (client.playerHasItem(6157) && client.playerHasItem(6159) && client.playerHasItem(6161)) {
                    if (client.inventoryTransaction { remove(6157); remove(6159); remove(6161); add(6129) }) {
                        client.showPlayerChat(arrayOf("I just made Rock-shell body."), PLAYER_CHAT_EMOTE)
                    }
                } else {
                    client.showPlayerChat(arrayOf("I need the following items:", client.getItemName(6161) + ", " + client.getItemName(6159) + " and " + client.getItemName(6157)), PLAYER_CHAT_EMOTE)
                }
            }

            3 -> {
                if (client.playerHasItem(6159) && client.playerHasItem(6157)) {
                    if (client.inventoryTransaction { remove(6157); remove(6159); add(6130) }) {
                        client.showPlayerChat(arrayOf("I just made Rock-shell legs."), PLAYER_CHAT_EMOTE)
                    }
                } else {
                    client.showPlayerChat(arrayOf("I need the following items:", client.getItemName(6159) + " and " + client.getItemName(6157)), PLAYER_CHAT_EMOTE)
                }
            }

            4 -> {
                if (client.playerHasItem(6161) && client.playerHasItem(6159)) {
                    if (client.inventoryTransaction { remove(6159); remove(6161); add(6145) }) {
                        client.showPlayerChat(arrayOf("I just made Rock-shell boots."), PLAYER_CHAT_EMOTE)
                    }
                } else {
                    client.showPlayerChat(arrayOf("I need the following items:", client.getItemName(6161) + " and " + client.getItemName(6159)), PLAYER_CHAT_EMOTE)
                }
            }

            5 -> {
                if (client.playerHasItem(6161, 2)) {
                    if (client.inventoryTransaction { remove(6161, 2); add(6151) }) {
                        client.showPlayerChat(arrayOf("I just made Rock-shell gloves."), PLAYER_CHAT_EMOTE)
                    }
                } else {
                    client.showPlayerChat(arrayOf("I need two of " + client.getItemName(6161)), PLAYER_CHAT_EMOTE)
                }
            }
        }

        DialogueService.setDialogueSent(client, true)
        return true
    }
}
