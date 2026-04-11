package net.dodian.uber.game.systems.net

import net.dodian.uber.game.Server
import net.dodian.uber.game.content.events.partyroom.Balloons
import net.dodian.uber.game.content.skills.cooking.Cooking
import net.dodian.uber.game.content.skills.herblore.Herblore
import net.dodian.uber.game.content.skills.slayer.Slayer
import net.dodian.uber.game.content.skills.smithing.SmithingInterface
import net.dodian.uber.game.content.shop.ShopRulesService
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.InventoryInterface
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendFrame27
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.audit.ConsoleAuditLog
import net.dodian.uber.game.systems.interaction.ui.SkillingInterfaceItemService

object PacketBankingService {

    // -------------------------------------------------------------------------
    // Bank search (SyntaxInputListener opcode 60)
    // -------------------------------------------------------------------------

    /** Returns true when the client has a pending bank search input to process. */
    @JvmStatic
    fun hasPendingBankSearch(client: Client): Boolean = client.bankSearchPendingInput

    /**
     * Clears the pending-input flag and applies the bank-item filter.
     * No-ops if the player is not currently banking.
     */
    @JvmStatic
    fun applyPendingBankSearch(client: Client, input: String) {
        client.bankSearchPendingInput = false
        if (!client.IsBanking) return
        ConsoleAuditLog.bankSearch(client, input)
        client.applyBankSearch(input)
    }

    // -------------------------------------------------------------------------
    // Bank tab creation (BankTabCreationListener opcode 216)
    // -------------------------------------------------------------------------

    /**
     * Assigns a bank item to a tab from a drag gesture.
     * Guards: must be banking AND NOT in the bank-style-view.
     */
    @JvmStatic
    fun handleTabCreation(client: Client, fromInterface: Int, dragFromSlot: Int, toTab: Int) {
        if (!client.IsBanking || client.bankStyleViewOpen) return
        if (fromInterface < 50300 || fromInterface > 50310) return
        val clampedTab = toTab.coerceIn(0, 10)
        val bankSlot = client.resolveBankSlot(fromInterface, dragFromSlot)
        if (bankSlot < 0) return
        client.assignBankSlotToTab(bankSlot, clampedTab)
    }

    // -------------------------------------------------------------------------
    // BankAll (opcode 129) — absorbs guard + item resolution from listener
    // -------------------------------------------------------------------------

    /**
     * Simplified entry point used by [BankAllListener].
     * Internally handles the bank-style-view guard and resolves the true item id
     * from the player's inventory / bank before forwarding to the existing logic.
     */
    @JvmStatic
    fun handleBankAllDecoded(client: Client, interfaceId: Int, removeSlot: Int, removeId: Int) {
        // Guard: cannot interact while the styled bank view is open
        if ((interfaceId == 5382 || interfaceId in 50300..50310) && client.bankStyleViewOpen) return

        var bankSlot = removeSlot
        var resolvedItemId = removeId

        when {
            interfaceId == 5064 -> {
                if (removeSlot >= 0 && removeSlot < client.playerItems.size && client.playerItems[removeSlot] > 0) {
                    resolvedItemId = client.playerItems[removeSlot] - 1
                }
            }
            interfaceId == 5382 || interfaceId in 50300..50310 -> {
                bankSlot = client.resolveBankSlot(interfaceId, removeSlot)
                if (bankSlot >= 0 && bankSlot < client.bankItems.size && client.bankItems[bankSlot] > 0) {
                    resolvedItemId = client.bankItems[bankSlot] - 1
                }
            }
        }

        handleBankAll(client, interfaceId, removeSlot, removeId, bankSlot, resolvedItemId)
    }

    // -------------------------------------------------------------------------
    // Decoded entry-points for Bank5 / Bank10 listeners (guard + resolution)
    // -------------------------------------------------------------------------

