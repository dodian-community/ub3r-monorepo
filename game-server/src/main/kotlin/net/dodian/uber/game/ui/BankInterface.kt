package net.dodian.uber.game.ui

import net.dodian.uber.game.engine.systems.inventory.EconomyTransaction
import net.dodian.uber.game.netty.listener.out.SendEnterName
import net.dodian.uber.game.persistence.player.PlayerSaveSegment
import net.dodian.uber.game.ui.bank.PlayerBankService
import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding
import net.dodian.uber.game.model.entity.UpdateFlag
import net.dodian.uber.game.model.entity.player.PendingInputState

object BankInterface : InterfaceButtonContent {
    private const val INTERFACE_ID = 60000

    // interfaceId + 8 = 60008 (Deposit Inventory hover button)
    private val depositInventoryButtons = intArrayOf(60008)
    // interfaceId + 11 = 60011 (Deposit Worn Items hover button)
    private val depositEquipmentButtons = intArrayOf(60011)
    // interfaceId + 7 = 60007 (Toggle Note/Un-note config button)
    private val withdrawAsNoteButtons = intArrayOf(60007)
    // interfaceId + 6 = 60006 (Toggle Insert/Swap config button)
    private val withdrawAsItemButtons = intArrayOf(60006)
    // interfaceId + 20 = 60020 (clear/search hover button in bank header)
    private val searchButtons = intArrayOf(60020)
    // interfaceId + 72 = 60072 (Release Place Holders)
    private val releasePlaceholdersButtons = intArrayOf(60072)
    // interfaceId + 73 = 60073 (Toggle Place Holders hover button)
    private val togglePlaceholdersButtons = intArrayOf(60073)
    // Tab display settings: item icons (60023), numbers (60025), roman numerals (60027)
    private val tabDisplayButtons = intArrayOf(60023, 60025, 60027)

    // Tab select buttons: interfaceId + 32 + tabIndex*4 (hover config button, left-click)
    private val tabSelectButtons: List<Int> = (0..9).map { INTERFACE_ID + 32 + it * 4 }

    // Tab collapse buttons: interfaceId + 31 + tabIndex*4 (plain button, right-click "Collapse")
    private val tabCollapseButtons: List<Int> = (0..9).map { INTERFACE_ID + 31 + it * 4 }

