package net.dodian.uber.game.content.ui

import net.dodian.uber.game.content.ui.buttons.InterfaceButtonBinding

@Deprecated("Use net.dodian.uber.game.ui.AppearanceInterface")
object AppearanceInterface {
    val bindings: List<InterfaceButtonBinding>
        get() = net.dodian.uber.game.ui.AppearanceInterface.bindings
}
