package net.dodian.uber.game.content.buttons.settings

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.content.buttons.ButtonContent

object SettingsTabButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        44511, // Mystic settings tab: "More Settings"
        23020, // Mystic basic settings: Confirm
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        when (buttonId) {
            44511 -> client.setSidebarInterface(11, 23000)
            23020 -> client.setSidebarInterface(11, 44500)
            else -> return false
        }

        return true
    }
}
