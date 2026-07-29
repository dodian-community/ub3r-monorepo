package net.dodian.uber.game.engine.systems.inventory

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.audit.ConsoleAuditLog

/** Core equipment boundary; mutations remain on the game thread and reservation-aware APIs. */
object EquipmentService {
    @JvmStatic
    fun wear(client: Client, itemId: Int, slot: Int, interfaceId: Int) {
        if (interfaceId != 3214 || slot !in client.playerItems.indices || client.playerItems[slot] != itemId + 1) {
            client.performLegacyWear(itemId, slot, interfaceId)
            return
        }
        val target = Server.itemManager.getSlot(itemId)
        if (client.isBusy || client.duelConfirmed && !client.duelFight || client.canUse(itemId) ||
            net.dodian.uber.game.social.exchange.DuelingService.isEquipmentRestricted(client, target) ||
            itemId in intArrayOf(5733, 6583, 7927, 4155) || target !in client.equipment.indices || requiresLegacyConflictHandling(client, itemId, target)) {
            client.performLegacyWear(itemId, slot, interfaceId)
            return
        }
        if (!client.checkEquip(itemId, target, slot)) return
        val amount = client.playerItemsN[slot]
        if (amount <= 0) return
        val existingId = client.equipment[target]
        val existingAmount = client.equipmentN[target]
        val finalAmount = if (existingId == itemId && Server.itemManager.isStackable(itemId)) existingAmount + amount else amount
        val committed = EconomyTransaction.run {
            inventory(client).removeAt(slot, itemId, amount)
            if (existingId > 0 && existingAmount > 0 && existingId != itemId) inventory(client).add(existingId, existingAmount)
            equipment(client).setAt(target, itemId, finalAmount)
        }
        if (!committed) {
            client.send(SendMessage("Not enough space to equip this item!"))
            return
        }
        client.wearing = false
        ConsoleAuditLog.equipmentWear(client, itemId, amount, slot, target, existingId.takeIf { existingAmount > 0 && it > 0 && it != itemId })
    }
    @JvmStatic fun unequip(client: Client, slot: Int, force: Boolean) = client.remove(slot, force)
    @JvmStatic fun refresh(client: Client) = client.refreshEquipmentState()

    /**
     * Stages an ordinary unequip so an inventory-capacity failure never clears
     * the worn item. Forced removals retain their legacy caller-owned handling.
     */
    @JvmStatic
    fun unequipToInventory(client: Client, slot: Int): Boolean = unequipToInventory(client, slot, false)

    @JvmStatic
    fun forceUnequipToInventory(client: Client, slot: Int): Boolean = unequipToInventory(client, slot, true)

    private fun unequipToInventory(client: Client, slot: Int, force: Boolean): Boolean {
        if (slot !in client.equipment.indices || client.equipment[slot] < 0 || client.equipmentN[slot] <= 0) return false
        if (client.duelConfirmed && !force) return false
        val itemId = client.equipment[slot]
        val amount = client.equipmentN[slot]
        val committed = EconomyTransaction.run {
            equipment(client).removeAt(slot, itemId, amount)
            inventory(client).add(itemId, amount)
        }
        if (!committed) client.send(SendMessage("Not enough space to unequip this item!"))
        else ConsoleAuditLog.equipmentRemove(client, itemId, amount, slot)
        return committed
    }

    private fun requiresLegacyConflictHandling(client: Client, itemId: Int, target: Int): Boolean {
        val weaponSlot = Equipment.Slot.WEAPON.id
        val shieldSlot = Equipment.Slot.SHIELD.id
        val weapon = client.equipment[weaponSlot]
        val shield = client.equipment[shieldSlot]
        val specialBow = { id: Int -> id == 4212 || id == 6724 || id == 4734 || id == 20997 }
        return (target == weaponSlot && shield > 0 && (Server.itemManager.isTwoHanded(itemId) || specialBow(itemId))) ||
            (target == shieldSlot && weapon > 0 && (Server.itemManager.isTwoHanded(weapon) || specialBow(weapon)))
    }
}
