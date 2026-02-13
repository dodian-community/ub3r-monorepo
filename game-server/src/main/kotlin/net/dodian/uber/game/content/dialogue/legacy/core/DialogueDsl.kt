package net.dodian.uber.game.content.dialogue.legacy.core

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces

class DialogueDsl(private val client: Client) {

    fun npcChat(npcId: Int = client.NpcTalkTo, emote: Int, vararg lines: String) {
        DialogueUi.showNpcChat(client, npcId, emote, lines.toList().toTypedArray())
        client.NpcDialogueSend = true
    }

    fun playerChat(emote: Int, vararg lines: String) {
        DialogueUi.showPlayerChat(client, lines.toList().toTypedArray(), emote)
        client.NpcDialogueSend = true
    }

    fun options(vararg lines: String) {
        DialogueUi.showPlayerOption(client, lines.toList().toTypedArray())
        client.NpcDialogueSend = true
    }

    fun setNextDiag(dialogueId: Int) {
        client.nextDiag = dialogueId
    }

    fun setDialogue(dialogueId: Int) {
        client.NpcDialogue = dialogueId
    }

    fun markSent(sent: Boolean = true) {
        client.NpcDialogueSend = sent
    }

    fun close() {
        client.send(RemoveInterfaces())
        client.NpcDialogueSend = false
    }
}

inline fun dialogue(client: Client, block: DialogueDsl.() -> Unit): Boolean {
    DialogueDsl(client).block()
    return true
}
