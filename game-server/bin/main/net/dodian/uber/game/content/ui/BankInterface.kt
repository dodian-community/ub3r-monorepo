package net.dodian.uber.game.content.ui

import net.dodian.uber.game.netty.listener.out.SendEnterName
import net.dodian.uber.game.systems.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.systems.ui.buttons.buttonBinding

object BankInterface : InterfaceButtonContent {
    private const val INTERFACE_ID = 5292

    private val depositInventoryButtons = intArrayOf(89223, 50004)
    private val depositWornItemsButtons = intArrayOf(50007)
    private val withdrawAsNoteButtons = intArrayOf(21011, 5387)
    private val withdrawAsItemButtons = intArrayOf(21010, 5386)
    private val searchButtons = intArrayOf(50010)
    private val tabButtons: List<Int> = (0..9).map { 50070 + it * 4 }

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
                            client.bankItem(client.playerItems[i] - 1, i, client.playerItemsN[i])
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
                    rawButtonIds = depositWornItemsButtons,
                    requiredInterfaceId = INTERFACE_ID,
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
                                client.bankItem(equipId, client.getItemSlot(equipId), equipAmount)
                            }
                        }
                    }
                    client.sendMessage("You deposit your worn items.")
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
                    client.takeAsNote = true
                    client.sendMessage("You can now note items.")
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
                    client.takeAsNote = false
                    client.sendMessage("You can no longer note items.")
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
                        client.bankSearchPendingInput = true
                        client.send(SendEnterName("Search bank:"))
                    }
                    true
                },
            )

            tabButtons.forEachIndexed { tab, rawButtonId ->
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
                add(
                    buttonBinding(
                        interfaceId = INTERFACE_ID,
                        componentId = 200 + tab,
                        componentKey = "bank.tab.$tab.collapse",
                        rawButtonIds = intArrayOf(rawButtonId),
                        requiredInterfaceId = INTERFACE_ID,
                        opIndex = 1,
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
