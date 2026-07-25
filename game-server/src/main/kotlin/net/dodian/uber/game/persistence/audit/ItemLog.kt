package net.dodian.uber.game.persistence.audit

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Player

object ItemLog {
    @JvmStatic
    fun playerPickup(player: Player, userId: Int, itemId: Int, itemAmount: Int, pos: Position, npc: Boolean) {
        ConsoleAuditLog.itemPickup(player, userId, itemId, itemAmount, pos, npc)
    }

    @JvmStatic
    @JvmOverloads
    fun playerDrop(
        player: Player,
        itemId: Int,
        itemAmount: Int,
        pos: Position,
        reason: String,
        invSlot: Int = -1,
        interfaceId: Int = 3214
    ) {
        val dropReason = if (reason.isEmpty()) "player" else reason
        ConsoleAuditLog.itemDrop(player, itemId, itemAmount, pos, dropReason, invSlot, interfaceId)
    }

    @JvmStatic
    fun npcDrop(player: Player, npcId: Int, itemId: Int, itemAmount: Int, pos: Position) {
        ConsoleAuditLog.npcDrop(player, npcId, itemId, itemAmount, pos)
    }

    @JvmStatic
    fun playerGathering(player: Player, itemId: Int, itemAmount: Int, pos: Position, reason: String) {
        ConsoleAuditLog.itemGathering(player, itemId, itemAmount, pos, reason)
    }
}