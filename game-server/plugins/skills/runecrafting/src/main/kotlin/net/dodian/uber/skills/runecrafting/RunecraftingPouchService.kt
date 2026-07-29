package net.dodian.uber.skills.runecrafting

import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.game.model.player.skills.Skill

/** Plugin-owned fill, empty, and inspect behavior for the four rune pouches. */
object RunecraftingPouchService {
    fun fill(player: SkillPlayer, pouchId: Int): Boolean {
        val slot = slotFor(pouchId) ?: return false
        val pouch = player.runePouches
        if (!meetsLevel(player, slot)) return true
        if (pouch.amount(slot) >= pouch.capacity(slot)) {
            player.ui.message("This pouch is currently full of essence!")
            return true
        }
        val amount = minOf(player.inventory.amount(RunecraftingModule.RUNE_ESSENCE_ID), pouch.capacity(slot) - pouch.amount(slot))
        if (amount <= 0) {
            player.ui.message("No essence in your inventory!")
            return true
        }
        if (player.inventory.transaction { remove(RunecraftingModule.RUNE_ESSENCE_ID, amount) }) {
            pouch.addAmount(slot, amount)
            player.inventory.refresh()
        }
        return true
    }

    fun empty(player: SkillPlayer, pouchId: Int): Boolean {
        val slot = slotFor(pouchId) ?: return false
        val pouch = player.runePouches
        if (!meetsLevel(player, slot)) return true
        if (player.inventory.freeSlots() <= 0) {
            player.ui.message("Not enough inventory slot to empty the pouch!")
            return true
        }
        val amount = minOf(player.inventory.freeSlots(), pouch.amount(slot))
        if (amount <= 0) {
            player.ui.message("No essence in your pouch!")
            return true
        }
        if (player.inventory.transaction { add(RunecraftingModule.RUNE_ESSENCE_ID, amount) }) {
            pouch.addAmount(slot, -amount)
            player.inventory.refresh()
        }
        return true
    }

    fun check(player: SkillPlayer, pouchId: Int): Boolean {
        val slot = slotFor(pouchId) ?: return false
        player.ui.message("There is ${player.runePouches.amount(slot)} rune essence in this pouch!")
        return true
    }

    private fun meetsLevel(player: SkillPlayer, slot: Int): Boolean {
        val required = player.runePouches.levelRequirement(slot)
        if (player.skills.current(Skill.RUNECRAFTING) >= required) return true
        player.ui.message("You need level $required runecrafting to do this!")
        return false
    }

    private fun slotFor(pouchId: Int): Int? {
        val slot = if (pouchId == 5509) 0 else (pouchId - 5508) / 2
        return slot.takeIf { it in 0..3 }
    }
}
