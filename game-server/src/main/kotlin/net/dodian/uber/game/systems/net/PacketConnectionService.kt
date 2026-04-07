package net.dodian.uber.game.systems.net

import net.dodian.uber.game.model.entity.player.Client

/**
 * Thin Kotlin façade for connection-lifecycle packet side-effects that must not
 * live inside the Netty listener layer.
 *
 * Covers: keepalive, focus-change, region-change, follow-player, and
 * private-chat mode updates.
 */
object PacketConnectionService {

    /** Resets the server-side timeout counter (opcode 0 – keep-alive). */
    @JvmStatic
    fun handleKeepAlive(client: Client) {
        client.resetTimeOutCounter()
    }

    /** Applies a client window focus-state change (opcode 3). */
    @JvmStatic
    fun handleFocusChange(client: Client, focused: Boolean) {
        client.setWindowFocused(focused)
    }

    /**
     * Marks the player as fully loaded into the world (opcodes 121, 210).
     *
     * @param loadCustomObjects true when opcode 121 (initial region load) is received.
     */
    @JvmStatic
    fun handleRegionChange(client: Client, loadCustomObjects: Boolean) {
        if (!client.pLoaded) {
            client.pLoaded = true
        }
        if (!client.IsPMLoaded) {
            client.refreshFriends()
            client.IsPMLoaded = true
        }
        if (loadCustomObjects) {
            client.customObjects()
        }
    }

    /**
     * Updates the player's private-chat mode setting (opcode 95).
     * Does **not** notify friends; the listener performs the friend-list walk
     * which is pure read-only traversal and does not violate boundary rules.
     */
    @JvmStatic
    fun setPrivateChatMode(client: Client, mode: Int) {
        client.Privatechat = mode
    }

    /** Clears the walking queue for a follow-player request (opcode 39). */
    @JvmStatic
    fun handleFollowPlayer(client: Client) {
        client.resetWalkingQueue()
    }
}

