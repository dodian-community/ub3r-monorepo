package net.dodian.uber.game.content.ui

import net.dodian.uber.game.systems.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.systems.ui.buttons.buttonBinding

object SettingsInterface : InterfaceButtonContent {
    private const val SETTINGS_TAB_ID = 44500
    private const val MORE_SETTINGS_TAB_ID = 23000

    private val openMoreSettingsButtons = intArrayOf(44511)
    private val closeMoreSettingsButtons = intArrayOf(23020)
    private val pinHelpButtons = intArrayOf(58073)
    private val bossYellEnableButtons = intArrayOf(24136)
    private val bossYellDisableButtons = intArrayOf(24137)

    override val bindings =
        listOf(
            buttonBinding(-1, 0, "settings.open_more", openMoreSettingsButtons) { client, _ ->
                client.setSidebarInterface(11, MORE_SETTINGS_TAB_ID)
                true
            },
            buttonBinding(-1, 1, "settings.close_more", closeMoreSettingsButtons) { client, _ ->
                client.setSidebarInterface(11, SETTINGS_TAB_ID)
                true
            },
            buttonBinding(-1, 2, "settings.pin_help", pinHelpButtons) { client, _ ->
                client.sendMessage("Visit the Dodian.net UserCP and click edit pin to remove your pin")
                true
            },
            buttonBinding(-1, 3, "settings.boss_yell.enable", bossYellEnableButtons) { client, _ ->
                client.yellOn = true
                client.sendMessage("You enabled the boss yell messages.")
                true
            },
            buttonBinding(-1, 4, "settings.boss_yell.disable", bossYellDisableButtons) { client, _ ->
                client.yellOn = false
                client.sendMessage("You disabled the boss yell messages.")
                true
            },
        )
}
