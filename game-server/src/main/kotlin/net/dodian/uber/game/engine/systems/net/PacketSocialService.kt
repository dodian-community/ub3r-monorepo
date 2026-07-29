package net.dodian.uber.game.engine.systems.net

import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.widget.CommandEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.persistence.audit.ConsoleAuditLog
import net.dodian.uber.game.engine.systems.interaction.commands.CommandDispatcher
import net.dodian.uber.game.social.PlayerSocialService
import net.dodian.uber.game.model.entity.player.PendingInputState

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
        client.contentRuntimeState.setPendingInputState(PendingInputState.ADD_FRIEND)
        client.send(net.dodian.uber.game.netty.listener.out.SendEnterName("Enter name of friend to add:"))
    }

    @JvmStatic
    fun requestRemoveFriend(client: Client) {
        client.contentRuntimeState.setPendingInputState(PendingInputState.REMOVE_FRIEND)
        client.send(net.dodian.uber.game.netty.listener.out.SendEnterName("Enter name of friend to delete:"))
    }

    @JvmStatic
    fun requestAddIgnore(client: Client) {
        client.contentRuntimeState.setPendingInputState(PendingInputState.ADD_IGNORE)
        client.send(net.dodian.uber.game.netty.listener.out.SendEnterName("Enter name of player to add to ignore list:"))
    }

    @JvmStatic
    fun requestRemoveIgnore(client: Client) {
        client.contentRuntimeState.setPendingInputState(PendingInputState.REMOVE_IGNORE)
        client.send(net.dodian.uber.game.netty.listener.out.SendEnterName("Enter name of player to delete from ignore list:"))
    }

    @JvmStatic
    fun handlePendingNameInput(client: Client, encodedName: Long): Boolean {
        return when (client.contentRuntimeState.getPendingInputState()) {
            PendingInputState.ADD_FRIEND -> { client.contentRuntimeState.clearPendingInputState(); handleAddFriend(client, encodedName); true }
            PendingInputState.REMOVE_FRIEND -> { client.contentRuntimeState.clearPendingInputState(); handleRemoveFriend(client, encodedName); true }
            PendingInputState.ADD_IGNORE -> { client.contentRuntimeState.clearPendingInputState(); handleAddIgnore(client, encodedName); true }
            PendingInputState.REMOVE_IGNORE -> { client.contentRuntimeState.clearPendingInputState(); handleRemoveIgnore(client, encodedName); true }
            else -> false
        }
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

}
