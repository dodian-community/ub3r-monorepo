package net.dodian.uber.game.content.interfaces.ui

import net.dodian.uber.game.netty.listener.out.SetTabInterface
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
        )
}

