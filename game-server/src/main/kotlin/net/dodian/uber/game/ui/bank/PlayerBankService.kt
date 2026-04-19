package net.dodian.uber.game.ui.bank

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.InventoryInterface
import net.dodian.uber.game.netty.listener.out.SendBankItems
import net.dodian.uber.game.netty.listener.out.SendCurrentBankTab
import net.dodian.uber.game.activity.partyroom.PartyRoomBalloons
import net.dodian.uber.game.persistence.audit.ConsoleAuditLog
import net.dodian.uber.game.persistence.player.PlayerSaveSegment
import net.dodian.uber.game.engine.systems.interaction.PlayerInteractionGuardService
import java.util.ArrayList
import java.util.Arrays

object PlayerBankService {
    @JvmStatic
    fun replaceBankContentsWithItemIds(client: Client, itemIds: List<Int>, amount: Int): Int? {
        val bankSize = client.bankSize()
        if (itemIds.size > bankSize) {
            return null
        }
        Arrays.fill(client.bankItems, 0)
        Arrays.fill(client.bankItemsN, 0)
        client.bankSlotTabs = IntArray(bankSize)
        client.bankContainerSlotMap = Array(11) { IntArray(bankSize) }
        client.currentBankTab = 0
        client.previousBankTab = 0
        client.bankSearchActive = false
        client.bankSearchPendingInput = false
        client.bankSearchQuery = ""
        clearBankStyleView(client)

        itemIds.forEachIndexed { index, itemId ->
            client.bankItems[index] = itemId + 1
            client.bankItemsN[index] = amount
        }

        client.markSaveDirty(PlayerSaveSegment.BANK.mask)
        checkItemUpdate(client)
        return itemIds.size
    }

    @JvmStatic
    fun openBankStyleView(client: Client, ids: ArrayList<Int>, amounts: ArrayList<Int>, title: String) {
        client.bankStyleViewIds = ArrayList(ids)
        client.bankStyleViewAmounts = ArrayList(amounts)
        client.bankStyleViewTitle = title
        client.bankStyleViewOpen = true
        client.IsBanking = false
        client.checkBankInterface = false
        client.bankSearchActive = false
        client.bankSearchPendingInput = false
        client.bankSearchQuery = ""
        client.currentBankTab = 0
        client.previousBankTab = 0
        sendBankStyleViewContainers(client)
        client.resetItems(5064)
        client.send(InventoryInterface(5292, 5063))
    }

    @JvmStatic
    fun clearBankStyleView(client: Client) {
        client.bankStyleViewOpen = false
        client.bankStyleViewIds.clear()
        client.bankStyleViewAmounts.clear()
        client.bankStyleViewTitle = ""
        client.bankStyleViewSlotMap = null
    }

    @JvmStatic
    fun moveBankItems(client: Client, from: Int, to: Int, moveWindow: Int): Boolean {
        if (moveWindow != 5382 && (moveWindow < 50300 || moveWindow > 50310)) {
            return false
        }
        if (client.bankStyleViewOpen) {
            return true
        }
        val actualFrom = resolveBankSlot(client, moveWindow, from)
        val actualTo = resolveBankSlot(client, moveWindow, to)
        if (actualFrom < 0 || actualTo < 0 || actualFrom >= client.bankSize() || actualTo >= client.bankSize()) {
            return true
        }
        ensureBankTabState(client)
        val tempItem = client.bankItems[actualFrom]
        val tempAmount = client.bankItemsN[actualFrom]
        val tempTab = client.bankSlotTabs[actualFrom]
        client.bankItems[actualFrom] = client.bankItems[actualTo]
        client.bankItemsN[actualFrom] = client.bankItemsN[actualTo]
        client.bankSlotTabs[actualFrom] = client.bankSlotTabs[actualTo]
        client.bankItems[actualTo] = tempItem
        client.bankItemsN[actualTo] = tempAmount
        client.bankSlotTabs[actualTo] = tempTab
        if (client.bankItems[actualFrom] <= 0 || client.bankItemsN[actualFrom] <= 0) {
            client.bankSlotTabs[actualFrom] = 0
        }
        if (client.bankItems[actualTo] <= 0 || client.bankItemsN[actualTo] <= 0) {
            client.bankSlotTabs[actualTo] = 0
        }
        client.markSaveDirty(PlayerSaveSegment.BANK.mask)
        checkItemUpdate(client)
        return true
    }

