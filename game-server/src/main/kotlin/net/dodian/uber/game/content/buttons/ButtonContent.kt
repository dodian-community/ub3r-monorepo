package net.dodian.uber.game.content.buttons

import net.dodian.uber.game.model.entity.player.Client

interface ButtonContent {
    val buttonIds: IntArray
    fun onClick(client: Client, buttonId: Int): Boolean
}

