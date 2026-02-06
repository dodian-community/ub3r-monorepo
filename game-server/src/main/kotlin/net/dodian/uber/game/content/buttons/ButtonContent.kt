package net.dodian.uber.game.content.buttons

import net.dodian.uber.game.model.entity.player.Client

interface ButtonContent {
    val buttonIds: IntArray
    /**
     * If set, this button only executes while the player has this interface open.
     * Tab interfaces should usually leave this as -1.
     */
    val requiredInterfaceId: Int
        get() = -1
    fun onClick(client: Client, buttonId: Int): Boolean
}
