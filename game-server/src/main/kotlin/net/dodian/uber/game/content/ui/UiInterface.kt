package net.dodian.uber.game.content.ui

import net.dodian.uber.game.netty.listener.out.SetTabInterface
import net.dodian.uber.game.systems.api.content.ContentActionCancelReason
import net.dodian.uber.game.systems.api.content.ContentActions
import net.dodian.uber.game.systems.api.content.ContentSafety
import net.dodian.uber.game.systems.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.systems.ui.buttons.buttonBinding
import net.dodian.uber.game.systems.ui.dialogue.DialogueService

object UiInterface : InterfaceButtonContent {
    private val runOffButtons = intArrayOf(152)
    private val runOnButtons = intArrayOf(19158)
    private val runToggleButtons = intArrayOf(74214)
    private val tabInterfaceDefaultButtons = intArrayOf(83093)
    private val tabInterfaceEquipmentButtons = intArrayOf(27653)
    private val sidebarHomeButtons = intArrayOf(7212)
    private val closeInterfaceButtons = intArrayOf(83051, 9118, 19022, 50001)
    private val questTabToggleButtons = intArrayOf(83097)
    private val logoutButtons = intArrayOf(2458, 9154)
    private val morphButtons = intArrayOf(23132)
    private val ignoredButtons = intArrayOf(26076, 4130, 130, 3014, 3016, 3017)

    override val bindings =
        listOf(
            buttonBinding(-1, 0, "ui.run.off", runOffButtons) { client, _ ->
                client.buttonOnRun = false
                true
            },
            buttonBinding(-1, 1, "ui.run.on", runOnButtons) { client, _ ->
                client.buttonOnRun = true
                true
            },
            buttonBinding(-1, 2, "ui.run.toggle", runToggleButtons) { client, _ ->
                client.buttonOnRun = !client.buttonOnRun
                true
            },
            buttonBinding(-1, 3, "ui.tab.default_inventory", tabInterfaceDefaultButtons) { client, _ ->
                client.send(SetTabInterface(21172, 3213))
                true
            },
            buttonBinding(-1, 4, "ui.tab.equipment_stats", tabInterfaceEquipmentButtons) { client, _ ->
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
                if (client.herbMaking != -1) {
                    client.herbMaking = -1
                }
                var refreshItems = false
                if (wasBanking) {
                    client.IsBanking = false
                    client.bankSearchActive = false
                    client.bankSearchPendingInput = false
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
                if (wasPartyInterface) {
                    client.isPartyInterface = false
                    refreshItems = true
                }
                if (wasShopping) {
                    client.MyShopID = -1
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
                if (System.currentTimeMillis() < client.walkBlock && !client.UsingAgility) {
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
        )
}
