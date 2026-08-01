package net.dodian.uber.game.ui.bank

import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.config.FeatureStateService
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.InventoryInterface
import net.dodian.uber.game.netty.listener.out.SendBankItems
import net.dodian.uber.game.netty.listener.out.SetVarbit
import net.dodian.uber.game.netty.listener.out.SendTooltip
import net.dodian.uber.game.activity.partyroom.PartyRoomBalloons
import net.dodian.uber.game.persistence.audit.ConsoleAuditLog
import net.dodian.uber.game.persistence.player.PlayerSaveSegment
import net.dodian.uber.game.engine.systems.interaction.PlayerInteractionGuardService
import net.dodian.uber.game.model.item.transaction.BankTransactions
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import java.util.ArrayList
import java.util.Arrays

object PlayerBankService {
    private const val INSERT_MODE_VARP = 304
    private const val NOTE_MODE_VARP = 115
    private const val PLACEHOLDER_MODE_VARP = 116
    private const val INSERT_MODE_BUTTON = 60006
    private const val NOTE_MODE_BUTTON = 60007
    private const val PLACEHOLDER_MODE_BUTTON = 60073

    @JvmStatic fun withdraw(client: Client, itemId: Int, slot: Int, requested: Int) {
        if (!client.IsBanking) { client.send(RemoveInterfaces()); return }
        if (slot !in client.bankItems.indices || client.bankItems[slot] - 1 != itemId) return
        if (client.bankItemsN[slot] == 0) { releasePlaceholder(client, slot); return }
        val noted = client.getNotedItem(itemId)
        if (client.takeAsNote && noted <= 0) client.send(SendMessage("${Server.itemManager.getName(itemId)} can't be drawn as note."))
        var amount = requested
        if (amount == -2) amount = if (!client.takeAsNote && !Server.itemManager.isStackable(itemId)) minOf(client.bankItemsN[slot], client.freeSlots()) else client.bankItemsN[slot]
        amount = minOf(amount, client.bankItemsN[slot]); if (amount <= 0) return
        val received = if (client.takeAsNote && noted > 0) noted else itemId
        if (!BankTransactions.withdraw(client, itemId, slot, amount, received, client.bankPlaceholdersEnabled)) client.send(SendMessage("Not enough space in your inventory."))
    }

    @JvmStatic fun deposit(client: Client, itemId: Int, slot: Int, requested: Int) {
        if (!client.IsBanking || slot !in client.playerItems.indices || client.playerItems[slot] != itemId + 1) return
        ensureBankTabState(client)
        val bankItemId = client.getUnnotedItem(itemId).takeIf { it > 0 } ?: itemId
        val available = if (Server.itemManager.isStackable(itemId)) client.playerItemsN[slot] else client.getInvAmt(itemId)
        val amount = requested.coerceAtLeast(0).coerceAtMost(available)
        if (amount == 0) return
        val newEntry = client.getBankSlot(bankItemId) < 0
        if (!BankTransactions.deposit(client, itemId, slot, amount, bankItemId)) { client.send(SendMessage("Bank full!")); return }
        if (newEntry) client.getBankSlot(bankItemId).takeIf { it >= 0 }?.let { bankSlot ->
            client.bankSlotTabs[bankSlot] = if (client.currentBankTab in 1..9 && !client.bankSearchActive) client.currentBankTab else 0
        }
    }

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
        client.contentRuntimeState.clearPendingInputState()
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
        client.contentRuntimeState.clearPendingInputState()
        client.bankSearchQuery = ""
        client.currentBankTab = 0
        client.previousBankTab = 0
        sendBankStyleViewContainers(client)
        client.resetItems(5064)
        client.pendingInterfaceOpenContext = "bank-style read-only view: \"$title\""
        client.send(InventoryInterface(60000, 5063))
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
    fun moveBankItems(client: Client, from: Int, to: Int, moveWindow: Int, mode: Int = 0): Boolean {
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
        if (mode == 1) {
            insertBankItem(client, actualFrom, actualTo)
        } else {
            val tempItem = client.bankItems[actualFrom]
            val tempAmount = client.bankItemsN[actualFrom]
            val tempTab = client.bankSlotTabs[actualFrom]
            client.bankItems[actualFrom] = client.bankItems[actualTo]
            client.bankItemsN[actualFrom] = client.bankItemsN[actualTo]
            client.bankSlotTabs[actualFrom] = client.bankSlotTabs[actualTo]
            client.bankItems[actualTo] = tempItem
            client.bankItemsN[actualTo] = tempAmount
            client.bankSlotTabs[actualTo] = tempTab
        }
        if (client.bankItems[actualFrom] <= 0) {
            client.bankSlotTabs[actualFrom] = 0
        }
        if (client.bankItems[actualTo] <= 0) {
            client.bankSlotTabs[actualTo] = 0
        }
        client.markSaveDirty(PlayerSaveSegment.BANK.mask)
        checkItemUpdate(client)
        return true
    }

