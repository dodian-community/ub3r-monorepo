package net.dodian.uber.game.modelkt.inter

import net.dodian.uber.game.modelkt.entity.player.Player
import net.dodian.uber.game.modelkt.inter.dialogue.DialogueListener

class InterfaceSet(
    val player: Player,
    var amountListener: EnterAmountListener? = null,
    var dialogueListener: DialogueListener? = null,
    var listener: InterfaceListener? = null,
    val interfaces: MutableMap<InterfaceType, Int> = mutableMapOf()
) {

    fun close() {
        if (interfaces.isEmpty()) return


    }
}