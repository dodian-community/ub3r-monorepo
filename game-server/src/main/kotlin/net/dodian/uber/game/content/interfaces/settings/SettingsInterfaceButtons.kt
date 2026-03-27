package net.dodian.uber.game.content.interfaces.settings

import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.systems.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.systems.ui.buttons.buttonBinding

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
            buttonBinding(-1, 2, "settings.pin_help", SettingsComponents.pinHelpButtons) { client, _ ->
                client.sendMessage("Visit the Dodian.net UserCP and click edit pin to remove your pin")
                true
            },
            buttonBinding(-1, 3, "settings.boss_yell.enable", SettingsComponents.bossYellEnableButtons) { client, _ ->
                client.yellOn = true
                client.sendMessage("You enabled the boss yell messages.")
                true
            },
            buttonBinding(-1, 4, "settings.boss_yell.disable", SettingsComponents.bossYellDisableButtons) { client, _ ->
                client.yellOn = false
                client.sendMessage("You disabled the boss yell messages.")
                true
            },
        )
}