    @JvmStatic
    fun openUpBank(client: Client) {
        if (!FeatureStateService.banking.get()) {
            client.sendMessage("Banking have been disabled!")
            return
        }
        // A bank interaction is an explicit modal transition.  The legacy client
        // often closes the visible widget before its dialogue/session bookkeeping
        // has caught up, so make the server state authoritative here as well.
        if (PlayerInteractionGuardService.isDuelLocked(client)) {
            client.sendMessage("You can't do that while in a duel.")
            return
        }
        if (PlayerInteractionGuardService.hasBlockingInterface(client) || PlayerInteractionGuardService.hasActiveDialogue(client)) {
            client.closeInterfaces()
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
        client.contentRuntimeState.clearPendingInputState()
        client.bankSearchQuery = ""
        client.IsBanking = true
        client.checkBankInterface = false
        client.pendingInterfaceOpenContext = "player bank (PlayerBankService.openUpBank)"
        clearBankStyleView(client)
        // Reset the client-side search field to its placeholder so a previous search doesn't persist.
        client.sendString("Search...", 60019)
        refreshBankPreferences(client)
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
                    client.send(SetVarbit(211, client.currentBankTab))
                }
                client.resetItems(5064)
                client.send(InventoryInterface(60000, 5063))
            }

            client.bankStyleViewOpen -> {
                sendBankStyleViewContainers(client)
                client.resetItems(5064)
                client.send(InventoryInterface(60000, 5063))
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
        // Keep currentBankTab=0 so the client gets SetVarbit(211,0) → settings[211]=0 →
        // newSlot=0, and bankInvTemp[0..N-1] renders the search results at the right position.
        // (Using tab 10 caused settings[211]=10 → newSlot=total items → wrong slot resolution.)
        client.currentBankTab = 0
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
            if (client.bankItems[index] <= 0 || client.bankItemsN[index] < 0) {
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
        val ids = ArrayList<Int>()
        val amounts = ArrayList<Int>()
        val tabAmounts = IntArray(10)

        if (client.bankSearchActive) {
            var count = 0
            var localSlot = 0
            while (localSlot < size) {
                val globalSlot = client.bankContainerSlotMap[10][localSlot]
                if (globalSlot >= 0) {
                    ids.add(client.bankItems[globalSlot] - 1)
                    amounts.add(client.bankItemsN[globalSlot])
                    count++
                }
                localSlot++
            }
            tabAmounts[0] = count
        } else {
            var tab = 0
            while (tab < 10) {
                var count = 0
                var localSlot = 0
                while (localSlot < size) {
                    val globalSlot = client.bankContainerSlotMap[tab][localSlot]
                    if (globalSlot >= 0) {
                        ids.add(client.bankItems[globalSlot] - 1)
                        amounts.add(client.bankItemsN[globalSlot])
                        count++
                    }
                    localSlot++
                }
                tabAmounts[tab] = count
                tab++
            }
        }
        client.send(SendBankItems(ids, amounts, 5382, tabAmounts))
    }

    @JvmStatic
    fun sendBankStyleViewContainers(client: Client) {
        rebuildBankStyleViewContainers(client)
        val size = client.bankSize()
        client.sendString(client.bankStyleViewTitle, 5383)
        client.send(SetVarbit(211, 0))
        
        val ids = ArrayList<Int>()
        val amounts = ArrayList<Int>()
        val tabAmounts = IntArray(10)
        
        var count = 0
        var localSlot = 0
        while (localSlot < size) {
            val viewSlot = client.bankStyleViewSlotMap[0][localSlot]
            if (viewSlot >= 0) {
                ids.add(client.bankStyleViewIds[viewSlot])
                amounts.add(client.bankStyleViewAmounts[viewSlot])
                count++
            }
            localSlot++
        }
        tabAmounts[0] = count
        
        client.send(SendBankItems(ids, amounts, 5382, tabAmounts))
    }

    @JvmStatic
    fun resolveBankSlot(client: Client, interfaceId: Int, containerSlot: Int): Int {
        if (containerSlot < 0) {
            return -1
        }
        if (client.bankStyleViewOpen && (interfaceId == 5382 || interfaceId in 50300..50310)) {
            return -1
        }
        if (interfaceId == 5382) {
            rebuildBankContainers(client)
            val size = client.bankSize()
            if (client.bankSearchActive) {
                var currentVisualSlot = 0
                var localSlot = 0
                while (localSlot < size) {
                    val globalSlot = client.bankContainerSlotMap[10][localSlot]
                    if (globalSlot >= 0) {
                        if (currentVisualSlot == containerSlot) {
                            return globalSlot
                        }
                        currentVisualSlot++
                    }
                    localSlot++
                }
            } else {
                var currentVisualSlot = 0
                var tab = 0
                while (tab < 10) {
                    var localSlot = 0
                    while (localSlot < size) {
                        val globalSlot = client.bankContainerSlotMap[tab][localSlot]
                        if (globalSlot >= 0) {
                            if (currentVisualSlot == containerSlot) {
                                return globalSlot
                            }
                            currentVisualSlot++
                        }
                        localSlot++
                    }
                    tab++
                }
            }
            return -1
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

    /**
     * Finds the physical bank slot holding [itemId], ignoring any display/container slot.
     *
     * Bank items are unique (each item id occupies at most one bank slot), so the id alone
     * unambiguously identifies the slot. This mirrors Tarnish's `withdraw()` which recomputes
     * the slot via `computeIndexForId(itemId)` rather than trusting the slot the client sent —
     * it is the key to correct withdrawals while searching (the client sends a filtered display
     * index, but the item id it sends is always the real one shown in the search results).
     *
     * Returns -1 if the item is not present in the bank.
     */
    @JvmStatic
    fun resolveBankSlotByItemId(client: Client, itemId: Int): Int {
        if (itemId < 0) return -1
        val size = client.bankSize()
        var i = 0
        while (i < size) {
            if (client.bankItems[i] - 1 == itemId && client.bankItemsN[i] >= 0) {
                return i
            }
            i++
        }
        return -1
    }

    /**
     * Compacts bank tab numbers so that occupied tabs are always 0, 1, 2, … with no gaps.
     *
     * This is the ub3r equivalent of Tarnish's collapse/shift: when the last item leaves a tab,
     * the higher tabs slide left to fill the gap. Crucially it also pulls the lowest occupied
     * tab down to **tab 0** — the Tarnish bank client's tab renderer assumes `tabAmounts[0]` is
     * never 0 while the bank holds items (it computes `itemSlot = tabAm - tabAmounts[i]`, which
     * goes negative and throws if tab 0 is empty), producing the disappearing-icon / gap bugs.
     * Keeping the lowest group in tab 0 mirrors Tarnish, where emptying the main tab pulls the
     * next tab down into it.
     *
     * ub3r tags each slot with its tab rather than physically reordering, so this runs at render
     * time (it is idempotent) to keep the numbering canonical regardless of which operation
     * changed it, and remaps the current/previous tab pointers to match.
     */
    @JvmStatic
    fun normalizeBankTabs(client: Client) {
        val size = client.bankSize()
        val tabs = client.bankSlotTabs ?: return
        val occupied = BooleanArray(10)
        var slot = 0
        while (slot < size) {
            if (client.bankItems[slot] > 0 && client.bankItemsN[slot] >= 0) {
                val t = tabs[slot]
                if (t in 0..9) occupied[t] = true
            }
            slot++
        }
        // Assign sequential numbers (0,1,2,…) to occupied tabs in ascending order. The lowest
        // occupied tab becomes tab 0 so the "view all" group is always populated.
        val remap = IntArray(10) { -1 }
        var next = 0
        for (t in 0..9) {
            if (occupied[t]) {
                remap[t] = next++
            }
        }
        var changed = false
        for (t in 0..9) {
            if (occupied[t] && remap[t] != t) {
                changed = true
                break
            }
        }
        if (changed) {
            slot = 0
            while (slot < size) {
                if (client.bankItems[slot] > 0 && client.bankItemsN[slot] >= 0) {
                    val t = tabs[slot]
                    if (t in 0..9) tabs[slot] = remap[t]
                } else {
                    tabs[slot] = 0
                }
                slot++
            }
        }
        // Remap the current/previous tab pointers against the new numbering. A pointer at a tab
        // that is now empty collapses to the "view all" tab (0), matching Tarnish's collapse().
        if (client.currentBankTab in 1..9) {
            client.currentBankTab = if (occupied[client.currentBankTab]) remap[client.currentBankTab] else 0
        }
        if (client.previousBankTab in 1..9) {
            client.previousBankTab = if (occupied[client.previousBankTab]) remap[client.previousBankTab] else 0
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
        if (client.bankItems[bankSlot] <= 0 || client.bankItemsN[bankSlot] < 0) {
            return
        }
        val itemId = client.bankItems[bankSlot] - 1
        val currentTab = client.bankSlotTabs[bankSlot]
        if (itemId == 995 && currentTab in 1..9 && tab in 1..9 && currentTab != tab && !hasBankTabItems(client, tab)) {
            return
        }
        // Clamp the destination so a new tab can only ever be the next sequential one.
        // Without this, dragging onto a far-right "+" would create e.g. tab 5 while only tab 1
        // exists, leaving gaps the client can't render. The highest used tab (excluding this
        // item's own slot) + 1 is the furthest a drag may create.
        var highestUsed = 0
        var i = 0
        val size = client.bankSize()
        while (i < size) {
            if (i != bankSlot && client.bankItems[i] > 0 && client.bankItemsN[i] >= 0) {
                val t = client.bankSlotTabs[i]
                if (t in 1..9 && t > highestUsed) highestUsed = t
            }
            i++
        }
        val maxAssignable = (highestUsed + 1).coerceAtMost(9)
        val targetTab = clampOwnedTab(tab).coerceAtMost(maxAssignable)
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
            // The tab button is a client-side config button that locally sets settings[211]=tab.
            // Send SetVarbit(211, currentBankTab) to revert the client's local change.
            client.send(SetVarbit(211, client.currentBankTab))
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
        client.contentRuntimeState.clearPendingInputState()
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
            if (client.bankItems[index] > 0 && client.bankItemsN[index] >= 0 && client.bankSlotTabs[index] == tab) {
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
            if (client.bankItems[index] > 0 && client.bankItemsN[index] >= 0) {
                used++
            }
            index++
        }
        client.sendString(used.toString(), 50053)
        client.sendString(size.toString(), 50055)
    }

    private fun rebuildBankContainers(client: Client) {
        ensureBankTabState(client)
        normalizeBankTabs(client)
        var tab = 0
        while (tab < client.bankContainerSlotMap.size) {
            Arrays.fill(client.bankContainerSlotMap[tab], -1)
            tab++
        }
        val counts = IntArray(11)
        val size = client.bankSize()
        var slot = 0
        while (slot < size) {
            if (client.bankItems[slot] <= 0 || client.bankItemsN[slot] < 0) {
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
        if (slot < 0 || slot >= client.bankSize() || client.bankItems[slot] <= 0 || client.bankItemsN[slot] < 0) {
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

    @JvmStatic
    fun togglePlaceholders(client: Client) {
        if (!client.IsBanking || client.bankStyleViewOpen) return
        client.bankPlaceholdersEnabled = !client.bankPlaceholdersEnabled
        client.markSaveDirty(PlayerSaveSegment.BANK.mask)
        refreshBankPreferences(client)
    }

    @JvmStatic
    fun releaseAllPlaceholders(client: Client): Int {
        if (!client.IsBanking || client.bankStyleViewOpen) return 0
        var released = 0
        for (slot in 0 until client.bankSize()) {
            if (client.bankItems[slot] > 0 && client.bankItemsN[slot] == 0) {
                clearBankEntry(client, slot)
                released++
            }
        }
        if (released > 0) {
            normalizeBankTabs(client)
            client.markSaveDirty(PlayerSaveSegment.BANK.mask)
            checkItemUpdate(client)
        }
        return released
    }

    @JvmStatic
    fun releasePlaceholder(client: Client, slot: Int): Boolean {
        if (!client.IsBanking || slot !in 0 until client.bankSize()) return false
        if (client.bankItems[slot] <= 0 || client.bankItemsN[slot] != 0) return false
        clearBankEntry(client, slot)
        normalizeBankTabs(client)
        client.markSaveDirty(PlayerSaveSegment.BANK.mask)
        checkItemUpdate(client)
        return true
    }

    @JvmStatic
    fun createPlaceholder(client: Client, itemId: Int): Boolean {
        if (!client.IsBanking || client.bankStyleViewOpen) return false
        val slot = resolveBankSlotByItemId(client, itemId)
        if (slot < 0) return false
        if (client.bankItemsN[slot] == 0) return true
        val previous = client.bankPlaceholdersEnabled
        client.bankPlaceholdersEnabled = true
        try {
            withdraw(client, itemId, slot, -2)
        } finally {
            client.bankPlaceholdersEnabled = previous
        }
        checkItemUpdate(client)
        return client.bankItems[slot] == itemId + 1 && client.bankItemsN[slot] == 0
    }

    @JvmStatic
    fun toggleInsertMode(client: Client) {
        if (!client.IsBanking || client.bankStyleViewOpen) return
        client.bankInsertMode = !client.bankInsertMode
        refreshBankPreferences(client)
    }

    @JvmStatic
    fun toggleNoteMode(client: Client) {
        if (!client.IsBanking || client.bankStyleViewOpen) return
        client.takeAsNote = !client.takeAsNote
        refreshBankPreferences(client)
    }

    @JvmStatic
    fun refreshBankPreferences(client: Client) {
        client.send(SetVarbit(INSERT_MODE_VARP, if (client.bankInsertMode) 1 else 0))
        client.send(SendTooltip(if (client.bankInsertMode) "Enable swapping" else "Enable inserting", INSERT_MODE_BUTTON))
        client.send(SetVarbit(NOTE_MODE_VARP, if (client.takeAsNote) 1 else 0))
        client.send(SendTooltip((if (client.takeAsNote) "Disable" else "Enable") + " noting", NOTE_MODE_BUTTON))
        client.send(SetVarbit(PLACEHOLDER_MODE_VARP, if (client.bankPlaceholdersEnabled) 1 else 0))
        client.send(
            SendTooltip(
                (if (client.bankPlaceholdersEnabled) "Disable" else "Enable") + " place holders",
                PLACEHOLDER_MODE_BUTTON,
            ),
        )
    }

    private fun clearBankEntry(client: Client, slot: Int) {
        client.bankItems[slot] = 0
        client.bankItemsN[slot] = 0
        client.bankSlotTabs[slot] = 0
    }

    private fun insertBankItem(client: Client, from: Int, to: Int) {
        if (from == to) return
        rebuildBankContainers(client)
        val orderedSlots = ArrayList<Int>()
        for (ownerTab in 0..9) {
            for (physicalSlot in client.bankContainerSlotMap[ownerTab]) {
                if (physicalSlot >= 0) orderedSlots.add(physicalSlot)
            }
        }
        val fromIndex = orderedSlots.indexOf(from)
        val toIndex = orderedSlots.indexOf(to)
        if (fromIndex < 0 || toIndex < 0) return
        val item = client.bankItems[from]
        val amount = client.bankItemsN[from]
        val sourceTab = client.bankSlotTabs[from]
        val targetTab = client.bankSlotTabs[to]
        if (fromIndex < toIndex) {
            for (index in fromIndex until toIndex) {
                val destination = orderedSlots[index]
                val source = orderedSlots[index + 1]
                client.bankItems[destination] = client.bankItems[source]
                client.bankItemsN[destination] = client.bankItemsN[source]
                client.bankSlotTabs[destination] = client.bankSlotTabs[source]
            }
        } else {
            for (index in fromIndex downTo toIndex + 1) {
                val destination = orderedSlots[index]
                val source = orderedSlots[index - 1]
                client.bankItems[destination] = client.bankItems[source]
                client.bankItemsN[destination] = client.bankItemsN[source]
                client.bankSlotTabs[destination] = client.bankSlotTabs[source]
            }
        }
        val destination = orderedSlots[toIndex]
        client.bankItems[destination] = item
        client.bankItemsN[destination] = amount
        client.bankSlotTabs[destination] = if (sourceTab == targetTab) sourceTab else targetTab
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
