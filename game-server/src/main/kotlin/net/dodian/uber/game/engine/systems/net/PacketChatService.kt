package net.dodian.uber.game.engine.systems.net

import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.config.FeatureStateService
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.widget.ChatMessageEvent
import net.dodian.uber.game.model.entity.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.codec.PublicChatCodec
import net.dodian.uber.game.persistence.audit.ChatLog

/**
 *
 * Moves chat-state mutation and guard-message sends out of ChatListener, leaving the listener as
 * a pure decode / bounds-check / delegate adapter.
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
     */
    @JvmStatic
    fun handlePublicChat(client: Client, color: Int, effects: Int, chat: String) {
        if (!client.validClient) {
            client.send(SendMessage("Please use another client"))
            return
        }
        if (client.isMuted()) {
            client.send(SendMessage("You are currently muted!"))
            return
        }
        if (!FeatureStateService.publicChatYell.get() && client.playerRights == 0) {
            client.send(SendMessage("Public chat has been temporarily restricted"))
            return
        }

        client.chatTextEffects = effects
        client.chatTextColor = color

        val encodedChat = PublicChatCodec.encode(chat)
        val copyLen = minOf(encodedChat.size, client.chatText.size)
        client.chatTextSize = copyLen
        if (copyLen > 0) {
            System.arraycopy(encodedChat, 0, client.chatText, 0, copyLen)
        }
        client.chatTextMessage = chat
        GameEventBus.post(ChatMessageEvent(client, chat))
        client.updateFlags.setRequired(UpdateFlag.CHAT, true)
        ChatLog.recordPublicChat(client, chat)
    }
}
