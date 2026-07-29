package net.dodian.uber.game.skill.runecrafting

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.runecrafting.RunecraftingModule
import org.slf4j.LoggerFactory

/**
 * Rune pouches (fill/empty/check) only: wired directly from raw item-click packet
 * listeners rather than the skill plugin registry, and key off Player-scoped pouch
 * arrays. Altar crafting itself lives in [RunecraftingModule].
 */
object Runecrafting {
    private val logger = LoggerFactory.getLogger(Runecrafting::class.java)

    @JvmStatic
    fun fillPouch(client: Client, pouchId: Int): Boolean {
        val slot = resolvePouchSlot(pouchId) ?: return false
        val pouch = client.runePouchState
        if (client.getLevel(Skill.RUNECRAFTING) < pouch.levelRequirement(slot)) {
            client.sendMessage("You need level ${pouch.levelRequirement(slot)} runecrafting to do this!")
            return true
        }
        if (pouch.amount(slot) >= pouch.capacity(slot)) {
            client.sendMessage("This pouch is currently full of essence!")
            return true
        }
        val now = System.currentTimeMillis()
        val lastAltarAt: Long = client.contentRuntimeState.getPluginAttribute(RunecraftingModule.LAST_ALTAR_CRAFT_KEY.id) ?: 0L
        if (now - lastAltarAt <= 1200L) {
            logger.warn("Rapid pouch fill after altar craft player={} pouchId={} deltaMs={}", client.playerName, pouchId, now - lastAltarAt)
        }
        val max = pouch.capacity(slot) - pouch.amount(slot)
        val amount = minOf(client.getInvAmt(RunecraftingModule.RUNE_ESSENCE_ID), max)
        if (amount > 0) {
            repeat(amount) {
                client.deleteItem(RunecraftingModule.RUNE_ESSENCE_ID, 1)
            }
            pouch.addAmount(slot, amount)
            client.checkItemUpdate()
        } else {
            client.sendMessage("No essence in your inventory!")
        }
        return true
    }

    @JvmStatic
    fun emptyPouch(client: Client, pouchId: Int): Boolean {
        val slot = resolvePouchSlot(pouchId) ?: return false
        val pouch = client.runePouchState
        if (client.getLevel(Skill.RUNECRAFTING) < pouch.levelRequirement(slot)) {
            client.sendMessage("You need level ${pouch.levelRequirement(slot)} runecrafting to do this!")
            return true
        }
        var amount = client.freeSlots()
        if (amount <= 0) {
            client.sendMessage("Not enough inventory slot to empty the pouch!")
            return true
        }
        amount = minOf(amount, pouch.amount(slot))
        if (amount > 0) {
            repeat(amount) {
                client.addItem(RunecraftingModule.RUNE_ESSENCE_ID, 1)
            }
            pouch.addAmount(slot, -amount)
            client.checkItemUpdate()
        } else {
            client.sendMessage("No essence in your pouch!")
        }
        return true
    }

    @JvmStatic
    fun checkPouch(client: Client, pouchId: Int): Boolean {
        val slot = resolvePouchSlot(pouchId) ?: return false
        client.sendMessage("There is ${client.runePouchState.amount(slot)} rune essence in this pouch!")
        return true
    }

    private fun resolvePouchSlot(pouchId: Int): Int? {
        val slot = if (pouchId == 5509) 0 else (pouchId - 5508) / 2
        return if (slot in 0..3) slot else null
    }
}
