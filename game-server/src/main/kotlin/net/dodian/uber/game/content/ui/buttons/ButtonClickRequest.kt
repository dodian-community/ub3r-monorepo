package net.dodian.uber.game.content.ui.buttons

import net.dodian.uber.game.model.entity.player.Client

data class ButtonClickRequest(
    val client: Client,
    val rawButtonId: Int,
    val opIndex: Int,
    val activeInterfaceId: Int,
    val interfaceId: Int,
    val componentId: Int,
    val componentKey: String,
)

