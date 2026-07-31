@file:Suppress("unused")

package net.dodian.uber.game.ui

import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding

import net.dodian.uber.game.api.content.ContentActionCancelReason
import net.dodian.uber.game.api.content.ContentActions
import net.dodian.uber.game.api.content.ContentSafety
import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.netty.listener.out.SetTabInterface
import net.dodian.uber.game.netty.listener.out.ShowInterface
import net.dodian.uber.game.economy.PriceCheckerService
import net.dodian.uber.game.engine.systems.skills.SkillItemListMenuService

object UiInterface : InterfaceButtonContent {
    private val runOffButtons = intArrayOf(152)
    private val runOnButtons = intArrayOf(19158)
    private val runToggleButtons = intArrayOf(74214, 1050)
    private val tabInterfaceDefaultButtons = intArrayOf(83093)
    private val tabInterfaceEquipmentButtons = intArrayOf(27653)
    private val sidebarHomeButtons = intArrayOf(7212)
    private val closeInterfaceButtons = intArrayOf(83051, 9118, 19022, 50001, 58002, 28502, 48502)
    private val questTabToggleButtons = intArrayOf(83097)
    private val logoutButtons = intArrayOf(2458, 9154)
    private val morphButtons = intArrayOf(23132)
    private val ignoredButtons = intArrayOf(26076, 4130, 130, 3014, 3016, 3017)
    private val sharedDepositOrChatButtons = intArrayOf(50004)
    private val sharedDepositOrRunButtons = intArrayOf(50007)
    private val acceptAidButtons = intArrayOf(50006)
    private val rightClickButtons = intArrayOf(50201)
    private val cameraScrollButtons = intArrayOf(50202)
    private val tabInterfaceDeathButtons = intArrayOf(27654)
    private val tabInterfacePriceCheckerButtons = intArrayOf(27651)
    private val tabInterfaceOverrideButtons = intArrayOf(27659)

