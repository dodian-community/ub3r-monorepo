package net.dodian.uber.game.social

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Friend
import net.dodian.uber.game.engine.config.rankAdminGroupIds
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.netty.listener.out.RemoveFriend
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.audit.ChatLog
import net.dodian.utilities.Utils

/** Owns friend, ignore, and private-message behavior; lists remain player-persisted data. */
object PlayerSocialService {
    @JvmStatic fun addFriend(client: Client, name: Long) {
        if (client.friends.any { it.name == name }) {
            client.send(SendMessage("${Utils.longToPlayerName(name)} is already on your friends list"))
            return
        }
        client.friends.add(Friend(name, true))
        PlayerRegistry.playersOnline.values.filter { hasFriend(it, client.longName) }.forEach(::refreshFriends)
        refreshFriends(client)
    }

    @JvmStatic fun removeFriend(client: Client, name: Long) {
        if (client.friends.removeIf { it.name == name }) {
            client.send(RemoveFriend(name))
            refreshFriends(client)
        }
    }

    @JvmStatic fun addIgnore(client: Client, name: Long) {
        if (client.ignores.size >= 100) {
            client.send(SendMessage("Maximum ignores reached!"))
            return
        }
        if (client.ignores.any { it.name == name }) {
            client.send(SendMessage("You already got this guy on your ignoreList!"))
            return
        }
        client.ignores.add(Friend(name, true))
        PlayerRegistry.playersOnline[name]?.let(::refreshFriends)
    }

    @JvmStatic fun removeIgnore(client: Client, name: Long) {
        if (client.ignores.removeIf { it.name == name }) {
            refreshFriends(client)
            PlayerRegistry.playersOnline[name]?.let(::refreshFriends)
        }
    }

    @JvmStatic fun hasFriend(client: Client, name: Long): Boolean = client.friends.any { it.name == name }

    @JvmStatic fun refreshFriends(client: Client) {
        client.friends.forEach { friend ->
            val player = PlayerRegistry.playersOnline[friend.name]
            val ignored = player?.ignores?.any { it.name == client.longName } == true
            client.loadpm(friend.name, if (player == null || ignored) 0 else 1)
        }
    }

    @JvmStatic fun sendPrivateMessage(client: Client, friend: Long, payload: ByteArray, size: Int) {
        if (client.isMuted) {
            client.send(SendMessage("You are currently muted!"))
            return
        }
        if (!hasFriend(client, friend)) {
            client.send(SendMessage("That player is not on your friends list"))
            return
        }
        val recipient = PlayerRegistry.playersOnline[friend]
        if (recipient == null) {
            client.send(SendMessage("That player is not online"))
            return
        }
        if (rankAdminGroupIds.contains(recipient.playerGroup) && recipient.busy && client.playerRights < 1) {
            client.send(SendMessage("<col=FF0000>This player is busy and did not receive your message."))
            return
        }
        if (recipient.Privatechat != 0 && (recipient.Privatechat != 1 || !hasFriend(recipient, client.longName))) {
            client.send(SendMessage("That player is not available"))
            return
        }
        recipient.sendpm(client.longName, client.playerRights, payload, size)
        ChatLog.recordPrivateChat(client, recipient, Utils.textUnpack(payload, size))
    }
}
