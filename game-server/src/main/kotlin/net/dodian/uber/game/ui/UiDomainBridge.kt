@file:Suppress("unused")

package net.dodian.uber.game.ui

typealias ButtonClickRequest = net.dodian.uber.game.content.ui.buttons.ButtonClickRequest
typealias InterfaceButtonBinding = net.dodian.uber.game.content.ui.buttons.InterfaceButtonBinding
typealias InterfaceButtonContent = net.dodian.uber.game.content.ui.buttons.InterfaceButtonContent

fun buttonBinding(
    interfaceId: Int,
    componentId: Int,
    componentKey: String,
    rawButtonIds: IntArray,
    requiredInterfaceId: Int = -1,
    opIndex: Int? = null,
    handler: (net.dodian.uber.game.model.entity.player.Client, ButtonClickRequest) -> Boolean,
): InterfaceButtonBinding =
    net.dodian.uber.game.content.ui.buttons.buttonBinding(
        interfaceId = interfaceId,
        componentId = componentId,
        componentKey = componentKey,
        rawButtonIds = rawButtonIds,
        requiredInterfaceId = requiredInterfaceId,
        opIndex = opIndex,
        handler = handler,
    )

