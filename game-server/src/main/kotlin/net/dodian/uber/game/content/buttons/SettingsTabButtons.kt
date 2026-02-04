package net.dodian.uber.game.content.buttons

import net.dodian.uber.game.model.entity.player.Client

object SettingsTabButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        44511, // Mystic settings tab: "More Settings"
        23020, // Mystic basic settings: Confirm
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        // We move the print here so it executes before the branching logic.
        // This is great for debugging which IDs are being sent from the client!
        println("Button clicked: $buttonId")

        when (buttonId) {
            44511 -> client.setSidebarInterface(11, 23000)
            23020 -> client.setSidebarInterface(11, 44500)
            else -> return false // If it's not one of our IDs, tell the handler we didn't use it
        }

        return true // We handled a button, so we return true
    }
}