    /**
     * Absorbs the bank-style-view guard and bank-slot resolution for
     * "deposit/withdraw N items" packets (Bank5Listener, Bank10Listener).
     */
    @JvmStatic
    fun handleFixedAmountDecoded(client: Client, interfaceId: Int, removeId: Int, removeSlot: Int, amount: Int) {
        if ((interfaceId == 5382 || interfaceId in 50300..50310) && client.bankStyleViewOpen) return
        var bankSlot = removeSlot
        var resolvedItemId = removeId
        if (interfaceId == 5382 || interfaceId in 50300..50310) {
            bankSlot = client.resolveBankSlot(interfaceId, removeSlot)
            resolvedItemId = client.resolveBankItemId(interfaceId, removeSlot, removeId)
        }
        handleFixedAmount(client, interfaceId, resolvedItemId, removeSlot, bankSlot, amount)
    }

    // -------------------------------------------------------------------------
    // Decoded entry-point for BankX1Listener (guard + resolution)
    // -------------------------------------------------------------------------

    /**
     * Absorbs the bank-style-view guard and bank-slot resolution for the
     * "X amount" prompt trigger (BankX1Listener opcode 135).
     */
    @JvmStatic
    fun handleXPromptDecoded(client: Client, interfaceId: Int, slot: Int, itemId: Int) {
        if ((interfaceId == 5382 || interfaceId in 50300..50310) && client.bankStyleViewOpen) return
        var resolvedSlot = slot
        var resolvedItemId = itemId
        if (interfaceId == 5382 || interfaceId in 50300..50310) {
            resolvedItemId = client.resolveBankItemId(interfaceId, slot, itemId)
            resolvedSlot = client.resolveBankSlot(interfaceId, slot)
            if (resolvedSlot < 0) return
        }
        handleXPrompt(client, interfaceId, resolvedSlot, resolvedItemId)
    }

    // -------------------------------------------------------------------------
    // Decoded entry-point for RemoveItemListener (guard + resolution)
    // -------------------------------------------------------------------------

    /**
     * Absorbs the bank-style-view guard and bank-slot resolution for the
     * single-item remove action (RemoveItemListener opcode 145).
     */
    @JvmStatic
    fun handleRemoveItemDecoded(client: Client, interfaceId: Int, removeSlot: Int, removeID: Int) {
        if ((interfaceId == 5382 || interfaceId in 50300..50310) && client.bankStyleViewOpen) return
        var bankSlot = removeSlot
        var resolvedItemId = removeID
        if (interfaceId == 5382 || interfaceId in 50300..50310) {
            bankSlot = client.resolveBankSlot(interfaceId, removeSlot)
            resolvedItemId = client.resolveBankItemId(interfaceId, removeSlot, removeID)
        }
        handleRemoveItem(client, interfaceId, removeSlot, resolvedItemId, bankSlot)
    }

