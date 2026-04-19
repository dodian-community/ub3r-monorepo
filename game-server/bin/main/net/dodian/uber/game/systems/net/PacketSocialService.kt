package net.dodian.uber.game.systems.net

import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.widget.CommandEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.persistence.audit.ConsoleAuditLog
import net.dodian.uber.game.systems.interaction.commands.CommandDispatcher

/**
 * Kotlin service for social/chat-command packet side-effects that must stay
 * out of Netty inbound listeners.
 */
object PacketSocialService {
    /**
     * Applies command guards, posts the command event, then dispatches through
     * the systems-layer command runtime.
     */
    @JvmStatic
    fun handleCommand(client: Client, command: String) {
        if (PacketInteractionRequestService.rejectInvalidClientCommand(client)) {
            return
        }

        ConsoleAuditLog.command(client, command)

        val parts = command.split(" ")
        if (GameEventBus.postWithResult(CommandEvent(client, command, parts))) {
            return
        }
        CommandDispatcher.dispatch(client, command)
    }

    @JvmStatic
    fun handleAddFriend(client: Client, encodedName: Long) {
        client.addFriend(encodedName)
    }

    @JvmStatic
    fun handleRemoveFriend(client: Client, encodedName: Long) {
        client.removeFriend(encodedName)
    }

    @JvmStatic
    fun handleAddIgnore(client: Client, encodedName: Long) {
        client.addIgnore(encodedName)
    }

    @JvmStatic
    fun handleRemoveIgnore(client: Client, encodedName: Long) {
        client.removeIgnore(encodedName)
    }
}

