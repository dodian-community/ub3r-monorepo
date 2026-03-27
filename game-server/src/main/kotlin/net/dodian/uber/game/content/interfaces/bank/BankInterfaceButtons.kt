package net.dodian.uber.game.content.interfaces.bank

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendEnterName
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.systems.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.systems.ui.buttons.buttonBinding

object BankInterfaceButtons : InterfaceButtonContent {
    override val bindings =
        buildList {
            add(
                buttonBinding(
                    interfaceId = BankComponents.INTERFACE_ID,
                    componentId = 0,
                    componentKey = "bank.deposit_inventory",
                    rawButtonIds = BankComponents.depositInventoryButtons,
                    requiredInterfaceId = BankComponents.INTERFACE_ID,
                ) { client, _ ->
                    if (!client.IsBanking) return@buttonBinding true
                    for (i in client.playerItems.indices) {
                        if (client.playerItems[i] > 0) {
                            client.bankItem(client.playerItems[i] - 1, i, client.playerItemsN[i])
                        }
                    }
                    client.send(SendMessage("You deposit all your items."))
                    client.checkItemUpdate()
                    true
                }
            )
            add(
                buttonBinding(
                    interfaceId = BankComponents.INTERFACE_ID,
                    componentId = 1,
                    componentKey = "bank.deposit_equipment",
                    rawButtonIds = BankComponents.depositWornItemsButtons,
                    requiredInterfaceId = BankComponents.INTERFACE_ID,
                ) { client, _ ->
                    if (!client.IsBanking) return@buttonBinding true
                    val equipment = client.equipment
                    val equipmentN = client.equipmentN
                    for (i in equipment.indices) {
                        val equipId = equipment[i]
                        val equipAmount = equipmentN[i]
                        if (equipId > 0 && equipAmount > 0 && client.hasSpace()) {
                            if (client.remove(i, false)) {
                                client.addItem(equipId, equipAmount)
                                client.bankItem(equipId, client.GetItemSlot(equipId), equipAmount)
                            }
                        }
                    }
                    client.send(SendMessage("You deposit your worn items."))
                    client.checkItemUpdate()
                    true
                }
            )
            add(
                buttonBinding(
                    interfaceId = BankComponents.INTERFACE_ID,
                    componentId = 2,
                    componentKey = "bank.withdraw_note",
                    rawButtonIds = BankComponents.withdrawAsNoteButtons,
                    requiredInterfaceId = BankComponents.INTERFACE_ID,
                ) { client, _ ->
                    if (!client.IsBanking || client.bankStyleViewOpen) return@buttonBinding true
                    client.takeAsNote = true
                    client.send(SendMessage("You can now note items."))
                    true
                }
            )
            add(
                buttonBinding(
                    interfaceId = BankComponents.INTERFACE_ID,
                    componentId = 3,
                    componentKey = "bank.withdraw_item",
                    rawButtonIds = BankComponents.withdrawAsItemButtons,
                    requiredInterfaceId = BankComponents.INTERFACE_ID,
                ) { client, _ ->
                    if (!client.IsBanking || client.bankStyleViewOpen) return@buttonBinding true
                    client.takeAsNote = false
                    client.send(SendMessage("You can no longer note items."))
                    true
                }
            )
            add(
                buttonBinding(
                    interfaceId = BankComponents.INTERFACE_ID,
                    componentId = 4,
                    componentKey = "bank.search",
                    rawButtonIds = BankComponents.searchButtons,
                    requiredInterfaceId = BankComponents.INTERFACE_ID,
                ) { client, _ ->
                    if (!client.IsBanking || client.bankStyleViewOpen) return@buttonBinding true
                    if (client.bankSearchActive) {
                        client.clearBankSearch()
                    } else {
                        client.bankSearchPendingInput = true
                        client.send(SendEnterName("Search bank:"))
                    }
                    true
                }
            )
            BankComponents.tabButtons.forEachIndexed { tab, rawButtonId ->
                add(
                    buttonBinding(
                        interfaceId = BankComponents.INTERFACE_ID,
                        componentId = 100 + tab,
                        componentKey = "bank.tab.$tab.select",
                        rawButtonIds = intArrayOf(rawButtonId),
                        requiredInterfaceId = BankComponents.INTERFACE_ID,
                    ) { client, _ ->
                        if (!client.IsBanking || client.bankStyleViewOpen) return@buttonBinding true
                        client.selectBankTab(tab)
                        true
                    }
                )
                add(
                    buttonBinding(
                        interfaceId = BankComponents.INTERFACE_ID,
                        componentId = 200 + tab,
                        componentKey = "bank.tab.$tab.collapse",
                        rawButtonIds = intArrayOf(rawButtonId),
                        requiredInterfaceId = BankComponents.INTERFACE_ID,
                        opIndex = 1,
                    ) { client, _ ->
                        if (!client.IsBanking || client.bankStyleViewOpen) return@buttonBinding true
                        if (tab > 0) {
                            client.collapseBankTab(tab)
                        } else {
                            client.selectBankTab(tab)
                        }
                        true
                    }
                )
            }
        }
}