    @JvmStatic
    fun handleBankAll(
        client: Client,
        interfaceId: Int,
        removeSlot: Int,
        removeId: Int,
        bankSlot: Int,
        resolvedItemId: Int,
    ) {
        val stack = Server.itemManager.isStackable(resolvedItemId)
        if (!isValidBankAllRequest(client, interfaceId, removeSlot, bankSlot, stack)) {
            return
        }

        when {
            interfaceId == 5064 -> {
                val amount = if (stack) client.playerItemsN[removeSlot] else client.getInvAmt(resolvedItemId)
                if (client.IsBanking) {
                    ConsoleAuditLog.bankDeposit(client, resolvedItemId, "ALL", removeSlot)
                    client.bankItem(resolvedItemId, removeSlot, amount)
                } else if (client.isPartyInterface) {
                    Balloons.offerItems(client, resolvedItemId, amount, removeSlot)
                }
                client.checkItemUpdate()
            }

            interfaceId == 5382 || interfaceId in 50300..50310 -> {
                if (bankSlot >= 0) {
                    ConsoleAuditLog.bankWithdraw(client, resolvedItemId, "ALL", bankSlot)
                    client.fromBank(resolvedItemId, bankSlot, -2)
                }
            }

            interfaceId == 2274 -> {
                Balloons.removeOfferItems(client, removeId, if (!stack) 8 else Int.MAX_VALUE, removeSlot)
            }

            interfaceId == 3322 && client.inTrade && client.canOffer -> {
                val amount = if (stack) client.playerItemsN[removeSlot] else client.getInvAmt(removeId)
                client.tradeItem(removeId, removeSlot, amount)
            }

            interfaceId == 3322 && client.inDuel && client.canOffer -> {
                val amount = if (stack) client.playerItemsN[removeSlot] else client.getInvAmt(removeId)
                client.stakeItem(removeId, removeSlot, amount)
            }

            interfaceId == 6669 && client.inDuel && client.canOffer -> {
                client.fromDuel(removeId, removeSlot, if (stack) client.offeredItems[removeSlot].amount else 28)
            }

            interfaceId == 3415 && client.inTrade && client.canOffer -> {
                client.fromTrade(removeId, removeSlot, if (stack) client.offeredItems[removeSlot].amount else 28)
            }

            SkillingInterfaceItemService.handleContainerAmount(
                client,
                interfaceId,
                resolvedItemId,
                removeSlot,
                if (interfaceId in 4233..4257) client.getInvAmt(2357) else Int.MAX_VALUE,
            ) -> {
            }

            interfaceId == 3823 -> {
                if (client.playerRights < 2) {
                    client.sellItem(resolvedItemId, removeSlot, 10)
                } else {
                    client.send(SendFrame27())
                    client.XinterfaceID = interfaceId
                    client.XremoveID = resolvedItemId
                    client.XremoveSlot = bankSlot
                }
            }

            interfaceId == 3900 -> {
                client.send(SendFrame27())
                client.XinterfaceID = interfaceId
                client.XremoveID = resolvedItemId
                client.XremoveSlot = bankSlot
            }
        }

        if (client.playerRights == 2) {
            client.println_debug(
                "BankAll: interfaceId=$interfaceId itemId=$resolvedItemId slot=$removeSlot stack=$stack",
            )
        }
    }

    @JvmStatic
    fun handleFixedAmount(
        client: Client,
        interfaceId: Int,
        removeId: Int,
        removeSlot: Int,
        bankSlot: Int,
        amount: Int,
    ) {
        if (!isValidFixedAmountRequest(client, interfaceId, removeSlot, bankSlot)) {
            return
        }

        when (interfaceId) {
            3322 -> {
                if (client.inDuel && client.canOffer) {
                    client.stakeItem(removeId, removeSlot, amount)
                } else if (client.inTrade && client.canOffer) {
                    client.tradeItem(removeId, removeSlot, amount)
                }
            }

            6669 -> {
                if (client.inDuel && client.canOffer) {
                    client.fromDuel(removeId, removeSlot, amount)
                }
            }

            5064 -> {
                if (client.IsBanking) {
                    ConsoleAuditLog.bankDeposit(client, removeId, amount.toString(), removeSlot)
                    client.bankItem(removeId, removeSlot, amount)
                } else if (client.isPartyInterface) {
                    Balloons.offerItems(client, removeId, amount, removeSlot)
                }
                client.checkItemUpdate()
            }

            5382 -> {
                if (bankSlot >= 0) {
                    ConsoleAuditLog.bankWithdraw(client, removeId, amount.toString(), bankSlot)
                    client.fromBank(removeId, bankSlot, amount)
                }
            }

            2274 -> {
                Balloons.removeOfferItems(client, removeId, amount, removeSlot)
            }

            3415 -> {
                if (client.inTrade && client.canOffer) {
                    client.fromTrade(removeId, removeSlot, amount)
                }
            }

            else -> {
                if (interfaceId in 50300..50310) {
                    if (bankSlot >= 0) {
                        ConsoleAuditLog.bankWithdraw(client, removeId, amount.toString(), bankSlot)
                        client.fromBank(removeId, bankSlot, amount)
                    }
                } else {
                    handleFixedAmountSpecialInterfaces(client, interfaceId, removeId, removeSlot, amount)
                }
            }
        }
    }