    @JvmStatic
    fun openUpBank(client: Client) {
        if (!Server.banking) {
            client.sendMessage("Banking have been disabled!")
            return
        }
        if (!PlayerInteractionGuardService.canOpenBank(client)) {
            PlayerInteractionGuardService.blockingInteractionMessage(client)?.let {
                client.sendMessage(it)
            }
            return
        }
        client.resetAction(true)
        client.sendString("Withdraw as:", 5388)
        client.sendString("Note", 5389)
        client.sendString("Item", 5391)
        client.sendString("Bank of ${client.playerName}", 5383)
        ensureBankTabState(client)
        client.currentBankTab = 0
        client.previousBankTab = 0
        client.bankSearchActive = false
        client.bankSearchPendingInput = false
        client.bankSearchQuery = ""
        client.IsBanking = true
        client.checkBankInterface = false
        clearBankStyleView(client)
        checkItemUpdate(client)
    }

    @JvmStatic
    fun checkItemUpdate(client: Client) {
        when {
            client.isShopping -> {
                client.resetShop(client.MyShopID)
                client.resetItems(3823)
            }

            client.IsBanking || client.checkBankInterface -> {
                client.resetBank()
                if (client.IsBanking) {
                    refreshBankHeader(client)
                    client.send(SendCurrentBankTab(client.currentBankTab))
                }
                client.resetItems(5064)
                client.send(InventoryInterface(5292, 5063))
            }

            client.bankStyleViewOpen -> {
                sendBankStyleViewContainers(client)
                client.resetItems(5064)
                client.send(InventoryInterface(5292, 5063))
            }

            client.isPartyInterface -> {
                PartyRoomBalloons.displayDepositedItems(client)
                client.resetItems(5064)
                client.send(InventoryInterface(2156, 5063))
            }

            client.inTrade || client.inDuel -> client.resetItems(3322)
        }
        client.resetItems(3214)
    }

    @JvmStatic
    fun applyBankSearch(client: Client, query: String?) {
        if (!client.IsBanking || client.bankStyleViewOpen) {
            return
        }
        val normalized = query?.trim()?.lowercase() ?: ""
        if (normalized.isEmpty()) {
            client.bankSearchActive = false
            client.bankSearchQuery = ""
            client.currentBankTab = clampNormalTab(client.previousBankTab)
            checkItemUpdate(client)
            return
        }
        ensureBankTabState(client)
        client.previousBankTab = clampNormalTab(if (client.currentBankTab in 0..9) client.currentBankTab else 0)
        client.bankSearchActive = true
        client.bankSearchQuery = normalized
        client.currentBankTab = 10
        checkItemUpdate(client)
    }

    @JvmStatic
    fun ensureBankTabState(client: Client) {
        val size = client.bankSize()
        if (client.bankSlotTabs == null || client.bankSlotTabs.size != size) {
            client.bankSlotTabs = IntArray(size)
        }
        if (client.bankContainerSlotMap == null ||
            client.bankContainerSlotMap.size != 11 ||
            client.bankContainerSlotMap[0].size != size
        ) {
            client.bankContainerSlotMap = Array(11) { IntArray(size) }
        }
        var index = 0
        while (index < size) {
            if (client.bankItems[index] <= 0 || client.bankItemsN[index] <= 0) {
                client.bankSlotTabs[index] = 0
            } else if (client.bankSlotTabs[index] < 0 || client.bankSlotTabs[index] > 9) {
                client.bankSlotTabs[index] = 0
            }
            index++
        }
    }

    @JvmStatic
    fun sendBankContainers(client: Client) {
        rebuildBankContainers(client)
        val size = client.bankSize()
        var tab = 0
        while (tab < 11) {
            val ids = ArrayList<Int>(size)
            val amounts = ArrayList<Int>(size)
            var localSlot = 0
            while (localSlot < size) {
                val globalSlot = client.bankContainerSlotMap[tab][localSlot]
                if (globalSlot >= 0) {
                    ids.add(client.bankItems[globalSlot] - 1)
                    amounts.add(client.bankItemsN[globalSlot])
                } else {
                    ids.add(0)
                    amounts.add(0)
                }
                localSlot++
            }
            client.send(SendBankItems(ids, amounts, 50300 + tab))
            tab++
        }
    }

