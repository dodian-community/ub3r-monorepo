package net.dodian.uber.game.content.interfaces.settings

import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding

object SettingsInterfaceButtons : InterfaceButtonContent {
    override val bindings =
        listOf(
            buttonBinding(-1, 0, "settings.open_more", SettingsComponents.openMoreSettingsButtons) { client, _ ->
                client.setSidebarInterface(11, SettingsComponents.MORE_SETTINGS_TAB_ID)
                true
            },
            buttonBinding(-1, 1, "settings.close_more", SettingsComponents.closeMoreSettingsButtons) { client, _ ->
                client.setSidebarInterface(11, SettingsComponents.SETTINGS_TAB_ID)
                true
            },
        )
}