    override val bindings =
        listOf(
            buttonBinding(-1, 0, "ui.run.off", runOffButtons) { client, _ ->
                client.buttonOnRun = false
                client.varbit(152, 0)
                true
            },
            buttonBinding(-1, 1, "ui.run.on", runOnButtons) { client, _ ->
                client.buttonOnRun = true
                client.varbit(152, 1)
                true
            },
            buttonBinding(-1, 2, "ui.run.toggle", runToggleButtons) { client, _ ->
                client.buttonOnRun = !client.buttonOnRun
                client.varbit(152, if (client.buttonOnRun) 1 else 0)
                true
            },
            buttonBinding(-1, 3, "ui.tab.default_inventory", tabInterfaceDefaultButtons) { client, _ ->
                client.send(SetTabInterface(21172, 3213))
                true
            },
            buttonBinding(-1, 4, "ui.tab.equipment_stats", tabInterfaceEquipmentButtons) { client, _ ->
                client.writeOsrsBonuses()
                client.send(SetTabInterface(15106, 3213))
                true
            },
            buttonBinding(-1, 5, "ui.sidebar.home", sidebarHomeButtons) { client, _ ->
                client.setSidebarInterface(0, 328)
                true
            },
            buttonBinding(-1, 6, "ui.close_interface", closeInterfaceButtons) { client, _ ->
                val wasBanking = client.IsBanking
                val wasBankPreview = client.checkBankInterface
                val wasItemListPreview = client.bankStyleViewOpen
                val wasPartyInterface = client.isPartyInterface
                val wasShopping = client.isShopping
                val wasPriceChecker = PriceCheckerService.isOpen(client)
                ContentActions.cancel(
                    player = client,
                    reason = ContentActionCancelReason.INTERFACE_CLOSED,
                    fullResetAnimation = false,
                    resetCompatibilityState = true,
                )
                DialogueService.closeBlockingDialogue(client, closeInterfaces = true)
                if (client.refundSlot != -1) {
                    client.refundSlot = -1
                }
                SkillItemListMenuService.clear(client)
                var refreshItems = false
                if (wasBanking) {
                    client.IsBanking = false
                    client.bankSearchActive = false
                    client.contentRuntimeState.clearPendingInputState()
                    client.bankSearchQuery = ""
                    client.currentBankTab = 0
                    refreshItems = true
                }
                if (wasBankPreview) {
                    client.checkBankInterface = false
                    refreshItems = true
                }
                if (wasItemListPreview) {
                    client.clearBankStyleView()
                    refreshItems = true
                }
                if (client.checkInv) {
                    client.checkInv = false
                    client.resetItems(3214)
                }
                if (wasPartyInterface) {
                    client.isPartyInterface = false
                    refreshItems = true
                }
                if (wasShopping) {
                    client.MyShopID = -1
                    refreshItems = true
                }
                if (wasPriceChecker) {
                    PriceCheckerService.close(client)
                    refreshItems = true
                }
                if (refreshItems) {
                    client.checkItemUpdate()
                }
                true
            },
            buttonBinding(-1, 7, "ui.quest_tab.toggle", questTabToggleButtons) { client, _ ->
                client.questPage = if (client.questPage == 0) 1 else 0
                true
            },
            buttonBinding(-1, 8, "ui.logout", logoutButtons) { client, _ ->
                if (client.disconnected) {
                    return@buttonBinding true
                }
                if (client.isWalkBlocked() && !client.UsingAgility) {
                    client.sendMessage("You are unable to logout right now.")
                    return@buttonBinding true
                }
                if (ContentSafety.isLogoutLocked(client)) {
                    val seconds = ContentSafety.logoutLockRemainingSeconds(client)
                    client.sendMessage("You must wait $seconds seconds before you can logout!")
                    return@buttonBinding true
                }
                if (System.currentTimeMillis() - client.lastPlayerCombat <= 30000 && client.inWildy()) {
                    client.sendMessage("You must wait 30 seconds after combat in the wilderness to logout.")
                    client.sendMessage("If you X out or disconnect you will stay online for up to a minute")
                    return@buttonBinding true
                }
                client.logout()
                true
            },
            buttonBinding(-1, 9, "ui.morph.clear", morphButtons) { client, _ ->
                if (client.morph) {
                    client.unMorph()
                }
                true
            },
            buttonBinding(-1, 10, "ui.ignored", ignoredButtons) { _, _ ->
                true
            },
            buttonBinding(-1, 11, "ui.shared.deposit_or_chat", sharedDepositOrChatButtons) { client, _ ->
                if (client.IsBanking) {
                    for (i in client.playerItems.indices) {
                        if (client.playerItems[i] > 0) {
                            net.dodian.uber.game.ui.bank.PlayerBankService.deposit(client, client.playerItems[i] - 1, i, client.playerItemsN[i])
                        }
                    }
                    client.sendMessage("You deposit all your items.")
                    client.checkItemUpdate()
                } else {
                    client.setSidebarInterface(11, 50200)
                    client.varbit(980, 2)
                }
                true
            },
            buttonBinding(-1, 12, "ui.shared.deposit_or_run", sharedDepositOrRunButtons) { client, _ ->
                if (client.IsBanking) {
                    val equipment = client.equipment
                    val equipmentN = client.equipmentN
                    for (i in equipment.indices) {
                        val equipId = equipment[i]
                        val equipAmount = equipmentN[i]
                        if (equipId > 0 && equipAmount > 0) {
                            if (net.dodian.uber.game.engine.systems.inventory.EquipmentService.unequipToInventory(client, i)) {
                                  net.dodian.uber.game.ui.bank.PlayerBankService.deposit(client, equipId, client.getItemSlot(equipId), equipAmount)
                            }
                        }
                    }
                    client.sendMessage("You deposit your worn items.")
                    client.checkItemUpdate()
                } else {
                    client.buttonOnRun = !client.buttonOnRun
                    client.varbit(152, if (client.buttonOnRun) 1 else 0)
                }
                true
            },
            buttonBinding(-1, 13, "ui.accept_aid.toggle", acceptAidButtons) { client, _ ->
                client.acceptAid = 1 - client.acceptAid
                client.varbit(427, client.acceptAid)
                true
            },
            buttonBinding(-1, 14, "ui.right_click.toggle", rightClickButtons) { client, _ ->
                client.rightClickToggle = 1 - client.rightClickToggle
                client.varbit(170, client.rightClickToggle)
                true
            },
            buttonBinding(-1, 15, "ui.camera_scroll.toggle", cameraScrollButtons) { client, _ ->
                client.scrollCameraToggle = 1 - client.scrollCameraToggle
                client.varbit(207, client.scrollCameraToggle)
                true
            },
            buttonBinding(-1, 16, "ui.tab.items_kept_on_death", tabInterfaceDeathButtons) { client, _ ->
                client.openInterface(17100)
                true
            },
            buttonBinding(-1, 17, "ui.tab.price_checker", tabInterfacePriceCheckerButtons) { client, _ ->
                PriceCheckerService.open(client)
                true
            },
            buttonBinding(-1, 18, "ui.tab.override", tabInterfaceOverrideButtons) { client, _ ->
                client.openInterface(60106)
                true
            }
        )
}