    @JvmStatic
    fun sendBankStyleViewContainers(client: Client) {
        rebuildBankStyleViewContainers(client)
        val size = client.bankSize()
        client.sendString(client.bankStyleViewTitle, 5383)
        client.send(SendCurrentBankTab(0))
        var tab = 0
        while (tab < 11) {
            val ids = ArrayList<Int>(size)
            val amounts = ArrayList<Int>(size)
            var localSlot = 0
            while (localSlot < size) {
                val viewSlot = client.bankStyleViewSlotMap[tab][localSlot]
                if (viewSlot >= 0) {
                    ids.add(client.bankStyleViewIds[viewSlot])
                    amounts.add(client.bankStyleViewAmounts[viewSlot])
                } else {
                    ids.add(0)
                    amounts.add(0)
                }
                localSlot++
            }
            client.send(SendBankItems(ids, amounts, 50300 + tab))
            tab++
        }
    }

    @JvmStatic
    fun resolveBankSlot(client: Client, interfaceId: Int, containerSlot: Int): Int {
        if (containerSlot < 0) {
            return -1
        }
        if (client.bankStyleViewOpen && interfaceId in 50300..50310) {
            return -1
        }
        if (interfaceId == 5382) {
            return if (containerSlot < client.bankSize()) containerSlot else -1
        }
        if (interfaceId !in 50300..50310) {
            return containerSlot
        }
        rebuildBankContainers(client)
        val tab = interfaceId - 50300
        return if (containerSlot < client.bankContainerSlotMap[tab].size) {
            client.bankContainerSlotMap[tab][containerSlot]
        } else {
            -1
        }
    }

    @JvmStatic
    fun resolveBankItemId(client: Client, interfaceId: Int, containerSlot: Int, fallbackItemId: Int): Int {
        val bankSlot = resolveBankSlot(client, interfaceId, containerSlot)
        return if (bankSlot >= 0 && bankSlot < client.bankSize() && client.bankItems[bankSlot] > 0) {
            client.bankItems[bankSlot] - 1
        } else {
            fallbackItemId
        }
    }

    @JvmStatic
    fun assignBankSlotToTab(client: Client, bankSlot: Int, tab: Int) {
        if (client.bankStyleViewOpen) {
            return
        }
        ensureBankTabState(client)
        if (bankSlot < 0 || bankSlot >= client.bankSize()) {
            return
        }
        if (client.bankItems[bankSlot] <= 0 || client.bankItemsN[bankSlot] <= 0) {
            return
        }
        val itemId = client.bankItems[bankSlot] - 1
        val currentTab = client.bankSlotTabs[bankSlot]
        if (itemId == 995 && currentTab in 1..9 && tab in 1..9 && currentTab != tab && !hasBankTabItems(client, tab)) {
            return
        }
        val targetTab = clampOwnedTab(tab)
        client.bankSlotTabs[bankSlot] = targetTab
        if (client.currentBankTab == 10) {
            client.bankSearchActive = false
            client.bankSearchQuery = ""
            client.currentBankTab = 0
        }
        client.markSaveDirty(PlayerSaveSegment.BANK.mask)
        ConsoleAuditLog.bankTabAssignment(client, itemId, bankSlot, currentTab, targetTab)
        checkItemUpdate(client)
    }

    @JvmStatic
    fun selectBankTab(client: Client, tab: Int) {
        if (client.bankStyleViewOpen) {
            return
        }
        ensureBankTabState(client)
        if (tab in 1..9 && !hasBankTabItems(client, tab)) {
            client.sendMessage("To create a new tab, drag an item onto this tab.")
            return
        }
        if (tab != 10 && client.bankSearchActive) {
            client.bankSearchActive = false
            client.bankSearchQuery = ""
        }
        client.currentBankTab = clampUiTab(tab)
        if (client.currentBankTab in 0..9) {
            client.previousBankTab = client.currentBankTab
        }
        checkItemUpdate(client)
    }

    @JvmStatic
    fun collapseBankTab(client: Client, tab: Int) {
        if (client.bankStyleViewOpen) {
            return
        }
        ensureBankTabState(client)
        if (tab <= 0 || tab > 9) {
            return
        }
        var index = 0
        val size = client.bankSize()
        while (index < size) {
            when {
                client.bankSlotTabs[index] == tab -> client.bankSlotTabs[index] = 0
                client.bankSlotTabs[index] > tab -> client.bankSlotTabs[index]--
            }
            index++
        }
        when {
            client.currentBankTab == tab || client.currentBankTab > 9 -> client.currentBankTab = 0
            client.currentBankTab > tab -> client.currentBankTab--
        }
        when {
            client.previousBankTab == tab || client.previousBankTab > 9 -> client.previousBankTab = 0
            client.previousBankTab > tab -> client.previousBankTab--
        }
        client.bankSearchActive = false
        client.bankSearchQuery = ""
        checkItemUpdate(client)
    }

