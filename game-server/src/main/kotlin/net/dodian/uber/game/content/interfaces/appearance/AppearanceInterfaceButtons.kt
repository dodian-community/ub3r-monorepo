package net.dodian.uber.game.content.interfaces.appearance

import net.dodian.uber.game.model.UpdateFlag
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.systems.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.systems.ui.buttons.buttonBinding

object AppearanceInterfaceButtons : InterfaceButtonContent {
    override val bindings =
        listOf(
            buttonBinding(-1, 0, "appearance.confirm", AppearanceComponents.confirmButtons) { client, _ ->
                client.send(RemoveInterfaces())
                client.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
                true
            }
        )
}
