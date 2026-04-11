package net.dodian.uber.game.engine.systems.net

import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.widget.ChatMessageEvent
import net.dodian.uber.game.model.entity.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.audit.ChatLog

/**
 * Kotlin service for public-chat packet side-effects (opcode 4).
 *
 * Moves all [Client.setChatText*], [Client.invalidateCachedUpdateBlock] and
 * guard-message sends out of ChatListener, leaving the listener as a
 * pure decode / bounds-check / delegate adapter.
 */
object PacketChatService {

    /**
     * Validates guards and, if all pass, applies the decoded chat message to
     * the player state so it is broadcast in the next sync tick.
     *
     * @param client   the sending player
     * @param color    raw color byte from packet
     * @param effects  raw effects byte from packet
     * @param chat     decoded chat string (max 256 chars, null-terminated)
     * @param chatBytes pre-computed byte representation of [chat]
     */
    @JvmStatic
    fun handlePublicChat(client: Client, color: Int, effects: Int, chat: String, chatBytes: ByteArray) {
        if (!client.validClient) {
            client.send(SendMessage("Please use another client"))
            return
        }
        if (client.isMuted()) {
            client.send(SendMessage("You are currently muted!"))
            return
        }
        if (!Server.chatOn && client.playerRights == 0) {
            client.send(SendMessage("Public chat has been temporarily restricted"))
            return
        }

        client.setChatTextEffects(effects)
        client.setChatTextColor(color)

        val copyLen = minOf(chatBytes.size, client.getChatText().size)
        client.setChatTextSize(copyLen)
        if (copyLen > 0) {
            System.arraycopy(chatBytes, 0, client.getChatText(), 0, copyLen)
        }
        client.setChatTextMessage(chat)
        GameEventBus.post(ChatMessageEvent(client, chat))
        client.invalidateCachedUpdateBlock()
        client.getUpdateFlags().setRequired(UpdateFlag.CHAT, true)
        ChatLog.recordPublicChat(client, chat)
    }
}
