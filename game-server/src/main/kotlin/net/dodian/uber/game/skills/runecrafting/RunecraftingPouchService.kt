package net.dodian.uber.game.skills.runecrafting

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import org.slf4j.LoggerFactory

object RunecraftingPouchService {
    private val logger = LoggerFactory.getLogger(RunecraftingPouchService::class.java)

    @JvmStatic
    fun fill(client: Client, pouchId: Int): Boolean {
        val slot = resolvePouchSlot(pouchId) ?: return false
        if (client.getLevel(Skill.RUNECRAFTING) < client.runePouchesLevel[slot]) {
            client.send(SendMessage("You need level ${client.runePouchesLevel[slot]} runecrafting to do this!"))
            return true
        }
        if (client.runePouchesAmount[slot] >= client.runePouchesMaxAmount[slot]) {
            client.send(SendMessage("This pouch is currently full of essence!"))
            return true
        }

        val now = System.currentTimeMillis()
        val lastAltarAt = client.runecraftingState?.lastAltarCraftAtMillis ?: 0L
        if (now - lastAltarAt <= 1200L) {
            logger.warn("Rapid pouch fill after altar craft player={} pouchId={} deltaMs={}", client.playerName, pouchId, now - lastAltarAt)
        }

        val max = client.runePouchesMaxAmount[slot] - client.runePouchesAmount[slot]
        val amount = minOf(client.getInvAmt(RunecraftingDefinitions.RUNE_ESSENCE_ID), max)
        if (amount > 0) {
            repeat(amount) {
                client.deleteItem(RunecraftingDefinitions.RUNE_ESSENCE_ID, 1)
            }
            client.runePouchesAmount[slot] += amount
            client.checkItemUpdate()
        } else {
            client.send(SendMessage("No essence in your inventory!"))
        }
        return true
    }

    @JvmStatic
    fun empty(client: Client, pouchId: Int): Boolean {
        val slot = resolvePouchSlot(pouchId) ?: return false
        if (client.getLevel(Skill.RUNECRAFTING) < client.runePouchesLevel[slot]) {
            client.send(SendMessage("You need level ${client.runePouchesLevel[slot]} runecrafting to do this!"))
            return true
        }
        var amount = client.freeSlots()
        if (amount <= 0) {
            client.send(SendMessage("Not enough inventory slot to empty the pouch!"))
            return true
        }
        amount = minOf(amount, client.runePouchesAmount[slot])
        if (amount > 0) {
            repeat(amount) {
                client.addItem(RunecraftingDefinitions.RUNE_ESSENCE_ID, 1)
            }
            client.runePouchesAmount[slot] -= amount
            client.checkItemUpdate()
        } else {
            client.send(SendMessage("No essence in your pouch!"))
        }
        return true
    }

    @JvmStatic
    fun check(client: Client, pouchId: Int): Boolean {
        val slot = resolvePouchSlot(pouchId) ?: return false
        client.send(SendMessage("There is ${client.runePouchesAmount[slot]} rune essence in this pouch!"))
        return true
    }

    private fun resolvePouchSlot(pouchId: Int): Int? {
        val slot = if (pouchId == 5509) 0 else (pouchId - 5508) / 2
        return if (slot in 0..3) slot else null
    }
}
