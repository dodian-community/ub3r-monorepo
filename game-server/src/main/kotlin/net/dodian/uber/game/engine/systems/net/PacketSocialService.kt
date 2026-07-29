package net.dodian.uber.game.engine.systems.net

import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.widget.CommandEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.persistence.audit.ConsoleAuditLog
import net.dodian.uber.game.engine.systems.interaction.commands.CommandDispatcher
import net.dodian.uber.game.social.PlayerSocialService

/**
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
        PlayerSocialService.addFriend(client, encodedName)
    }

    @JvmStatic
    fun requestAddFriend(client: Client) {
        clearPendingNameInputs(client)
        client.addFriendPendingInput = true
        client.send(net.dodian.uber.game.netty.listener.out.SendEnterName("Enter name of friend to add:"))
    }

    @JvmStatic
    fun requestRemoveFriend(client: Client) {
        clearPendingNameInputs(client)
        client.removeFriendPendingInput = true
        client.send(net.dodian.uber.game.netty.listener.out.SendEnterName("Enter name of friend to delete:"))
    }

    @JvmStatic
    fun requestAddIgnore(client: Client) {
        clearPendingNameInputs(client)
        client.addIgnorePendingInput = true
        client.send(net.dodian.uber.game.netty.listener.out.SendEnterName("Enter name of player to add to ignore list:"))
    }

    @JvmStatic
    fun requestRemoveIgnore(client: Client) {
        clearPendingNameInputs(client)
        client.removeIgnorePendingInput = true
        client.send(net.dodian.uber.game.netty.listener.out.SendEnterName("Enter name of player to delete from ignore list:"))
    }

    @JvmStatic
    fun handlePendingNameInput(client: Client, encodedName: Long): Boolean {
        if (client.addFriendPendingInput) {
            client.addFriendPendingInput = false
            handleAddFriend(client, encodedName)
            return true
        }
        if (client.removeFriendPendingInput) {
            client.removeFriendPendingInput = false
            handleRemoveFriend(client, encodedName)
            return true
        }
        if (client.addIgnorePendingInput) {
            client.addIgnorePendingInput = false
            handleAddIgnore(client, encodedName)
            return true
        }
        if (client.removeIgnorePendingInput) {
            client.removeIgnorePendingInput = false
            handleRemoveIgnore(client, encodedName)
            return true
        }
        return false
    }

    @JvmStatic
    fun handleRemoveFriend(client: Client, encodedName: Long) {
        PlayerSocialService.removeFriend(client, encodedName)
    }

    @JvmStatic
    fun handleAddIgnore(client: Client, encodedName: Long) {
        PlayerSocialService.addIgnore(client, encodedName)
    }

    @JvmStatic
    fun handleRemoveIgnore(client: Client, encodedName: Long) {
        PlayerSocialService.removeIgnore(client, encodedName)
    }

    private fun clearPendingNameInputs(client: Client) {
        client.addFriendPendingInput = false
        client.removeFriendPendingInput = false
        client.addIgnorePendingInput = false
        client.removeIgnorePendingInput = false
        client.bankSearchPendingInput = false
        client.modcpSearchPendingInput = false
    }
}
