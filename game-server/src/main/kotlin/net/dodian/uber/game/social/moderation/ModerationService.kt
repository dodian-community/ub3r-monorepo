package net.dodian.uber.game.social.moderation

import net.dodian.uber.game.model.entity.player.Client

/**
 * Moderation UI boundary.  Packet and UI adapters use this type rather than
 * reaching into the player session; the remaining implementation is migrated
 * here incrementally with the state holder in the same package.
 */
object ModerationService {
    @JvmStatic fun open(client: Client, targetName: String) = client.openModcp(targetName)
    @JvmStatic fun openList(client: Client) = client.openModcpList()
    @JvmStatic fun handleDialogue(client: Client, option: Int) = client.handleModcpDialogue(option)
}