    @JvmStatic
    fun clearBankSearch(client: Client) {
        if (client.bankStyleViewOpen) {
            return
        }
        client.bankSearchActive = false
        client.bankSearchPendingInput = false
        client.bankSearchQuery = ""
        client.currentBankTab = clampNormalTab(client.previousBankTab)
        checkItemUpdate(client)
    }

    @JvmStatic
    fun hasBankTabItems(client: Client, tab: Int): Boolean {
        ensureBankTabState(client)
        if (tab <= 0 || tab > 9) {
            return tab == 0
        }
        var index = 0
        val size = client.bankSize()
        while (index < size) {
            if (client.bankItems[index] > 0 && client.bankItemsN[index] > 0 && client.bankSlotTabs[index] == tab) {
                return true
            }
            index++
        }
        return false
    }

    @JvmStatic
    fun refreshBankHeader(client: Client) {
        var used = 0
        var index = 0
        val size = client.bankSize()
        while (index < size) {
            if (client.bankItems[index] > 0 && client.bankItemsN[index] > 0) {
                used++
            }
            index++
        }
        client.sendString(used.toString(), 50053)
        client.sendString(size.toString(), 50055)
    }

    private fun rebuildBankContainers(client: Client) {
        ensureBankTabState(client)
        var tab = 0
        while (tab < client.bankContainerSlotMap.size) {
            Arrays.fill(client.bankContainerSlotMap[tab], -1)
            tab++
        }
        val counts = IntArray(11)
        val size = client.bankSize()
        var slot = 0
        while (slot < size) {
            if (client.bankItems[slot] <= 0 || client.bankItemsN[slot] <= 0) {
                client.bankSlotTabs[slot] = 0
                slot++
                continue
            }
            var ownerTab = client.bankSlotTabs[slot]
            if (ownerTab < 0 || ownerTab > 9) {
                ownerTab = 0
                client.bankSlotTabs[slot] = 0
            }
            val tabIndex = counts[ownerTab]++
            client.bankContainerSlotMap[ownerTab][tabIndex] = slot
            if (client.bankSearchActive && bankMatchesSearch(client, slot)) {
                val searchIndex = counts[10]++
                client.bankContainerSlotMap[10][searchIndex] = slot
            }
            slot++
        }
    }

    private fun bankMatchesSearch(client: Client, slot: Int): Boolean {
        if (!client.bankSearchActive || client.bankSearchQuery.isEmpty()) {
            return false
        }
        if (slot < 0 || slot >= client.bankSize() || client.bankItems[slot] <= 0 || client.bankItemsN[slot] <= 0) {
            return false
        }
        val itemName = client.getItemName(client.bankItems[slot] - 1) ?: return false
        return itemName.lowercase().contains(client.bankSearchQuery)
    }

    private fun rebuildBankStyleViewContainers(client: Client) {
        val size = client.bankSize()
        if (client.bankStyleViewSlotMap == null ||
            client.bankStyleViewSlotMap.size != 11 ||
            client.bankStyleViewSlotMap[0].size != size
        ) {
            client.bankStyleViewSlotMap = Array(11) { IntArray(size) }
        }
        var tab = 0
        while (tab < client.bankStyleViewSlotMap.size) {
            Arrays.fill(client.bankStyleViewSlotMap[tab], -1)
            tab++
        }
        val previewSize = minOf(size, client.bankStyleViewIds.size, client.bankStyleViewAmounts.size)
        var slot = 0
        while (slot < previewSize) {
            client.bankStyleViewSlotMap[0][slot] = slot
            slot++
        }
    }

    private fun clampOwnedTab(tab: Int): Int {
        return when {
            tab < 0 -> 0
            tab > 9 -> 9
            else -> tab
        }
    }

    private fun clampNormalTab(tab: Int): Int {
        return when {
            tab < 0 -> 0
            tab > 9 -> 9
            else -> tab
        }
    }

    private fun clampUiTab(tab: Int): Int {
        return when {
            tab < 0 -> 0
            tab > 10 -> 10
            else -> tab
        }
    }
}