    override val bindings =
        buildList {
            add(
                buttonBinding(
                    interfaceId = INTERFACE_ID,
                    componentId = 0,
                    componentKey = "bank.deposit_inventory",
                    rawButtonIds = depositInventoryButtons,
                    requiredInterfaceId = INTERFACE_ID,
                ) { client, _ ->
                    if (!client.IsBanking) return@buttonBinding true
                    for (i in client.playerItems.indices) {
                        if (client.playerItems[i] > 0) {
                            PlayerBankService.deposit(client, client.playerItems[i] - 1, i, client.playerItemsN[i])
                        }
                    }
                    client.sendMessage("You deposit all your items.")
                    client.checkItemUpdate()
                    true
                },
            )
            add(
                buttonBinding(
                    interfaceId = INTERFACE_ID,
                    componentId = 1,
                    componentKey = "bank.deposit_equipment",
                    rawButtonIds = depositEquipmentButtons,
                    requiredInterfaceId = INTERFACE_ID,
                ) { client, _ ->
                    if (!client.IsBanking) return@buttonBinding true
                    val toDeposit = client.equipment.indices
                        .filter { slot -> client.equipment[slot] >= 0 && client.equipmentN[slot] > 0 }
                        .map { slot -> Triple(slot, client.equipment[slot], client.equipmentN[slot]) }
                    if (toDeposit.isEmpty()) {
                        client.checkItemUpdate()
                        return@buttonBinding true
                    }
                    PlayerBankService.ensureBankTabState(client)
                    val newEntries = toDeposit.filter { (_, id, _) -> client.getBankSlot(id) < 0 }.map { it.second }
                    // Bank and equipment are mutated as one atomic commit — either every worn
                    // item moves to the bank, or none of them do (no half-deposited state).
                    val committed = EconomyTransaction.run {
                        toDeposit.forEach { (slot, id, amount) ->
                            bank(client).add(id, amount)
                            equipment(client).removeAt(slot, id, amount)
                        }
                    }
                    if (!committed) {
                        client.sendMessage("Your bank is full!")
                    } else {
                        newEntries.forEach { id ->
                            val bankSlot = client.getBankSlot(id)
                            if (bankSlot >= 0 && client.currentBankTab in 1..9 && !client.bankSearchActive) {
                                client.bankSlotTabs[bankSlot] = client.currentBankTab
                            }
                        }
                        client.markSaveDirty(PlayerSaveSegment.EQUIPMENT.mask or PlayerSaveSegment.BANK.mask)
                        client.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true)
                    }
                    client.checkItemUpdate()
                    true
                },
            )
            add(
                buttonBinding(
                    interfaceId = INTERFACE_ID,
                    componentId = 2,
                    componentKey = "bank.withdraw_note",
                    rawButtonIds = withdrawAsNoteButtons,
                    requiredInterfaceId = INTERFACE_ID,
                ) { client, _ ->
                    if (!client.IsBanking || client.bankStyleViewOpen) return@buttonBinding true
                    PlayerBankService.toggleNoteMode(client)
                    true
                },
            )
            add(
                buttonBinding(
                    interfaceId = INTERFACE_ID,
                    componentId = 3,
                    componentKey = "bank.withdraw_item",
                    rawButtonIds = withdrawAsItemButtons,
                    requiredInterfaceId = INTERFACE_ID,
                ) { client, _ ->
                    if (!client.IsBanking || client.bankStyleViewOpen) return@buttonBinding true
                    PlayerBankService.toggleInsertMode(client)
                    true
                },
            )
            add(
                buttonBinding(
                    interfaceId = INTERFACE_ID,
                    componentId = 4,
                    componentKey = "bank.search",
                    rawButtonIds = searchButtons,
                    requiredInterfaceId = INTERFACE_ID,
                ) { client, _ ->
                    if (!client.IsBanking || client.bankStyleViewOpen) return@buttonBinding true
                    if (client.bankSearchActive) {
                        client.clearBankSearch()
                    } else {
                        client.contentRuntimeState.setPendingInputState(PendingInputState.BANK_SEARCH)
                        client.send(SendEnterName("Search bank:"))
                    }
                    true
                },
            )
            add(
                buttonBinding(
                    interfaceId = INTERFACE_ID,
                    componentId = 5,
                    componentKey = "bank.release_placeholders",
                    rawButtonIds = releasePlaceholdersButtons,
                    requiredInterfaceId = INTERFACE_ID,
                ) { client, _ ->
                    if (!client.IsBanking || client.bankStyleViewOpen) return@buttonBinding true
                    val released = PlayerBankService.releaseAllPlaceholders(client)
                    client.sendMessage(
                        if (released == 0) {
                            "There are no place holders available for you to release."
                        } else {
                            "You have released $released place holders."
                        },
                    )
                    true
                },
            )
            add(
                buttonBinding(
                    interfaceId = INTERFACE_ID,
                    componentId = 6,
                    componentKey = "bank.toggle_placeholders",
                    rawButtonIds = togglePlaceholdersButtons,
                    requiredInterfaceId = INTERFACE_ID,
                ) { client, _ ->
                    PlayerBankService.togglePlaceholders(client)
                    true
                },
            )
            add(
                buttonBinding(
                    interfaceId = INTERFACE_ID,
                    componentId = 7,
                    componentKey = "bank.tab_display",
                    rawButtonIds = tabDisplayButtons,
                    requiredInterfaceId = INTERFACE_ID,
                ) { _, _ ->
                    // Client updates settings[210] locally via button script; no server action needed.
                    true
                },
            )

            tabSelectButtons.forEachIndexed { tab, rawButtonId ->
                add(
                    buttonBinding(
                        interfaceId = INTERFACE_ID,
                        componentId = 100 + tab,
                        componentKey = "bank.tab.$tab.select",
                        rawButtonIds = intArrayOf(rawButtonId),
                        requiredInterfaceId = INTERFACE_ID,
                    ) { client, _ ->
                        if (!client.IsBanking || client.bankStyleViewOpen) return@buttonBinding true
                        client.selectBankTab(tab)
                        true
                    },
                )
            }

            tabCollapseButtons.forEachIndexed { tab, rawButtonId ->
                add(
                    buttonBinding(
                        interfaceId = INTERFACE_ID,
                        componentId = 200 + tab,
                        componentKey = "bank.tab.$tab.collapse",
                        rawButtonIds = intArrayOf(rawButtonId),
                        requiredInterfaceId = INTERFACE_ID,
                    ) { client, _ ->
                        if (!client.IsBanking || client.bankStyleViewOpen) return@buttonBinding true
                        if (tab > 0) {
                            client.collapseBankTab(tab)
                        } else {
                            client.selectBankTab(tab)
                        }
                        true
                    },
                )
            }
        }
}
