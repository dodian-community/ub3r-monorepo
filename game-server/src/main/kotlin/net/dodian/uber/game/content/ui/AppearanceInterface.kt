package net.dodian.uber.game.content.ui

import net.dodian.uber.game.model.entity.UpdateFlag
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.systems.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.systems.ui.buttons.buttonBinding

object AppearanceInterface : InterfaceButtonContent {
    private val confirmButtons = intArrayOf(3651)

    override val bindings =
        listOf(
            buttonBinding(-1, 0, "appearance.confirm", confirmButtons) { client, _ ->
                client.send(RemoveInterfaces())
                client.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
                true
            },
        )
}
