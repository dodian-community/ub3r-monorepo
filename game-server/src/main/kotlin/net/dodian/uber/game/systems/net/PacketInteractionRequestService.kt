package net.dodian.uber.game.systems.net

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.systems.combat.CombatLogoutLockService
import net.dodian.uber.game.systems.interaction.PlayerInteractionGuardService

/**
 * Kotlin service for player-to-player social interaction requests that have
 * been extracted from the Netty listener layer.
 *
 * Covers:
 * - Trade request (opcode 139)
 * - Duel request (opcode 153)
 * - Command invalid-client guard (opcode 103)
 */
object PacketInteractionRequestService {

    // -------------------------------------------------------------------------
    // Trade
    // -------------------------------------------------------------------------

    /**
     * Processes a trade-request packet after the target slot has been decoded
     * and the target player validated.
     *
     * All guard messages and the [Client.tradeReq] call live here rather than
     * in the listener.
     */
    @JvmStatic
    fun handleTradeRequest(client: Client, targetSlot: Int, other: Client) {
        if (client.inHeat() || other.inHeat()) {
            client.send(SendMessage("It would not be a wise idea to trade with the heat in the background!"))
            return
        }
        if (client.isBusy() || other.isBusy()) {
            client.send(
                SendMessage(
                    if (client.isBusy()) "You are currently busy"
                    else "${other.getPlayerName()} is currently busy!"
                )
            )
            return
        }
        val guardMessage = PlayerInteractionGuardService.tradeBlockMessage(client, other)
        if (guardMessage != null) {
            client.send(SendMessage(guardMessage))
            return
        }
        if (!client.inTrade) {
            client.trade_reqId = targetSlot
            client.tradeReq(client.trade_reqId)
        }
    }

    // -------------------------------------------------------------------------
    // Duel
    // -------------------------------------------------------------------------

    /**
     * Processes a duel-request packet after the target pid has been decoded
     * and the target player validated.
     */
    @JvmStatic
    fun handleDuelRequest(client: Client, pid: Int, other: Client) {
        if (client.inWildy() || other.inWildy()) {
            client.send(SendMessage("You cant duel in the wilderness!"))
            return
        }
        if (client.isBusy() || other.isBusy()) {
            client.send(
                SendMessage(
                    if (client.isBusy()) "You are currently busy"
                    else "${other.getPlayerName()} is currently busy!"
                )
            )
            return
        }
        val guardMessage = PlayerInteractionGuardService.duelBlockMessage(client, other)
        if (guardMessage != null) {
            client.send(SendMessage(guardMessage))
            return
        }
        if (CombatLogoutLockService.isLocked(client) || CombatLogoutLockService.isLocked(other)) {
            client.send(
                SendMessage(
                    if (CombatLogoutLockService.isLocked(client))
                        "You can't duel while in combat."
                    else "${other.getPlayerName()} can't duel while in combat."
                )
            )
            return
        }
        client.duelReq(pid)
    }

    // -------------------------------------------------------------------------
    // Legacy Trade Request (opcode 128) — uses duelReq internally
    // -------------------------------------------------------------------------

    /**
     * Processes the legacy trade-request packet (opcode 128) after the target
     * slot has been decoded and validated.
     *
     * If the player is holding a rubber chicken (item 4566 in weapon slot)
     * this performs the emote instead of requesting a trade/duel.
     * Guard messages and the [Client.duelReq] call live here rather than in the listener.
     */
    @JvmStatic
    fun handleLegacyTradeRequest(client: Client, targetSlot: Int, other: Client) {
        // Rubber-chicken emote
        if (client.getEquipment()[Equipment.Slot.WEAPON.id] == 4566) {
            client.facePlayer(targetSlot)
            client.performAnimation(1833, 0)
            return
        }
        if (client.isBusy() || other.isBusy()) {
            client.send(
                SendMessage(
                    if (client.isBusy()) "You are currently busy"
                    else "${other.getPlayerName()} is currently busy!"
                )
            )
            return
        }
        val guardMessage = PlayerInteractionGuardService.tradeBlockMessage(client, other)
        if (guardMessage != null) {
            client.send(SendMessage(guardMessage))
            return
        }
        if (!client.inTrade) {
            client.duelReq(targetSlot)
        }
    }

    // -------------------------------------------------------------------------
    // Command guard
    // -------------------------------------------------------------------------

    /**
     * Sends the "invalid client" rejection message when a command arrives from
     * a non-validated client. Returns true if the command should be rejected.
     */
    @JvmStatic
    fun rejectInvalidClientCommand(client: Client): Boolean {
        if (!client.validClient) {
            client.send(SendMessage("Command ignored, please use another client."))
            return true
        }
        return false
    }
}

