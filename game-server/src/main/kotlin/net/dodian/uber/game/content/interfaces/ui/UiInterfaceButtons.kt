package net.dodian.uber.game.content.interfaces.ui

import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SetTabInterface
import net.dodian.uber.game.runtime.api.content.ContentActionCancelReason
import net.dodian.uber.game.runtime.api.content.ContentActions
import net.dodian.uber.game.runtime.api.content.ContentSafety
import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding

object UiInterfaceButtons : InterfaceButtonContent {
    override val bindings =
        listOf(
            buttonBinding(-1, 0, "ui.run.off", UiComponents.runOffButtons) { client, _ ->
                client.buttonOnRun = false
                true
            },
            buttonBinding(-1, 1, "ui.run.on", UiComponents.runOnButtons) { client, _ ->
                client.buttonOnRun = true
                true
            },
            buttonBinding(-1, 2, "ui.run.toggle", UiComponents.runToggleButtons) { client, _ ->
                client.buttonOnRun = !client.buttonOnRun
                true
            },
            buttonBinding(-1, 3, "ui.tab.default_inventory", UiComponents.tabInterfaceDefaultButtons) { client, _ ->
                client.send(SetTabInterface(21172, 3213))
                true
            },
            buttonBinding(-1, 4, "ui.tab.equipment_stats", UiComponents.tabInterfaceEquipmentButtons) { client, _ ->
                client.send(SetTabInterface(15106, 3213))
                true
            },
            buttonBinding(-1, 5, "ui.sidebar.home", UiComponents.sidebarHomeButtons) { client, _ ->
                client.setSidebarInterface(0, 328)
                true
            },
            buttonBinding(-1, 6, "ui.close_interface", UiComponents.closeInterfaceButtons) { client, _ ->
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
            buttonBinding(-1, 7, "ui.quest_tab.toggle", UiComponents.questTabToggleButtons) { client, _ ->
                client.questPage = if (client.questPage == 0) 1 else 0
                true
            },
            buttonBinding(-1, 8, "ui.logout", UiComponents.logoutButtons) { client, _ ->
                if (client.disconnected) {
                    return@buttonBinding true
                }
                if (System.currentTimeMillis() < client.walkBlock && !client.UsingAgility) {
                    client.send(SendMessage("You are unable to logout right now."))
                    return@buttonBinding true
                }
                if (ContentSafety.isLogoutLocked(client)) {
                    val seconds = ContentSafety.logoutLockRemainingSeconds(client)
                    client.send(SendMessage("You must wait $seconds seconds before you can logout!"))
                    return@buttonBinding true
                }
                if (System.currentTimeMillis() - client.lastPlayerCombat <= 30000 && client.inWildy()) {
                    client.send(SendMessage("You must wait 30 seconds after combat in the wilderness to logout."))
                    client.send(SendMessage("If you X out or disconnect you will stay online for up to a minute"))
                    return@buttonBinding true
                }
                client.logout()
                true
            },
            buttonBinding(-1, 9, "ui.morph.clear", UiComponents.morphButtons) { client, _ ->
                if (client.morph) {
                    client.unMorph()
                }
                true
            },
            buttonBinding(-1, 10, "ui.ignored", UiComponents.ignoredButtons) { _, _ ->
                true
            },
        )
}
