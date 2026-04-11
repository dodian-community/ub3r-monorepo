package net.dodian.uber.game.engine.systems.net

import net.dodian.uber.game.content.skills.smithing.SmithingData
import net.dodian.uber.game.model.entity.player.Client

/**
 * Kotlin service for button-click packet state mutations (opcodes 185, 186).
 *
 * Moves [Client.lastButtonActionIndex], [Client.actionButtonId] assignments
 * and [Client.resetAction] calls out of the Netty listener layer.
 */
object PacketButtonService {

    /**
     * Records the decoded action-index value from opcode 186.
     * Called before throttle validation so the index is always captured.
     */
    @JvmStatic
    fun recordLastActionIndex(client: Client, actionIndex: Int) {
        client.lastButtonActionIndex = actionIndex
    }

    /**
     * Returns true if the player currently has the smelting interface (2400) open.
     * Used for warning-log checks that were previously inlined in the listener.
     */
    @JvmStatic
    fun isSmeltingInterfaceActive(client: Client): Boolean = client.activeInterfaceId == 2400

    /**
     * Applies pre-action bookkeeping:
     * - Stores [actionButton] in [Client.actionButtonId] (if not a scroll button).
     * - Conditionally calls [Client.resetAction] unless the current interface
     *   is preserving a smelting/smithing selection or a specific button that
     *   must not trigger a reset.
     */
    @JvmStatic
    fun prepareAction(client: Client, actionButton: Int) {
        if (!(actionButton >= 9157 && actionButton <= 9194)) {
            client.actionButtonId = actionButton
        }
        val preserveSmeltingSelection = client.activeInterfaceId == 2400 &&
            SmithingData.isSmeltingInterfaceButton(actionButton)
        val preserveSmithingSelection = client.activeInterfaceId in 1119..1123
        if (!preserveSmeltingSelection && !preserveSmithingSelection &&
            actionButton != 10239 && actionButton != 10238 &&
            actionButton != 6212 && actionButton != 6211
        ) {
            client.resetAction(false)
        }
    }
}