    private fun handleFixedAmountSpecialInterfaces(
        client: Client,
        interfaceId: Int,
        removeId: Int,
        removeSlot: Int,
        amount: Int,
    ) {
        when {
            SkillingInterfaceItemService.handleContainerAmount(
                client,
                interfaceId,
                removeId,
                removeSlot,
                if (amount == 10 && interfaceId in 1119..1123) client.getInvAmt(removeId) else amount,
            ) -> {
            }

            amount == 5 && interfaceId == 3823 -> {
                client.sellItem(removeId, removeSlot, 1)
            }

            amount == 5 && interfaceId == 3900 -> {
                client.buyItem(removeId, removeSlot, 1)
            }

            amount == 5 && interfaceId == 1688 -> {
                if (removeId == 4566) {
                    client.performAnimation(1835, 0)
                } else if (removeSlot == 0 && client.gotSlayerHelmet(client)) {
                    Slayer.sendCurrentTask(client)
                }
            }

            amount == 10 && interfaceId == 3823 -> {
                client.sellItem(removeId, removeSlot, 5)
            }

            amount == 10 && interfaceId == 3900 -> {
                client.buyItem(removeId, removeSlot, 5)
            }
        }
    }

    @JvmStatic
    fun handleXPrompt(client: Client, interfaceId: Int, slot: Int, itemId: Int) {
        client.XremoveSlot = slot
        client.XinterfaceID = interfaceId
        client.XremoveID = itemId

        if (
            interfaceId == 5382 ||
            interfaceId in 50300..50310 ||
            interfaceId == 5064 ||
            interfaceId == 3322 ||
            interfaceId == 3415 ||
            interfaceId == 6669 ||
            interfaceId == 2274 ||
            interfaceId == 3900 ||
            interfaceId == 3823 ||
            interfaceId in 4233..4257 ||
            SmithingInterface.isSmeltingInterfaceFrame(interfaceId)
        ) {
            client.send(SendFrame27())
        }
    }

