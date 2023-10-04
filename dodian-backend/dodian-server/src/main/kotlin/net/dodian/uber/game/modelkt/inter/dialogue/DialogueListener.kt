package net.dodian.uber.game.modelkt.inter.dialogue

import net.dodian.uber.game.modelkt.inter.InterfaceListener


interface DialogueListener : InterfaceListener {
    fun buttonClicked(button: Int): Boolean
    fun continued()
}