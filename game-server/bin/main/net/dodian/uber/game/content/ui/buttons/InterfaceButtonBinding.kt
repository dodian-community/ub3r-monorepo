package net.dodian.uber.game.content.ui.buttons

import net.dodian.uber.game.model.entity.player.Client

fun interface InterfaceButtonHandler {
    fun onClick(request: ButtonClickRequest): Boolean
}

data class InterfaceButtonBinding(
    val interfaceId: Int,
    val componentId: Int,
    val componentKey: String,
    val rawButtonIds: IntArray,
    val requiredInterfaceId: Int = -1,
    val opIndex: Int? = null,
    val handler: InterfaceButtonHandler,
)

interface InterfaceButtonContent {
    val bindings: List<InterfaceButtonBinding>
}

fun buttonBinding(
    interfaceId: Int,
    componentId: Int,
    componentKey: String,
    rawButtonIds: IntArray,
    requiredInterfaceId: Int = -1,
    opIndex: Int? = null,
    handler: (Client, ButtonClickRequest) -> Boolean,
): InterfaceButtonBinding =
    InterfaceButtonBinding(
        interfaceId = interfaceId,
        componentId = componentId,
        componentKey = componentKey,
        rawButtonIds = rawButtonIds,
        requiredInterfaceId = requiredInterfaceId,
        opIndex = opIndex,
        handler = InterfaceButtonHandler { request -> handler(request.client, request) },
    )