    @JvmStatic
    fun handleXAmount(client: Client, enteredAmount: Int) {
        try {
            if (client.convoId == 1001) {
                client.send(RemoveInterfaces())
                Server.slots.rollDice(client, enteredAmount)
                return
            }
            if (Herblore.handleEnteredAmount(client, enteredAmount)) {
                return
            }
            if (client.XinterfaceID == 3838) {
                client.send(RemoveInterfaces())
                var amount = if (client.dailyReward.isEmpty()) 0 else client.dailyReward[2].toInt()
                val totalAmount = amount
                amount = minOf(enteredAmount, amount)
                val coins = client.getInvAmt(995)
                amount = if (coins == 0) 0 else minOf(amount, coins / 7000)
                if (coins < 7000) {
                    client.showNPCChat(3837, 597, arrayOf("You do not have enough coins to purchase one battlestaff."))
                } else {
                    client.deleteItem(995, amount * 7_000)
                    client.addItem(1392, amount)
                    client.dailyReward[2] = (totalAmount - amount).toString()
                    client.checkItemUpdate()
                    client.showNPCChat(3837, 595, arrayOf("Here is $amount battlestaffs for you."))
                }
                return
            }
            if (client.enterAmountId > 0) {
                client.send(RemoveInterfaces())
                if (client.enterAmountId == 1) {
                    if (client.inTrade || client.inDuel) {
                        client.send(SendMessage("Cannot cook in duel or trade"))
                        return
                    }
                    Cooking.startFromEnteredAmount(client, enteredAmount)
                    return
                }
                if (client.enterAmountId == 2) {
                    client.send(RemoveInterfaces())
                    SmithingInterface.startFromPending(client, enteredAmount)
                    return
                }
            }
            when {
                client.XinterfaceID == 5064 -> {
                    if (!isValidInventorySlot(client, client.XremoveSlot)) {
                        return
                    }
                    if (client.IsBanking) {
                        ConsoleAuditLog.bankDeposit(client, client.playerItems[client.XremoveSlot] - 1, enteredAmount.toString(), client.XremoveSlot)
                        client.bankItem(client.playerItems[client.XremoveSlot] - 1, client.XremoveSlot, enteredAmount)
                    } else if (client.isPartyInterface) {
                        Balloons.offerItems(
                            client,
                            client.playerItems[client.XremoveSlot] - 1,
                            enteredAmount,
                            client.XremoveSlot,
                        )
                    }
                    client.checkItemUpdate()
                }

                client.XinterfaceID == 5382 || client.XinterfaceID in 50300..50310 -> {
                    if (client.bankStyleViewOpen) {
                        return
                    }
                    if (!isValidBankSlot(client, client.XremoveSlot)) {
                        return
                    }
                    if (client.bankItems[client.XremoveSlot] > 0) {
                        ConsoleAuditLog.bankWithdraw(client, client.bankItems[client.XremoveSlot] - 1, enteredAmount.toString(), client.XremoveSlot)
                        client.fromBank(client.bankItems[client.XremoveSlot] - 1, client.XremoveSlot, enteredAmount)
                        client.checkItemUpdate()
                    }
                }

                client.XinterfaceID == 2274 -> {
                    if (!isValidPartyOfferSlot(client, client.XremoveSlot)) {
                        return
                    }
                    Balloons.removeOfferItems(
                        client,
                        client.offeredPartyItems[client.XremoveSlot].getId(),
                        enteredAmount,
                        client.XremoveSlot,
                    )
                    client.checkItemUpdate()
                }

                client.XinterfaceID == 3322 && client.inDuel && client.canOffer -> {
                    client.stakeItem(client.XremoveID, client.XremoveSlot, enteredAmount)
                }

                client.XinterfaceID == 6669 && client.inDuel && client.canOffer -> {
                    client.fromDuel(client.XremoveID, client.XremoveSlot, enteredAmount)
                }

                client.XinterfaceID == 3900 && client.XremoveID != -1 -> {
                    val id = client.XremoveID
                    val slot = client.XremoveSlot
                    client.XremoveID = -1
                    client.XremoveSlot = -1
                    client.buyItem(id, slot, enteredAmount)
                    client.checkItemUpdate()
                    client.send(InventoryInterface(3824, 3822))
                }

                client.XinterfaceID == 3823 && client.XremoveID != -1 -> {
                    val id = client.XremoveID
                    val slot = client.XremoveSlot
                    client.XremoveID = -1
                    client.XremoveSlot = -1
                    client.sellItem(id, slot, enteredAmount)
                    client.checkItemUpdate()
                    client.send(InventoryInterface(3824, 3822))
                }

                SmithingInterface.isSmeltingInterfaceFrame(client.XinterfaceID) -> {
                    SmithingInterface.startFromInterfaceItem(client, client.XremoveID, enteredAmount)
                }

                SkillingInterfaceItemService.handleContainerAmount(
                    client,
                    client.XinterfaceID,
                    client.XremoveID,
                    client.XremoveSlot,
                    enteredAmount,
                ) -> {
                }

                client.XinterfaceID == 3322 && client.inTrade && client.canOffer -> {
                    client.tradeItem(client.XremoveID, client.XremoveSlot, enteredAmount)
                }

                client.XinterfaceID == 3415 && client.inTrade && client.canOffer -> {
                    client.fromTrade(client.XremoveID, client.XremoveSlot, enteredAmount)
                }
            }
        } finally {
            client.XinterfaceID = -1
            client.enterAmountId = 0
        }
    }

