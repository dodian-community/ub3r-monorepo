package net.dodian.uber.game.systems.net

import net.dodian.uber.game.Constants
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.item.ItemClickEvent
import net.dodian.uber.game.events.item.ItemOnPlayerEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.systems.interaction.items.ItemDispatcher
import net.dodian.uber.game.systems.world.player.PlayerRegistry
import net.dodian.utilities.Misc

/**
 * Kotlin service for item-interaction packet side-effects that must be kept
 * out of the Netty listener layer.
 *
 * Covers:
 * - WearItem (opcode 41)
 * - ClickItem2 / ClickItem3 / UseItemOnNpc validation disconnects
 * - UseItemOnPlayer / Christmas cracker (opcode 14)
 */
object PacketItemActionService {

    /**
     * Equips an item from the inventory (opcode 41).
     * Mirrors the guard + [Client.wear] call previously in WearItemListener.
     */
    @JvmStatic
    fun handleWear(client: Client, wearId: Int, wearSlot: Int, interfaceId: Int) {
        if (client.randomed || client.UsingAgility) return
        client.wear(wearId, wearSlot, interfaceId)
    }

    /**
     * Validates that [slot] is within the legal inventory range (0–28).
     * If out of bounds the client is disconnected.
     *
     * @return true when the slot is valid and the caller may proceed.
     */
    @JvmStatic
    fun validateInventorySlot(client: Client, slot: Int): Boolean {
        if (slot < 0 || slot > 28) {
            client.disconnected = true
            return false
        }
        return true
    }

    /**
     * Validates that [slot] is within the legal range for use-item-on-NPC (0–27).
     * If out of bounds the client is disconnected.
     *
     * @return true when the slot is valid and the caller may proceed.
     */
    @JvmStatic
    fun validateItemOnNpcSlot(client: Client, slot: Int): Boolean {
        if (slot < 0 || slot > 27) {
            client.disconnected = true
            return false
        }
        return true
    }

    /**
     * Processes a first-click item action (opcode 122) after slot validation.
     * Enforces duel-rules guards and dispatches via GameEventBus / ItemDispatcher.
     */
    @JvmStatic
    fun handleFirstClickItem(client: Client, id: Int, slot: Int, interfaceId: Int) {
        val item = client.playerItems[slot] - 1
        if (item != id) return
        val bypassDuelGuards = item == 4155 || item == 2528 || item == 6543 || item == 5733
        if (!bypassDuelGuards && client.duelRule[7] && client.inDuel && client.duelFight) {
            client.send(SendMessage("Food has been disabled for this duel"))
            return
        }
        if (!bypassDuelGuards && (client.inDuel || client.duelFight || client.duelConfirmed || client.duelConfirmed2)) {
            if (item != 4155) {
                client.send(SendMessage("This item cannot be used in a duel!"))
                return
            }
        }
        if (GameEventBus.postWithResult(ItemClickEvent(client, item, slot, interfaceId))) {
            client.checkItemUpdate()
            return
        }
        if (ItemDispatcher.tryHandle(client, 1, item, slot, interfaceId)) {
            client.checkItemUpdate()
        }
    }

    /**
     * Handles a Use-Item-On-Player action (opcode 14).
     * Currently supports the Christmas cracker (item 962) and the hot-potato
     * internal mini-game (item 5733).
     */
    @JvmStatic
    fun handleUseItemOnPlayer(client: Client, playerSlot: Int, itemId: Int, crackerSlot: Int) {
        val target: Client? = if (playerSlot >= 0 && playerSlot < Constants.maxPlayers)
            PlayerRegistry.players[playerSlot] as? Client
        else null
        if (target == null || !client.playerHasItem(itemId)) return
        if (client.randomed || client.UsingAgility) return
        GameEventBus.post(ItemOnPlayerEvent(client, target, itemId, crackerSlot))

        if (itemId == 5733) { // hot-potato mini-game setup
            client.playerPotato.clear()
            client.playerPotato.add(0, 1)
            client.playerPotato.add(1, playerSlot)
            client.playerPotato.add(2, target.dbId)
            client.playerPotato.add(3, 1)
            return
        }

        if (itemId == 962) { // Christmas cracker
            if (target.freeSlots() <= 0) {
                client.send(SendMessage("Your partner need a slot free in their inventory!"))
                return
            }
            if (client.connectedFrom == target.connectedFrom) {
                client.send(SendMessage("Can't use it on another player from same address!"))
                return
            }
            client.deleteItem(itemId, crackerSlot, 1)
            val hats = intArrayOf(1038, 1040, 1042, 1044, 1046, 1048)
            val partyHat = hats[Misc.random(hats.size - 1)]
            if (Misc.random(99) < 50) {
                client.addItemSlot(partyHat, 1, crackerSlot)
                client.send(SendMessage("You got a ${client.getItemName(partyHat).lowercase()} from the cracker!"))
            } else {
                target.addItem(partyHat, 1)
                client.checkItemUpdate()
                target.send(SendMessage("You got a ${client.getItemName(partyHat).lowercase()} from ${client.getPlayerName()}"))
                client.send(SendMessage("${target.getPlayerName()} got a  ${client.getItemName(partyHat).lowercase()} from the cracker!"))
            }
        }
    }
}
