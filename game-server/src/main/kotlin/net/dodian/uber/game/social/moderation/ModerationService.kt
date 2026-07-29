package net.dodian.uber.game.social.moderation

import net.dodian.uber.game.model.entity.player.Client

/**
 * Moderation UI boundary.  Packet and UI adapters use this type rather than
 * reaching into the player session; the remaining implementation is migrated
 * here incrementally with the state holder in the same package.
 */
object ModerationService {
    private const val STATE_KEY = "social.moderation.runtime"

    @JvmStatic
    fun state(client: Client): ModerationRuntimeState =
        client.contentRuntimeState.getPluginAttribute<ModerationRuntimeState>(STATE_KEY)
            ?: ModerationRuntimeState().also { client.contentRuntimeState.putPluginAttribute(STATE_KEY, it) }

    @JvmStatic fun hasActiveDialogue(client: Client): Boolean = state(client).dialogueStage != 0
    @JvmStatic fun close(client: Client) { state(client).clearDialogue() }

    @JvmStatic fun open(client: Client, targetName: String) {
        val state = state(client)
        state.selectedTarget = targetName
        client.openModcp(targetName)
        syncFromLegacy(client, state)
    }

    @JvmStatic fun openList(client: Client) {
        client.openModcpList()
        syncFromLegacy(client, state(client))
    }

    @JvmStatic fun handleDialogue(client: Client, option: Int) {
        val state = state(client)
        client.modcpDialogState = state.dialogueStage
        client.managingName = state.selectedTarget
        client.handleModcpDialogue(option)
        syncFromLegacy(client, state)
    }

    @JvmStatic fun beginDialogue(client: Client, targetName: String) {
        state(client).apply { selectedTarget = targetName; dialogueStage = 1 }
    }

    private fun syncFromLegacy(client: Client, state: ModerationRuntimeState) {
        state.dialogueStage = client.modcpDialogState
        state.selectedTarget = client.managingName
        state.visiblePlayers.apply { clear(); addAll(client.modcpPlayerList) }
    }
}