    @JvmStatic
    fun handleRemoveItem(
        client: Client,
        interfaceId: Int,
        removeSlot: Int,
        removeId: Int,
        bankSlot: Int,
    ) {
        if (!isValidRemoveItemRequest(client, interfaceId, removeSlot, removeId, bankSlot)) {
            return
        }

        when {
            interfaceId == 3322 && client.inDuel && client.canOffer -> {
                client.stakeItem(removeId, removeSlot, 1)
            }

            interfaceId == 6669 && client.inDuel && client.canOffer -> {
                client.fromDuel(removeId, removeSlot, 1)
            }

            interfaceId == 1688 -> {
                if (client.hasSpace()) {
                    val id = client.getEquipment()[removeSlot]
                    val amount = client.getEquipmentN()[removeSlot]
                    if (client.remove(removeSlot, false)) {
                        client.addItem(id, amount)
                    }
                    client.checkItemUpdate()
                } else {
                    client.send(SendMessage("Not enough space to unequip this item!"))
                }
            }

            interfaceId == 5064 -> {
                if (client.IsBanking) {
                    ConsoleAuditLog.bankDeposit(client, removeId, "1", removeSlot)
                    client.bankItem(removeId, removeSlot, 1)
                } else if (client.isPartyInterface) {
                    Balloons.offerItems(client, removeId, 1, removeSlot)
                }
                client.checkItemUpdate()
            }

            interfaceId == 5382 || interfaceId in 50300..50310 -> {
                if (bankSlot >= 0) {
                    ConsoleAuditLog.bankWithdraw(client, removeId, "1", bankSlot)
                    client.fromBank(removeId, bankSlot, 1)
                }
            }

            interfaceId == 2274 -> {
                Balloons.removeOfferItems(client, removeId, 1, removeSlot)
            }

            interfaceId == 3322 && client.inTrade && client.canOffer -> {
                client.tradeItem(removeId, removeSlot, 1)
            }

            interfaceId == 3415 && client.inTrade && client.canOffer -> {
                client.fromTrade(removeId, removeSlot, 1)
            }

            SkillingInterfaceItemService.handleContainerAmount(client, interfaceId, removeId, removeSlot, 1) -> {
            }

            interfaceId == 3823 -> {
                if (!Server.shopping || client.tradeLocked) {
                    client.send(
                        SendMessage(
                            if (client.tradeLocked) {
                                "You are trade locked!"
                            } else {
                                "Currently selling stuff to the store has been disabled!"
                            },
                        ),
                    )
                    return
                }
                val sellItemId = client.getUnnotedItem(removeId).takeIf { it > 0 } ?: removeId
                if (ShopRulesService.sellPrice(sellItemId) < 0 || !Server.itemManager.isTradable(sellItemId)) {
                    client.send(
                        SendMessage(
                            "You cannot sell ${client.getItemName(removeId).lowercase()} in this store.",
                        ),
                    )
                    return
                }
                if (!ShopRulesService.canSellItemToShop(client.MyShopID, sellItemId)) {
                    client.send(
                        SendMessage(
                            "You cannot sell ${client.getItemName(removeId).lowercase()} in this store.",
                        ),
                    )
                } else {
                    val currency = ShopRulesService.currencyItemId(client.MyShopID)
                    val shopValue = ShopRulesService.sellPrice(sellItemId)
                    val shopAdd = formatValueSuffix(shopValue)
                    client.send(
                        SendMessage(
                            "${client.getItemName(removeId)}: shop will buy for $shopValue ${client.getItemName(currency).lowercase()}$shopAdd",
                        ),
                    )
                }
            }

            interfaceId == 3900 -> {
                val currency = ShopRulesService.currencyItemId(client.MyShopID)
                val shopValue = ShopRulesService.buyPrice(client.MyShopID, removeId, removeSlot)
                val shopAdd = formatValueSuffix(shopValue)
                client.send(
                    SendMessage(
                        "${client.getItemName(removeId)}: currently costs $shopValue ${client.getItemName(currency).lowercase()}$shopAdd",
                    ),
                )
            }
        }
        client.CheckGear()
    }

