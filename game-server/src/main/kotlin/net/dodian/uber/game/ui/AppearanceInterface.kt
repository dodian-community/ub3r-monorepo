@file:Suppress("unused")

package net.dodian.uber.game.ui

import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding

import net.dodian.uber.game.model.entity.UpdateFlag
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces

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
