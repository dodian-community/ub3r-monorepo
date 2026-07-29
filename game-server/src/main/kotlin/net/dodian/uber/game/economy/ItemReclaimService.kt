package net.dodian.uber.game.economy

import net.dodian.uber.game.Server
import net.dodian.uber.game.activity.partyroom.PartyRoomRewardItem
import net.dodian.uber.game.engine.systems.world.item.Ground
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.transaction.invAdd
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.audit.ItemLog
import net.dodian.uber.game.persistence.player.RefundRepository
import org.slf4j.LoggerFactory

/** Owns the runtime refund list and MySQL-backed claim lifecycle. */
object ItemReclaimService {
    private val logger = LoggerFactory.getLogger(ItemReclaimService::class.java)

    @JvmStatic
    fun open(client: Client) {
        if (!reload(client)) return
        client.refundSlot = 0
        showOptions(client)
    }

    private fun reload(client: Client): Boolean {
        client.rewardList.clear()
        client.refundDates.clear()
        return try {
            RefundRepository.loadUnclaimed(client.dbId).forEach { refund ->
                client.rewardList += PartyRoomRewardItem(refund.itemId, refund.amount)
                client.refundDates += refund.date
            }
            true
        } catch (exception: Exception) {
            logger.warn("Unable to load refunds for dbId={}", client.dbId, exception)
            client.send(SendMessage("Could not load your refunds right now."))
            false
        }
    }

    @JvmStatic
    fun showOptions(client: Client) {
        if (client.rewardList.isEmpty()) {
            client.refundSlot = -1
            client.send(SendMessage("You got no items to collect!"))
            return
        }
        val slot = client.refundSlot.coerceAtLeast(0)
        val size = client.rewardList.size
        val text = arrayOfNulls<String>(if (size < 4) size + 2 else if (size - slot <= 3) size - slot + 2 else 6)
        text[0] = "Refund Item List"
        val visible = minOf(3, size - slot)
        repeat(visible) { index ->
            val item = client.rewardList[slot + index]
            text[index + 1] = "Claim ${item.getAmount()} of ${client.getItemName(item.getId())}"
        }
        text[visible + 1] = if (text.size < 6 && slot == 0) "Close" else if (text.size == 6) "Next" else "Previous"
        if (text.size == 6) text[visible + 2] = if (slot == 0) "Close" else "Previous"
        client.showPlayerOption(text.requireNoNulls())
    }

    @JvmStatic
    fun claim(client: Client, position: Int) {
        val rowIndex = client.refundSlot + position - 1
        if (rowIndex !in client.rewardList.indices || rowIndex !in client.refundDates.indices) {
            client.sendMessage("That refund entry is no longer available.")
            reload(client)
            return
        }
        val item = client.rewardList[rowIndex]
        try {
            val delivered = if (Server.itemManager.isStackable(item.getId())) {
                if (client.freeSpace == 0) 0 else item.getAmount()
            } else minOf(item.getAmount(), client.freeSpace)
            if (delivered > 0 && !client.invAdd(item.getId(), delivered)) {
                client.send(SendMessage("Not enough space in your inventory!"))
                return
            }
            val dropped = item.getAmount() - delivered
            if (dropped > 0) {
                Ground.addFloorItem(client, item.getId(), dropped)
                client.send(SendMessage("Some items have been dropped to the ground!"))
                ItemLog.playerDrop(client, item.getId(), dropped, client.position.copy(), "Claim Items Dropped")
            }
            if (!RefundRepository.markClaimed(client.dbId, client.refundDates[rowIndex])) {
                logger.warn("Refund delivered to dbId={} but markClaimed failed for date={}", client.dbId, client.refundDates[rowIndex])
            }
            if (reload(client)) {
                if (client.rewardList.isEmpty()) client.send(RemoveInterfaces()) else client.refundSlot = 0
            }
        } catch (exception: Exception) {
            logger.warn("Unable to claim refund for dbId={}", client.dbId, exception)
            client.send(SendMessage("Could not claim that refund right now."))
        }
    }
}