    @JvmStatic
    fun handleMoveItems(client: Client, interfaceId: Int, itemFrom: Int, itemTo: Int) {
        if (!isValidInventorySlot(client, itemFrom) || !isValidInventorySlot(client, itemTo)) {
            return
        }
        client.moveItems(itemFrom, itemTo, interfaceId)
    }

    private fun isValidBankAllRequest(
        client: Client,
        interfaceId: Int,
        removeSlot: Int,
        bankSlot: Int,
        stack: Boolean,
    ): Boolean =
        when {
            interfaceId == 5064 -> isValidInventorySlot(client, removeSlot)
            interfaceId == 3322 -> isValidInventorySlot(client, removeSlot)
            interfaceId == 2274 -> isValidPartyOfferSlot(client, removeSlot)
            interfaceId == 6669 || interfaceId == 3415 -> !stack || isValidTradeOfferSlot(client, removeSlot)
            interfaceId == 5382 || interfaceId in 50300..50310 -> bankSlot < 0 || isValidBankSlot(client, bankSlot)
            else -> true
        }

    private fun isValidFixedAmountRequest(
        client: Client,
        interfaceId: Int,
        removeSlot: Int,
        bankSlot: Int,
    ): Boolean =
        when {
            interfaceId == 5064 || interfaceId == 3322 -> isValidInventorySlot(client, removeSlot)
            interfaceId == 2274 -> isValidPartyOfferSlot(client, removeSlot)
            interfaceId == 6669 || interfaceId == 3415 -> isValidTradeOfferSlot(client, removeSlot)
            interfaceId == 5382 || interfaceId in 50300..50310 -> bankSlot < 0 || isValidBankSlot(client, bankSlot)
            else -> true
        }

    private fun isValidRemoveItemRequest(
        client: Client,
        interfaceId: Int,
        removeSlot: Int,
        removeId: Int,
        bankSlot: Int,
    ): Boolean =
        when {
            interfaceId == 1688 -> {
                if (!isValidEquipmentSlot(client, removeSlot)) {
                    false
                } else {
                    val equippedId = client.getEquipment()[removeSlot]
                    val equippedAmount = client.getEquipmentN()[removeSlot]
                    equippedId >= 0 && equippedAmount > 0 && equippedId == removeId
                }
            }
            interfaceId == 5064 || interfaceId == 3322 -> isValidInventorySlot(client, removeSlot)
            interfaceId == 2274 -> isValidPartyOfferSlot(client, removeSlot)
            interfaceId == 6669 || interfaceId == 3415 -> isValidTradeOfferSlot(client, removeSlot)
            interfaceId == 5382 || interfaceId in 50300..50310 -> bankSlot < 0 || isValidBankSlot(client, bankSlot)
            else -> true
        }

    private fun isValidInventorySlot(client: Client, slot: Int): Boolean = slot in client.playerItems.indices

    private fun isValidBankSlot(client: Client, slot: Int): Boolean = slot >= 0 && slot < client.bankSize()

    private fun isValidTradeOfferSlot(client: Client, slot: Int): Boolean = slot in 0 until client.offeredItems.size

    private fun isValidPartyOfferSlot(client: Client, slot: Int): Boolean = slot in 0 until client.offeredPartyItems.size

    private fun isValidEquipmentSlot(client: Client, slot: Int): Boolean = slot in client.getEquipment().indices

    private fun formatValueSuffix(value: Int): String {
        val thousand = 1_000
        val million = 1_000_000
        return when {
            value >= thousand && value < million -> " (${value / thousand}K)"
            value >= million -> {
                val leftover = value - (value / million) * million
                val decimal = if (leftover / 100_000 > 0) ".${leftover / 100_000}" else ""
                " (${value / million}$decimal million)"
            }
            else -> ""
        }
    }
}
